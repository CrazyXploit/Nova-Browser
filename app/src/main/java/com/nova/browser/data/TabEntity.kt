package com.nova.browser.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val lastActive: Long,
    val isIncognito: Boolean = false,
)