package com.appsbyayush.paintspace.models

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Parcelable
import androidx.room.Ignore
import com.appsbyayush.paintspace.utils.enums.DrawingImageType
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import kotlinx.parcelize.Parcelize

data class DrawingImage(
    var savedElementId: String? = null,
    var imageType: DrawingImageType = DrawingImageType.SIMPLE,
    var graphicElementType: GraphicElementType = GraphicElementType.ELEMENT_SIMPLE,

    val uri: Uri,
    val rectF: RectF,
    var height: Float,
    var width: Float,
    var rotateAngle: Float = 0F,
    var newAngleChange: Float = 0F,

    var addedMaskUri: Uri? = null,
    var addedMaskRectF: RectF? = null,

    var mergedImageUri: Uri? = null,
    var mergedImageRectF: RectF? = null,

    @Ignore
    var bitmap: Bitmap? = null,

    @Ignore
    val addedMaskBitmap: Bitmap? = null,

    @Ignore
    val mergedImageBitmap: Bitmap? = null
)