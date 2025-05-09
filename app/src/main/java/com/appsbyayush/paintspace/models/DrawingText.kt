package com.appsbyayush.paintspace.models

import android.graphics.*
import android.net.Uri
import android.os.Parcelable
import androidx.room.Ignore
import com.appsbyayush.paintspace.customviews.ColorEditText
import kotlinx.parcelize.Parcelize

data class DrawingText(
    val textElementId: String,
    val textImageUri: Uri,
    val rectF: RectF,

    var height: Float,
    var width: Float,
    var rotateAngle: Float = 0F,
    var newAngleChange: Float = 0F,

    @Ignore
    var bitmap: Bitmap? = null
)