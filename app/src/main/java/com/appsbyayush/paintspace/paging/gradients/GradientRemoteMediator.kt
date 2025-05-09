package com.appsbyayush.paintspace.paging.gradients

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.appsbyayush.paintspace.api.PaintApi
import com.appsbyayush.paintspace.db.PaintDatabase
import com.appsbyayush.paintspace.models.Gradient
import com.appsbyayush.paintspace.models.GradientSearchResult
import com.appsbyayush.paintspace.paging.GradientSearchRemoteKey
import com.appsbyayush.paintspace.paging.graphicelements.GraphicElementRemoteMediator
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPagingApi::class)
class GradientRemoteMediator(
    private val searchQuery: String,
    private val paintApi: PaintApi,
    private val paintDatabase: PaintDatabase
): RemoteMediator<Int, Gradient>() {

    companion object {
        private const val TAG = "GradientRemoteMeyyy"
        private const val STARTING_PAGE_NUMBER = 1
    }

    private val paintDao = paintDatabase.getPaintDao()
    private val paintRemoteKeysDao = paintDatabase.getPaintRemoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Gradient>
    ): MediatorResult {

        Log.d(TAG, "load: Called $loadType")

        val currentPage = when(loadType) {
            LoadType.REFRESH -> STARTING_PAGE_NUMBER
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

            LoadType.APPEND -> {
                val remoteKey = paintRemoteKeysDao.getGradientSearchRemoteKey(searchQuery)
                remoteKey?.nextPageKey ?: return MediatorResult.Success(
                    endOfPaginationReached = remoteKey != null)
            }
        }

        try {
            val searchResponse = paintApi.searchOnlineGradients(searchQuery,
                state.config.pageSize, currentPage)

            Log.d(TAG, "load: Search Response -- $searchResponse")
            Log.d(TAG, "load: Search Response Elements -- ${searchResponse.gradients}")

            paintDatabase.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    paintDao.deleteGradientSearchResultsBySearchQuery(searchQuery)
                    paintRemoteKeysDao.deleteGradientRemoteKeyBySearchQuery(searchQuery)
                }

                val lastItemPosition = paintDao.getLastItemPositionOfGradientSearchResult(searchQuery) ?: 0
                val currentItemPosition = lastItemPosition + 1

                val gradientSearchResults = searchResponse.gradients.map { item ->
                    GradientSearchResult(searchQuery, item.id, currentItemPosition)
                }

                paintDao.insertGradientList(searchResponse.gradients)
                paintDao.insertGradientSearchResults(gradientSearchResults)

                val remoteKey = GradientSearchRemoteKey(
                    searchQuery = searchQuery,
                    nextPageKey = currentPage + 1,
                    queryTimestamp = System.currentTimeMillis()
                )
                paintRemoteKeysDao.insertGradientSearchRemoteKey(remoteKey)
            }

            return MediatorResult.Success(endOfPaginationReached = searchResponse.gradients.isEmpty())

        } catch(e: Exception) {
            Log.d(TAG, "Error Occurred: $e")
            return MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction {
        val searchQueryRemoteKey = paintRemoteKeysDao.getGradientSearchRemoteKey(searchQuery)

        val shouldRefresh = if(paintDao.getUserGradients().first().isNotEmpty()) {
            searchQueryRemoteKey?.let { remoteKey ->
                val timeElapsedSinceLastLoad = System.currentTimeMillis() - remoteKey.queryTimestamp
                timeElapsedSinceLastLoad > TimeUnit.MINUTES.toMillis(5)
            } ?: true
        } else {
            true
        }

        return if(shouldRefresh) InitializeAction.LAUNCH_INITIAL_REFRESH
        else InitializeAction.SKIP_INITIAL_REFRESH
    }
}