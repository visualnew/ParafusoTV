package com.lumora.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lumora.cache.FavoritesStore
import com.lumora.cache.PlaybackPositionStore
import com.lumora.cache.WatchedStore
import com.lumora.ParafusoProfile
import com.lumora.ParafusoProfileStore
import com.lumora.data.IptvProviderStore
import com.lumora.data.MediaServerStore
import com.lumora.data.local.LumoraDatabase
import com.lumora.data.local.entity.*
import com.lumora.model.IptvProviderConfig
import com.lumora.model.MediaServerConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages export/import of app data for backup and restore.
 * Exports IPTV provider configs, Jellyfin/Plex sessions, favorites, EPG sources,
 * custom groups, watch history and recording schedules to JSON.
 */
class BackupManager(private val context: Context) {

    private val TAG = "BackupManager"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    companion object {
        // Deliberately not bumped for the fields this change adds: they are additive and
        // Gson-tolerant in both directions. An old build reading a new backup drops the
        // unknown fields; a new build reading an old backup sees them absent, which Gson
        // surfaces as null - normalised back to "leave existing data alone" by
        // restoreBackupData rather than crashing.
        const val BACKUP_VERSION = 1
        private const val ENCRYPTED_MAGIC = "PARAFUSOBKP1"
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_BITS = 256

        /** Pure fallback used when a backup's selected profile is absent or no longer valid. */
        internal fun resolveActiveProfileId(profiles: List<ParafusoProfile>, savedActiveId: Int?): Int? =
            savedActiveId?.takeIf { candidate -> profiles.any { it.id == candidate } }
                ?: profiles.firstOrNull()?.id
    }

    // The list fields are nullable on purpose: gson.fromJson leaves them null when the JSON
    // array is missing outright (backups written before a field existed), Kotlin defaults
    // notwithstanding. Null means "absent" to the restore path - see restoreBackupData.
    data class BackupData(
        val version: Int = BACKUP_VERSION,
        val createdAt: String = "",
        val appVersion: String = "2.0",
        // Legacy field: the Room `providers` table was never written by anything except the
        // old export path, so real backups always carried an empty list. Kept for format
        // stability; the live configs live in iptvProviders/mediaServers below.
        val providers: List<ProviderBackup>? = emptyList(),
        val iptvProviders: List<IptvProviderBackup>? = emptyList(),
        val mediaServers: List<MediaServerBackup>? = emptyList(),
        val epgSources: List<EpgSourceBackup>? = emptyList(),
        val customGroups: List<CustomGroupBackup>? = emptyList(),
        // Legacy field, always empty in practice (the old export hardcoded it).
        // favoriteSeries/favoriteChannels retain the active profile for old builds; new builds
        // use profileFavorites for every profile.
        val favorites: List<String>? = emptyList(),
        val favoriteSeries: List<String>? = emptyList(),
        val favoriteChannels: List<String>? = emptyList(),
        // Added fields stay nullable so v1 backups written before profiles remain importable.
        val profiles: List<ProfileBackup>? = null,
        val activeProfileId: Int? = null,
        val profileFavorites: List<ProfileFavoritesBackup>? = null,
        val profilePlaybackPositions: List<ProfilePlaybackPositionsBackup>? = null,
        val profileWatchedMarks: List<ProfileWatchedMarksBackup>? = null,
        val watchHistory: List<WatchHistoryBackup>? = emptyList(),
        val recordingStorage: RecordingStorageBackup? = null,
        val recordingSchedules: List<RecordingScheduleBackup>? = emptyList(),
        val checksum: String = ""
    )

    data class ProviderBackup(
        val id: String, val name: String, val type: String,
        val serverUrl: String?, val username: String?, val password: String?,
        val m3uUrl: String?, val userAgent: String?, val macAddress: String?,
        val serialNumber: String?, val active: Boolean,
        val syncEnabled: Boolean, val epgSyncEnabled: Boolean
    )

    /** One IptvProviderConfig (Xtream/M3U/Stalker), round-tripped through the JSON backup. */
    data class IptvProviderBackup(
        val id: String, val type: String, val name: String,
        val enabled: Boolean, val liveEnabled: Boolean,
        val moviesEnabled: Boolean, val seriesEnabled: Boolean,
        val url: String?, val username: String?, val password: String?,
        val userAgent: String?
    )

