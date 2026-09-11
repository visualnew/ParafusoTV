package com.lumora.model

data class ContentShelf(
    val title: String,
    val items: List<Channel>,
    val pinned: Boolean = false,
    val categoryId: String? = null,
    /** Whether the source contains items beyond the horizontal shelf's visible subset. */
    val hasMoreItems: Boolean = true
)
