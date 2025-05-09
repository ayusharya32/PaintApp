package com.appsbyayush.paintspace.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import com.google.firebase.firestore.Exclude
import java.util.*

@Entity(tableName = "user_graphic_elements_table")
data class UserGraphicElement(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    var userId: String? = null,

    val name: String = "",
    val elementUrl: String = "",
    val addedMaskUrl: String = "",
    val type: GraphicElementType = GraphicElementType.ELEMENT_SIMPLE,
    var modifiedAt: Date = Calendar.getInstance().time,

    @get:Exclude
    var savedElementUri: Uri? = null,

    @get:Exclude
    var savedAddedMaskUri: Uri? = null,

    @get:Exclude
    var isSynced: Boolean = false
)