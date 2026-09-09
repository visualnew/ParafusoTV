package com.lumora.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lumora.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchHistoryMigrationTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LumoraDatabase::class.java,
        emptyList()
    )

    private var roomDatabase: LumoraDatabase? = null

    @After
    fun tearDown() {
        roomDatabase?.close()
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun migrationFrom4PreservesLegacyRowsAndScopesFutureRows() = runBlocking {
        migrationHelper.createDatabase(TEST_DB_NAME, 4).apply {
            execSQL(
                "INSERT INTO `watch_history` (" +
                    "`id`, `channelId`, `channelName`, `mediaType`, `positionMs`, " +
                    "`durationMs`, `progressPercent`, `status`, `watchCount`, " +
                    "`firstWatchedAt`, `lastWatchedAt`) VALUES (" +
                    "'same-id', 'channel-1', 'Legacy title', 'MOVIE', 1200, 3600, " +
                    "33.5, 'IN_PROGRESS', 4, 100, 200)"
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DB_NAME,
            5,
            true,
            LumoraDatabase.MIGRATION_4_5
        ).close()

        roomDatabase = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LumoraDatabase::class.java,
            TEST_DB_NAME
        ).allowMainThreadQueries().build()
        val dao = roomDatabase!!.watchHistoryDao()

        val legacy = dao.getAllForProfile(1).single()
        assertEquals(1, legacy.profileId)
        assertEquals("same-id", legacy.id)
        assertEquals("channel-1", legacy.channelId)
        assertEquals("Legacy title", legacy.channelName)
        assertEquals("MOVIE", legacy.mediaType)
        assertEquals(1200L, legacy.positionMs)
        assertEquals(3600L, legacy.durationMs)
        assertEquals(33.5f, legacy.progressPercent)
        assertEquals("IN_PROGRESS", legacy.status)
        assertEquals(4, legacy.watchCount)
        assertEquals(100L, legacy.firstWatchedAt)
        assertEquals(200L, legacy.lastWatchedAt)

        dao.insert(
            WatchHistoryEntity(
                profileId = 2,
                id = "same-id",
                channelId = "channel-2",
                channelName = "Second profile title",
                mediaType = "MOVIE",
                positionMs = 500,
                durationMs = 1000,
                progressPercent = 50f,
                status = "IN_PROGRESS",
                watchCount = 1,
                firstWatchedAt = 300,
                lastWatchedAt = 400
            )
        )

        assertEquals(listOf("same-id"), dao.getRecentForProfile(1).map { it.id })
        assertEquals(listOf("same-id"), dao.getRecentForProfile(2).map { it.id })
        assertEquals(2, dao.getAllForBackup().size)

        dao.deleteForProfile(1)

        assertTrue(dao.getAllForProfile(1).isEmpty())
        assertEquals("Second profile title", dao.getAllForProfile(2).single().channelName)
    }

    private companion object {
        const val TEST_DB_NAME = "watch-history-migration-test"
    }
}
