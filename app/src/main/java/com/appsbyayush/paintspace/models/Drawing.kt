package com.appsbyayush.paintspace.models

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@Entity(tableName = "drawings_table")
data class Drawing(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    var userId: String? = null,

    var name: String = "",
    var drawingImgUrl: String? = null,
    var modifiedAt: Date = Calendar.getInstance().time,

    var isImageUploaded: Boolean = false,
    var isDeleted: Boolean = false,

    @get:Exclude
    var localDrawingImgUri: Uri? = null,

    @get:Exclude
    var localShareableDrawingImgContentUri: Uri? = null,

    @get:Exclude
    var isSynced: Boolean = false
): Parcelable