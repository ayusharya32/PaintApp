package com.appsbyayush.paintspace.api

import com.appsbyayush.paintspace.models.Gradient

data class GradientSearchResponse(
    val totalPages: Int,
    val totalHits: Int,
    val pageNumber: Int,
    val pageSize: Int,

    val gradients: List<Gradient>
)