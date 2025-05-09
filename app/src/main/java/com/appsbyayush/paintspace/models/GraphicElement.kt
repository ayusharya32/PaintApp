package com.appsbyayush.paintspace.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import com.google.firebase.firestore.Exclude
import java.util.*

@Entity(tableName = "graphic_elements_table")
data class GraphicElement(
    @PrimaryKey
    val id: String,

    val name: String,
    val elementUrl: String = "",
    val addedMaskUrl: String = "",
    val type: GraphicElementType,
    val createdAt: Date,
)

@Entity(tableName = "graphic_element_search_results_table", primaryKeys = ["searchQuery", "itemId"])
data class GraphicElementSearchResult (
    val searchQuery: String,
    val itemId: String,
    val itemPosition: Int
)
