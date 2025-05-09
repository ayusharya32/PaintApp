package com.appsbyayush.paintspace.db

import androidx.room.*
import com.appsbyayush.paintspace.paging.GradientSearchRemoteKey
import com.appsbyayush.paintspace.paging.GraphicElementSearchRemoteKey

@Dao
interface PaintRemoteKeysDao {

    /**
     * GRAPHIC ELEMENT REMOTE KEY TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphicElementSearchRemoteKey(remoteKey: GraphicElementSearchRemoteKey)

    @Delete
    suspend fun deleteGraphicElementSearchRemoteKey(remoteKey: GraphicElementSearchRemoteKey)

    @Query("SELECT * FROM remote_key_graphic_element_table WHERE searchQuery = :searchQuery")
    suspend fun getGraphicElementSearchRemoteKey(searchQuery: String): GraphicElementSearchRemoteKey?

    @Query("DELETE FROM remote_key_graphic_element_table WHERE searchQuery = :searchQuery")
    suspend fun deleteGraphicElementRemoteKeyBySearchQuery(searchQuery: String)

    /**
     * GRADIENT REMOTE KEY TABLE
     */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradientSearchRemoteKey(remoteKey: GradientSearchRemoteKey)

    @Delete
    suspend fun deleteGradientSearchRemoteKey(remoteKey: GradientSearchRemoteKey)

    @Query("SELECT * FROM remote_key_gradient_table WHERE searchQuery = :searchQuery")
    suspend fun getGradientSearchRemoteKey(searchQuery: String): GradientSearchRemoteKey?

    @Query("DELETE FROM remote_key_gradient_table WHERE searchQuery = :searchQuery")
    suspend fun deleteGradientRemoteKeyBySearchQuery(searchQuery: String)
}