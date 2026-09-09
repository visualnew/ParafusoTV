package com.lumora.data.backup

import com.google.gson.Gson
import com.lumora.ParafusoProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupProfileDataTest {

    @Test
    fun `old backup without profile fields remains parseable`() {
        val legacy = Gson().fromJson(
            """{"version":1,"createdAt":"old","favoriteSeries":["legacy"]}""",
            BackupManager.BackupData::class.java
        )

        assertNull(legacy.profiles)
        assertNull(legacy.activeProfileId)
        assertNull(legacy.profileFavorites)
        assertNull(legacy.profilePlaybackPositions)
        assertNull(legacy.profileWatchedMarks)
        assertEquals(listOf("legacy"), legacy.favoriteSeries)
    }

    @Test
    fun `profile backup keeps ids and personal data separate`() {
        val source = BackupManager.BackupData(
            profiles = listOf(
                BackupManager.ProfileBackup(1, "Principal"),
                BackupManager.ProfileBackup(7, "Infantil", child = true)
            ),
            activeProfileId = 7,
            profileFavorites = listOf(
                BackupManager.ProfileFavoritesBackup(1, listOf("series-one"), listOf("channel-one")),
                BackupManager.ProfileFavoritesBackup(7, listOf("series-child"), emptyList())
            ),
            profilePlaybackPositions = listOf(
                BackupManager.ProfilePlaybackPositionsBackup(1, "{\"one\":{}}"),
                BackupManager.ProfilePlaybackPositionsBackup(7, "{\"child\":{}}")
            ),
            profileWatchedMarks = listOf(
                BackupManager.ProfileWatchedMarksBackup(1, listOf("m|one")),
                BackupManager.ProfileWatchedMarksBackup(7, listOf("m|child"))
            )
        )

        val restored = Gson().fromJson(Gson().toJson(source), BackupManager.BackupData::class.java)

        assertEquals(listOf(1, 7), restored.profiles!!.map { it.id })
        assertEquals(listOf("series-one"), restored.profileFavorites!!.single { it.profileId == 1 }.favoriteSeries)
        assertEquals(listOf("series-child"), restored.profileFavorites!!.single { it.profileId == 7 }.favoriteSeries)
        assertEquals("{\"one\":{}}", restored.profilePlaybackPositions!!.single { it.profileId == 1 }.payload)
        assertEquals("{\"child\":{}}", restored.profilePlaybackPositions!!.single { it.profileId == 7 }.payload)
        assertEquals(listOf("m|one"), restored.profileWatchedMarks!!.single { it.profileId == 1 }.marks)
        assertEquals(listOf("m|child"), restored.profileWatchedMarks!!.single { it.profileId == 7 }.marks)
    }

    @Test
    fun `invalid saved active profile falls back to an existing profile`() {
        val profiles = listOf(ParafusoProfile(4, "Quatro"), ParafusoProfile(9, "Nove"))

        assertEquals(4, BackupManager.resolveActiveProfileId(profiles, 99))
        assertEquals(9, BackupManager.resolveActiveProfileId(profiles, 9))
    }
}
