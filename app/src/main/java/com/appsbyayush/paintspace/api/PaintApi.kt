package com.appsbyayush.paintspace.api

import retrofit2.http.GET
import retrofit2.http.Query

interface PaintApi {
    @GET("search/graphics")
    suspend fun searchOnlineDrawingElements(
        @Query("searchQuery") searchQuery: String,
        @Query("pageSize") pageSize: Int,
        @Query("pageNumber") pageNumber: Int
    ): GraphicElementSearchResponse

    @GET("search/gradients")
    suspend fun searchOnlineGradients(
        @Query("searchQuery") searchQuery: String,
        @Query("pageSize") pageSize: Int,
        @Query("pageNumber") pageNumber: Int
    ): GradientSearchResponse
}