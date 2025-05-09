package com.appsbyayush.paintspace.models

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appsbyayush.paintspace.customviews.ColorEditText

@Entity(tableName = "user_text_elements_table")
data class UserTextElement(
    @PrimaryKey
    val id: String,

    var text: String,
    var textImageUri: Uri? = null,

    var hasBackground: Boolean,
    var typeface: Typeface,
    var textAlignment: ColorEditText.TextAlignment,
    var textColor: Int,
    var backgroundColor: Int,
    var textBold: Boolean,
    var textItalic: Boolean,
    var textUnderline: Boolean
)