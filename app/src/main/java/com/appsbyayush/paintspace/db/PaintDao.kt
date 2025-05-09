package com.appsbyayush.paintspace.db

import androidx.paging.PagingSource
import androidx.room.*
import com.appsbyayush.paintspace.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PaintDao {

    /**
     * DRAWINGS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: Drawing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawingsList(drawingList: List<Drawing>)

    @Query("SELECT * FROM drawings_table ORDER BY modifiedAt DESC")
    fun getAllDrawings(): Flow<List<Drawing>>

    @Query("SELECT * FROM drawings_table WHERE isDeleted = 0 ORDER BY modifiedAt DESC")
    fun getAllUntrashedDrawings(): Flow<List<Drawing>>

    @Query("SELECT * FROM drawings_table WHERE name LIKE :searchQuery AND isDeleted = 0 ORDER BY modifiedAt DESC")
    fun searchDrawings(searchQuery: String): Flow<List<Drawing>>

    @Query("SELECT * FROM drawings_table WHERE isSynced = 0 ORDER BY modifiedAt")
    suspend fun getAllUnsyncedDrawings(): List<Drawing>

    @Query("SELECT * FROM drawings_table WHERE isDeleted = 1 ORDER BY modifiedAt DESC")
    fun getAllTrashedDrawings(): Flow<List<Drawing>>

    @Query("DELETE FROM drawings_table")
    suspend fun clearDrawingsTable()

    @Query("SELECT * FROM drawings_table WHERE isDeleted = 1 AND modifiedAt < :timestamp")
    suspend fun getTrashedDrawingsOlderThanTimestamp(timestamp: Long): List<Drawing>

    @Query("DELETE FROM drawings_table WHERE isDeleted = 1 AND modifiedAt < :timestamp")
    suspend fun clearTrashedDrawingsOlderThanTimestamp(timestamp: Long)

    /**
     * GRAPHIC ELEMENTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphicElement(graphicElement: GraphicElement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphicElementList(graphicElementsList: List<GraphicElement>)

    @Query("SELECT * FROM graphic_elements_table INNER JOIN graphic_element_search_results_table " +
            "ON id = itemId WHERE searchQuery = :searchQuery ORDER BY itemPosition")
    fun getGraphicElementsForSearchQuery(searchQuery: String): PagingSource<Int, GraphicElement>

    @Query("DELETE FROM graphic_elements_table")
    suspend fun clearGraphicElementsTable()

    /**
     * GRAPHIC ELEMENT SEARCH RESULTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphicElementSearchResults(graphicElementSearchResult: List<GraphicElementSearchResult>)

    @Query("SELECT MAX(itemPosition) FROM graphic_element_search_results_table " +
            "WHERE searchQuery = :searchQuery")
    suspend fun getLastItemPositionOfGraphicElementSearchResult(searchQuery: String): Int?

    @Query("DELETE FROM graphic_element_search_results_table WHERE searchQuery = :searchQuery")
    suspend fun deleteGraphicElementSearchResultsBySearchQuery(searchQuery: String)

    @Query("DELETE FROM graphic_element_search_results_table")
    suspend fun clearGraphicElementSearchResultsTable()

    /**
     * GRADIENTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradient(gradient: Gradient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradientList(gradientList: List<Gradient>)

    @Query("SELECT * FROM gradients_table INNER JOIN gradient_search_results_table " +
            "ON id = itemId WHERE searchQuery = :searchQuery ORDER BY itemPosition")
    fun getGradientsForSearchQuery(searchQuery: String): PagingSource<Int, Gradient>

    @Query("DELETE FROM gradients_table")
    suspend fun clearGradientsTable()

    /**
     * GRADIENT SEARCH RESULTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradientSearchResults(gradientSearchResult: List<GradientSearchResult>)

    @Query("SELECT MAX(itemPosition) FROM gradient_search_results_table " +
            "WHERE searchQuery = :searchQuery")
    suspend fun getLastItemPositionOfGradientSearchResult(searchQuery: String): Int?

    @Query("DELETE FROM gradient_search_results_table WHERE searchQuery = :searchQuery")
    suspend fun deleteGradientSearchResultsBySearchQuery(searchQuery: String)

    @Query("DELETE FROM gradient_search_results_table")
    suspend fun clearGradientSearchResultsTable()

    /**
     * FONTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFontItem(fontItem: FontItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFontItemsList(fontItemsList: List<FontItem>)

    @Query("SELECT * FROM font_items_table ORDER BY name")
    fun getFontItems(): Flow<List<FontItem>>

    @Query("DELETE FROM font_items_table WHERE id = :fontId")
    suspend fun deleteFontItem(fontId: String)

    @Query("DELETE FROM font_items_table")
    suspend fun clearFontItemsTable()

    /**
     * USER GRAPHIC ELEMENTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGraphicElement(userGraphicElement: UserGraphicElement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGraphicElementList(userGraphicElement: List<UserGraphicElement>)

    @Query("SELECT * FROM user_graphic_elements_table ORDER BY modifiedAt DESC")
    fun getUserGraphicElements(): Flow<List<UserGraphicElement>>

    @Query("DELETE FROM user_graphic_elements_table WHERE id = :elementId")
    suspend fun deleteUserGraphicElement(elementId: String)

    @Query("DELETE FROM user_graphic_elements_table")
    suspend fun clearUserGraphicElementsTable()

    /**
     * USER GRADIENTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGradient(userGradient: UserGradient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGradientsList(userGradientList: List<UserGradient>)

    @Query("SELECT * FROM user_gradients_table ORDER BY modifiedAt DESC")
    fun getUserGradients(): Flow<List<UserGradient>>

    @Query("DELETE FROM user_gradients_table WHERE id = :gradientId")
    suspend fun deleteUserGradient(gradientId: String)

    @Query("DELETE FROM user_gradients_table")
    suspend fun clearUserGradientsTable()

    /**
     * USER TEXT ELEMENTS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserTextElement(userTextElement: UserTextElement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserTextElementList(userTextElementList: List<UserTextElement>)

    @Query("SELECT * FROM user_text_elements_table WHERE id = :elementId")
    suspend fun getUserTextElement(elementId: String): UserTextElement?

    @Query("SELECT * FROM user_text_elements_table")
    suspend fun getAllUserTextElements(): List<UserTextElement>

    @Query("DELETE FROM user_text_elements_table")
    suspend fun clearUserTextElementsTable()

    /**
     * DRAWING ITEMS TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawingItem(drawingItem: DrawingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawingItemsList(drawingsItemsList: List<DrawingItem>)

    @Query("SELECT * FROM drawing_items_table ORDER BY itemPosition")
    suspend fun getDrawingItems(): List<DrawingItem>

    @Query("SELECT COUNT(*) FROM drawing_items_table")
    suspend fun getDrawingItemsCount(): Int

    @Query("DELETE FROM drawing_items_table")
    suspend fun clearDrawingItemsTable()
}