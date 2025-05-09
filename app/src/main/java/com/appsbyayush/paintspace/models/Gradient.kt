package com.appsbyayush.paintspace.models

import android.graphics.drawable.GradientDrawable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appsbyayush.paintspace.utils.enums.GradientType
import java.util.*

@Entity(tableName = "gradients_table")
data class Gradient(
    @PrimaryKey
    val id: String,

    val name: String,
    val colors: List<String>,

    val createdAt: Date,
)

@Entity(tableName = "gradient_search_results_table", primaryKeys = ["searchQuery", "itemId"])
data class GradientSearchResult (
    val searchQuery: String,
    val itemId: String,
    val itemPosition: Int
)