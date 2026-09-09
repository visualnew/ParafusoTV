package com.lumora.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "watch_history",
    primaryKeys = ["profileId", "id"],
    indices = [
        Index(value = ["profileId", "lastWatchedAt"]),
        Index(value = ["profileId", "channelId"])
    ]
)
data class WatchHistoryEntity(
    /** Profile 1 owns every row written before the profile-aware schema existed. */
    val profileId: Int = 1,
    val id: String,
    val channelId: String,
    val channelName: String,
    val mediaType: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val progressPercent: Float = 0f,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, COMPLETED_AUTO, COMPLETED_MANUAL
    val watchCount: Int = 1,
    val firstWatchedAt: Long = System.currentTimeMillis(),
    val lastWatchedAt: Long = System.currentTimeMillis()
)