    /** One MediaServerConfig (Jellyfin/Plex session), round-tripped through the JSON backup. */
    data class MediaServerBackup(
        val id: String, val type: String, val name: String,
        val enabled: Boolean, val url: String?, val altUrls: List<String> = emptyList(),
        val username: String?, val password: String?,
        val token: String?, val userId: String?, val accountToken: String?,
        val liveEnabled: Boolean, val moviesEnabled: Boolean, val seriesEnabled: Boolean
    )

    // refreshIntervalHours is nullable so a backup written before the field existed imports
    // as "absent" rather than as Gson's raw 0 - a 0-hour interval would make the EPG worker
    // treat every source as perpetually due. Restore substitutes the entity's default.
    data class EpgSourceBackup(
        val id: String, val name: String, val url: String,
        val enabled: Boolean, val priority: Int,
        val userAgent: String? = null,
        val refreshIntervalHours: Int? = null
    )

    data class CustomGroupBackup(
        val id: String, val name: String, val mediaType: String,
        val isHidden: Boolean, val members: List<String> = emptyList()
    )

    data class ProfileBackup(
        val id: Int, val name: String, val child: Boolean = false, val accent: String = "silver"
    )

    data class ProfileFavoritesBackup(
        val profileId: Int, val favoriteSeries: List<String> = emptyList(), val favoriteChannels: List<String> = emptyList()
    )

    /** Existing profile-scoped playback JSON; its owning store remains the format authority. */
    data class ProfilePlaybackPositionsBackup(val profileId: Int, val payload: String)

    data class ProfileWatchedMarksBackup(val profileId: Int, val marks: List<String> = emptyList())

    data class WatchHistoryBackup(
        // Original row id, carried so restore keeps entries distinct per channel instead of
        // collapsing them onto one "backup_<channelId>" key. Null on backups written by old
        // builds; restore falls back to a per-entry id.
        val id: String? = null,
        // Absent from backups written before profile-scoped Room history; those rows belong
        // to the legacy/default profile when restored.
        val profileId: Int? = null,
        val channelId: String, val channelName: String,
        val mediaType: String, val positionMs: Long,
        val durationMs: Long, val status: String,
        val lastWatchedAt: Long,
        // Carried so restore doesn't lose firstWatchedAt (old backups only had lastWatchedAt).
        val firstWatchedAt: Long? = null,
        // Nullable for Gson compatibility with older backups that did not serialize them.
        val progressPercent: Float? = null,
        val watchCount: Int? = null
    )

    data class RecordingStorageBackup(
        val maxSimultaneous: Int, val retentionDays: Int,
        val fileNamePattern: String
    )

    data class RecordingScheduleBackup(
        // Original row id, carried so re-import REPLACEs the schedule instead of inserting a
        // fresh UUID and duplicating it. Null on old backups; restore falls back to a new UUID.
        val id: String? = null,
        val channelId: String, val channelName: String,
        val programTitle: String, val startTimeUtc: Long,
        val stopTimeUtc: Long, val recurringRule: String?
    )

    data class ImportResult(
        val providersImported: Int = 0,
        val epgSourcesImported: Int = 0,
        val customGroupsImported: Int = 0,
        val watchHistoryImported: Int = 0,
        val recordingSchedulesImported: Int = 0,
        val conflicts: Int = 0,
        // Set when the import failed outright (unreadable file, checksum mismatch, restore
        // error) so callers can tell "imported nothing because there was nothing to import"
        // apart from "imported nothing because it went wrong". Null on success.
        val errorMessage: String? = null
    )

