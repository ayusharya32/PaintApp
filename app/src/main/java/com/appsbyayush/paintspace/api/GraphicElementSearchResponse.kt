package com.appsbyayush.paintspace.api

import com.appsbyayush.paintspace.models.GraphicElement

data class GraphicElementSearchResponse(
    val totalPages: Int,
    val totalHits: Int,
    val pageNumber: Int,
    val pageSize: Int,

    val elements: List<GraphicElement>
)