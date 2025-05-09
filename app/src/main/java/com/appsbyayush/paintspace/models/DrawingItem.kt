package com.appsbyayush.paintspace.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appsbyayush.paintspace.utils.enums.DrawingItemType
import java.util.*

@Entity(tableName = "drawing_items_table")
data class DrawingItem(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    var type: DrawingItemType,

    var drawingPath: DrawingPath? = null,
    var drawingImage: DrawingImage? = null,
    var drawingText: DrawingText? = null,

    var eraseBrushOn: Boolean = false,

    var createdAt: Date = Calendar.getInstance().time,
    var modifiedAt: Date = Calendar.getInstance().time,

    var transparency: Int = 0,
    var isLocked: Boolean = false,

    var itemPosition: Int = 0
)