    /**
     * Export app data to a URI (SAF document).
     */
    suspend fun exportTo(uri: Uri, password: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = LumoraDatabase.getInstance(context)
            val data = collectBackupData(db)

            val json = gson.toJson(data)
            val checksum = md5(json)
            val finalData = data.copy(checksum = checksum)
            val finalJson = gson.toJson(finalData)
            val payload = if (password.isNullOrBlank()) finalJson.toByteArray(Charsets.UTF_8)
                else encrypt(finalJson.toByteArray(Charsets.UTF_8), password)

            val stream = context.contentResolver.openOutputStream(uri)
            if (stream == null) {
                Log.e(TAG, "Export failed: could not open output stream for $uri")
                return@withContext false
            }
            stream.use { outputStream -> outputStream.write(payload); outputStream.flush() }
            Log.d(TAG, "Backup exported successfully (${if (password.isNullOrBlank()) "plain" else "encrypted"})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}")
            false
        }
    }

    /**
     * Import app data from a URI (SAF document).
     */
    suspend fun importFrom(uri: Uri, confirmed: Boolean = false, password: String? = null): ImportResult = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext ImportResult(errorMessage = "Could not open backup file")
            val json = try {
                val raw = String(bytes, Charsets.UTF_8)
                if (raw.startsWith(ENCRYPTED_MAGIC)) {
                    if (password.isNullOrBlank()) return@withContext ImportResult(errorMessage = "PASSWORD_REQUIRED")
                    String(decrypt(bytes, password), Charsets.UTF_8)
                } else raw
            } catch (e: Exception) {
                if (bytes.size >= ENCRYPTED_MAGIC.length && bytes.copyOfRange(0, ENCRYPTED_MAGIC.length).contentEquals(ENCRYPTED_MAGIC.toByteArray(Charsets.US_ASCII))) {
                    return@withContext ImportResult(errorMessage = "INVALID_PASSWORD")
                }
                throw e
            }

            val data = gson.fromJson(json, BackupData::class.java)

            // Verify checksum
            if (data.checksum.isNotBlank()) {
                val expectedChecksum = md5(gson.toJson(data.copy(checksum = "")))
                if (data.checksum != expectedChecksum) {
                    Log.e(TAG, "Backup checksum mismatch - rejecting import")
                    return@withContext ImportResult(
                        errorMessage = "Checksum mismatch - backup is corrupt or was modified"
                    )
                }
            }

            // If not confirmed and there is existing data, return conflicts to prompt confirmation.
            // The gate runs on the real stores (provider configs, media-server sessions,
            // favourites, Room tables) - the old check on the Room `providers` table always
            // returned false because nothing ever writes that table, so the confirmation never
            // fired and every import silently merged into existing data.
            if (!confirmed && hasExistingData()) {
                val existingCount = countExistingData()
                Log.d(TAG, "Existing data detected ($existingCount items), confirmation required")
                return@withContext ImportResult(conflicts = existingCount)
            }

            val result = restoreBackupData(data)
            Log.d(TAG, "Import completed: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}")
            // Surface the failure instead of returning a zeroed result that read as a
            // successful-but-empty import.
            ImportResult(errorMessage = e.message ?: e.javaClass.simpleName)
        }
    }

    /** True when the device already holds any data a restore would touch: IPTV provider
     *  configs, media-server sessions, favourites, or any of the Room tables. The old
     *  implementation only checked the Room `providers` table, which nothing ever writes,
     *  so this was always false and the confirmation gate never fired. */
    private suspend fun hasExistingData(): Boolean = countExistingData() > 0

    /** Counts existing data across the real stores, used as the `conflicts` figure that
     *  prompts the confirmation dialog. A heuristic count is fine - it only needs to be
     *  non-zero to gate, and roughly right for the prompt. */
    private suspend fun countExistingData(): Int {
        val db = LumoraDatabase.getInstance(context)
        val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
        var count = 0
        count += IptvProviderStore.load(prefs).size
        count += MediaServerStore.load(prefs).size
        count += FavoritesStore.getFavoriteSeriesIds(context).size
        count += FavoritesStore.getFavoriteChannelIds(context).size
        count += db.epgSourceDao().getAll().size
        count += db.customGroupDao().getAll().size
        count += db.watchHistoryDao().countAllForBackup()
        count += db.recordingDao().getAll().size
        return count
    }

    private suspend fun collectBackupData(db: LumoraDatabase): BackupData {
        val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
        val profileStore = ParafusoProfileStore(context)
        val profiles = profileStore.ensureDefault()
        val activeProfileId = profileStore.activeId().takeIf { activeId -> profiles.any { it.id == activeId } }
            ?: profiles.firstOrNull()?.id ?: 1
        val epgSources = db.epgSourceDao().getAll()
        val customGroups = db.customGroupDao().getAll()
        // getAll, not getRecent: the LIMIT 50 read truncated every backup to the 50 most
        // recent entries and the rest was unrecoverable once the user cleared the app.
        val watchHistory = db.watchHistoryDao().getAllForBackup()
        val recordingStorage = db.recordingDao().getStorageConfig()
        val recordings = db.recordingDao().getAll()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        return BackupData(
            version = BACKUP_VERSION,
            createdAt = dateFormat.format(Date()),
            appVersion = "2.0",
            // The Room `providers` table is dead (nothing writes it) - the real configs live
            // in SharedPreferences via IptvProviderStore/MediaServerStore, so those are what
            // get serialized. `providers` stays empty for format stability.
            providers = emptyList(),
            iptvProviders = IptvProviderStore.load(prefs).map { it.toBackup() },
            mediaServers = MediaServerStore.load(prefs).map { it.toBackup() },
            epgSources = epgSources.map { EpgSourceBackup(
                id = it.id, name = it.name, url = it.url,
                enabled = it.enabled, priority = it.priority,
                userAgent = it.userAgent,
                refreshIntervalHours = it.refreshIntervalHours
            )},
            customGroups = customGroups.map { group ->
                val members = db.customGroupDao().getMembers(group.id)
                CustomGroupBackup(
                    id = group.id, name = group.name,
                    mediaType = group.mediaType, isHidden = group.isHidden,
                    members = members.map { it.channelId }
                )
            },
            favorites = emptyList(),
            // Retained for old builds that do not understand profileFavorites yet.
            favoriteSeries = FavoritesStore.getFavoriteSeriesIdsForProfile(context, activeProfileId).toList(),
            favoriteChannels = FavoritesStore.getFavoriteChannelIdsForProfile(context, activeProfileId).toList(),
            profiles = profiles.map { it.toBackup() },
            activeProfileId = activeProfileId,
            profileFavorites = profiles.map { profile ->
                ProfileFavoritesBackup(
                    profileId = profile.id,
                    favoriteSeries = FavoritesStore.getFavoriteSeriesIdsForProfile(context, profile.id).toList(),
                    favoriteChannels = FavoritesStore.getFavoriteChannelIdsForProfile(context, profile.id).toList()
                )
            },
            profilePlaybackPositions = profiles.map { profile ->
                ProfilePlaybackPositionsBackup(
                    profile.id, PlaybackPositionStore.exportForProfileBackup(context, profile.id)
                )
            },
            profileWatchedMarks = profiles.map { profile ->
                ProfileWatchedMarksBackup(
                    profile.id, WatchedStore.exportForProfileBackup(context, profile.id).toList()
                )
            },
            watchHistory = watchHistory.map { WatchHistoryBackup(
                id = it.id, profileId = it.profileId,
                channelId = it.channelId, channelName = it.channelName,
                mediaType = it.mediaType, positionMs = it.positionMs,
                durationMs = it.durationMs, status = it.status,
                lastWatchedAt = it.lastWatchedAt, firstWatchedAt = it.firstWatchedAt,
                progressPercent = it.progressPercent, watchCount = it.watchCount
            )},
            recordingStorage = recordingStorage?.let { RecordingStorageBackup(
                maxSimultaneous = it.maxSimultaneous,
                retentionDays = it.retentionDays,
                fileNamePattern = it.fileNamePattern
            )},
            recordingSchedules = recordings.filter { it.status == "SCHEDULED" }.map {
                RecordingScheduleBackup(
                    id = it.id, channelId = it.channelId, channelName = it.channelName,
                    programTitle = it.programTitle,
                    startTimeUtc = it.startTimeUtc, stopTimeUtc = it.stopTimeUtc,
                    recurringRule = it.recurringRule
                )
            }
        )
    }

    private suspend fun restoreBackupData(data: BackupData): ImportResult {
        val db = LumoraDatabase.getInstance(context)
        val prefs = context.getSharedPreferences("iptv_prefs", Context.MODE_PRIVATE)
        val profileStore = ParafusoProfileStore(context)
        // gson.fromJson answers null for every array that is absent from the file - backups
        // written before a field existed, so Kotlin's emptyList defaults never apply. Null
        // means "absent": per the contract below, an absent list leaves what the device
        // holds untouched, which is exactly what these normalised empties do. Normalising
        // once here also stops a mid-restore NPE from being swallowed by importFrom's catch
        // and reported as a silently zeroed result.
        val iptvProviders = data.iptvProviders.orEmpty()
        val mediaServers = data.mediaServers.orEmpty()
        val favoriteSeries = data.favoriteSeries.orEmpty()
        val favoriteChannels = data.favoriteChannels.orEmpty()
        val epgSources = data.epgSources.orEmpty()
        val customGroups = data.customGroups.orEmpty()
        val watchHistory = data.watchHistory.orEmpty()
        val recordingSchedules = data.recordingSchedules.orEmpty()
        var result = ImportResult()

        // A missing field means an old/partial backup and must leave local profiles alone.
        // New backups restore profile identities before any profile-keyed data reaches disk.
        val restoredProfiles = data.profiles
            ?.map { ParafusoProfile(it.id, it.name, it.child, it.accent) }
            ?.filter { it.id > 0 }
            ?.distinctBy { it.id }
            .orEmpty()
        val availableProfiles = if (data.profiles != null && restoredProfiles.isNotEmpty()) {
            profileStore.save(restoredProfiles)
            restoredProfiles
        } else {
            profileStore.ensureDefault()
        }
        if (data.profiles != null) {
            val restoredActiveId = resolveActiveProfileId(availableProfiles, data.activeProfileId)
            restoredActiveId?.let(profileStore::setActive)
        }
        val restoredProfileIds = availableProfiles.mapTo(mutableSetOf()) { it.id }

        // Null means the field did not exist in an older backup. An empty present list is a
        // legitimate snapshot with no profile favourites, so it intentionally has no writes.
        data.profileFavorites?.forEach { entry ->
            if (entry.profileId in restoredProfileIds) {
                FavoritesStore.replaceAllForProfile(
                    context, entry.profileId, entry.favoriteSeries.toSet(), entry.favoriteChannels.toSet()
                )
            }
        }
        data.profilePlaybackPositions?.forEach { entry ->
            if (entry.profileId in restoredProfileIds) {
                PlaybackPositionStore.restoreForProfileBackup(context, entry.profileId, entry.payload)
            }
        }
        data.profileWatchedMarks?.forEach { entry ->
            if (entry.profileId in restoredProfileIds) {
                WatchedStore.restoreForProfileBackup(context, entry.profileId, entry.marks.toSet())
            }
        }

        // Restore IPTV provider configs. The backup is a snapshot of the whole list, so a
        // present list replaces the current one outright (no merge); an absent list (old
        // backup format) leaves existing configs untouched.
        if (iptvProviders.isNotEmpty()) {
            IptvProviderStore.save(prefs, iptvProviders.map { it.toConfig() })
        }

        // Restore Jellyfin/Plex sessions the same way - replace when present, leave alone
        // when the backup predates this field.
        if (mediaServers.isNotEmpty()) {
            MediaServerStore.save(prefs, mediaServers.map { it.toConfig() })
        }

        // Restore favourites, replacing both sets outright.
        if (favoriteSeries.isNotEmpty() || favoriteChannels.isNotEmpty()) {
            FavoritesStore.replaceAll(context, favoriteSeries.toSet(), favoriteChannels.toSet())
        }

        result = result.copy(providersImported = iptvProviders.size + mediaServers.size)

        // Restore EPG sources
        for (e in epgSources) {
            db.epgSourceDao().insert(
                EpgSourceEntity(
                    id = e.id, name = e.name, url = e.url,
                    enabled = e.enabled, priority = e.priority,
                    userAgent = e.userAgent,
                    // Absent on old backups (null) -> keep the entity default rather than
                    // writing a 0-hour interval.
                    refreshIntervalHours = e.refreshIntervalHours ?: 24
                )
            )
            result = result.copy(epgSourcesImported = result.epgSourcesImported + 1)
        }

        // Restore custom groups
        for (g in customGroups) {
            db.customGroupDao().insert(
                CustomGroupEntity(
                    id = g.id, name = g.name,
                    mediaType = g.mediaType, isHidden = g.isHidden
                )
            )
            if (g.members.isNotEmpty()) {
                val members = g.members.mapIndexed { index, chId ->
                    CustomGroupMemberEntity(
                        id = "${g.id}_$chId",
                        groupId = g.id, channelId = chId,
                        sortOrder = index
                    )
                }
                db.customGroupDao().insertMembers(members)
            }
            result = result.copy(customGroupsImported = result.customGroupsImported + 1)
        }

        // Restore watch history. The original row id is carried in the backup so multiple
        // entries for one channel stay distinct (the old "backup_<channelId>" key collapsed
        // them via REPLACE) and firstWatchedAt survives. Old backups without an id fall back
        // to a per-entry key derived from lastWatchedAt, which is still unique per entry.
        for (h in watchHistory) {
            val historyProfileId = h.profileId
                ?.takeIf { it in restoredProfileIds }
                ?: availableProfiles.firstOrNull()?.id
                ?: 1
            db.watchHistoryDao().insert(
                WatchHistoryEntity(
                    id = h.id ?: "backup_${h.channelId}_${h.lastWatchedAt}",
                    profileId = historyProfileId,
                    channelId = h.channelId, channelName = h.channelName,
                    mediaType = h.mediaType, positionMs = h.positionMs,
                    durationMs = h.durationMs,
                    progressPercent = h.progressPercent ?: 0f,
                    status = h.status,
                    watchCount = h.watchCount ?: 1,
                    lastWatchedAt = h.lastWatchedAt,
                    firstWatchedAt = h.firstWatchedAt ?: h.lastWatchedAt
                )
            )
            result = result.copy(watchHistoryImported = result.watchHistoryImported + 1)
        }

        // Restore recording schedules. The original row id is carried so re-importing the
        // same backup REPLACEs the schedule instead of inserting a fresh UUID and
        // duplicating it. Old backups without an id get a fresh UUID as before.
        for (r in recordingSchedules) {
            db.recordingDao().insert(
                RecordingEntity(
                    id = r.id ?: UUID.randomUUID().toString(),
                    channelId = r.channelId, channelName = r.channelName,
                    programTitle = r.programTitle,
                    startTimeUtc = r.startTimeUtc, stopTimeUtc = r.stopTimeUtc,
                    recurringRule = r.recurringRule
                )
            )
            result = result.copy(recordingSchedulesImported = result.recordingSchedulesImported + 1)
        }

        // Restore the recording-storage settings row - exported but never written back until
        // now. It is read at export time from RecordingDao.getStorageConfig(); that DAO has
        // no write method and the dao file is outside this change's scope, so the row goes
        // back through Room's SQLite handle directly. Only the three portable fields are
        // restored: localPath/safTreeUri are device-specific (an absolute path or SAF tree
        // grant cannot survive a reinstall) and were not exported either.
        data.recordingStorage?.let { s ->
            db.openHelper.writableDatabase.execSQL(
                "INSERT OR REPLACE INTO recording_storage " +
                    "(id, maxSimultaneous, retentionDays, fileNamePattern) VALUES (?,?,?,?)",
                arrayOf("default", s.maxSimultaneous, s.retentionDays, s.fileNamePattern)
            )
        }

        return result
    }

    private fun IptvProviderConfig.toBackup() = IptvProviderBackup(
        id = id, type = type, name = name, enabled = enabled,
        liveEnabled = liveEnabled, moviesEnabled = moviesEnabled,
        seriesEnabled = seriesEnabled, url = url, username = username,
        password = password, userAgent = userAgent
    )

    private fun ParafusoProfile.toBackup() = ProfileBackup(
        id = id, name = name, child = child, accent = accent
    )

    private fun MediaServerConfig.toBackup() = MediaServerBackup(
        id = id, type = type, name = name, enabled = enabled,
        url = url, altUrls = altUrls, username = username, password = password,
        token = token, userId = userId, accountToken = accountToken,
        liveEnabled = liveEnabled, moviesEnabled = moviesEnabled,
        seriesEnabled = seriesEnabled
    )

    private fun IptvProviderBackup.toConfig() = IptvProviderConfig(
        id = id, type = type, name = name, enabled = enabled,
        liveEnabled = liveEnabled, moviesEnabled = moviesEnabled,
        seriesEnabled = seriesEnabled, url = url, username = username,
        password = password, userAgent = userAgent
    )

    private fun MediaServerBackup.toConfig() = MediaServerConfig(
        id = id, type = type, name = name, enabled = enabled,
        url = url, altUrls = altUrls, username = username, password = password,
        token = token, userId = userId, accountToken = accountToken,
        liveEnabled = liveEnabled, moviesEnabled = moviesEnabled,
        seriesEnabled = seriesEnabled
    )

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally { spec.clearPassword() }
    }

    private fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plain)
        return ENCRYPTED_MAGIC.toByteArray(Charsets.US_ASCII) + salt + iv + encrypted
    }

    private fun decrypt(payload: ByteArray, password: String): ByteArray {
        val magic = ENCRYPTED_MAGIC.toByteArray(Charsets.US_ASCII)
        if (payload.size < magic.size || !payload.copyOfRange(0, magic.size).contentEquals(magic) || payload.size <= magic.size + SALT_BYTES + IV_BYTES) throw IllegalArgumentException("Invalid encrypted backup")
        val saltStart = magic.size
        val ivStart = saltStart + SALT_BYTES
        val cipherStart = ivStart + IV_BYTES
        val salt = payload.copyOfRange(saltStart, ivStart)
        val iv = payload.copyOfRange(ivStart, cipherStart)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(payload.copyOfRange(cipherStart, payload.size))
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
