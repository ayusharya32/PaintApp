package com.appsbyayush.paintspace.paging

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_key_graphic_element_table")
data class GraphicElementSearchRemoteKey(
    @PrimaryKey
    val searchQuery: String,

    val nextPageKey: Int,
    val queryTimestamp: Long
)

@Entity(tableName = "remote_key_gradient_table")
data class GradientSearchRemoteKey(
    @PrimaryKey
    val searchQuery: String,

    val nextPageKey: Int,
    val queryTimestamp: Long
)