package com.appsbyayush.paintspace.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.appsbyayush.paintspace.utils.enums.GradientType
import com.google.firebase.firestore.Exclude
import java.util.*

@Entity(tableName = "user_gradients_table")
data class UserGradient(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    var userId: String? = null,

    var gradientType: GradientType = GradientType.LINEAR,
    var colors: List<String> = listOf(),

    var modifiedAt: Date = Calendar.getInstance().time,

    @get:Exclude
    var isSynced: Boolean = false,

    @Ignore
    @get:Exclude
    var savedImageUri: Uri? = null
)
