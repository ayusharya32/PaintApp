package com.appsbyayush.paintspace.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import java.util.*

@Entity(tableName = "font_items_table")
data class FontItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String = "",
    val imgUrl: String = "",
    val fontFileUrl: String = "",
    val createdAt: Date = Date(),

    @get:Exclude
    var fontFileLocalUri: Uri? = null
)
