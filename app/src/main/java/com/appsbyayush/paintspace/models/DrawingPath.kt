package com.appsbyayush.paintspace.models

import android.graphics.MaskFilter
import android.graphics.Path
import android.graphics.PointF
import androidx.room.Ignore
import com.appsbyayush.paintspace.utils.enums.BrushType
import java.io.Serializable

data class DrawingPath(
    val pathPoints: MutableList<PointF>,
    val strokeColor: Int,
    val strokeWidth: Float,
    val brushType: BrushType?,

    @Ignore
    var path: Path
): Serializable