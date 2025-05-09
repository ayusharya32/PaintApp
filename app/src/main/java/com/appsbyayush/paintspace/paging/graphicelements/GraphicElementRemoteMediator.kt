package com.appsbyayush.paintspace.paging.graphicelements

import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.appsbyayush.paintspace.api.PaintApi
import com.appsbyayush.paintspace.db.PaintDatabase
import com.appsbyayush.paintspace.models.GraphicElement
import com.appsbyayush.paintspace.models.GraphicElementSearchResult
import com.appsbyayush.paintspace.paging.GraphicElementSearchRemoteKey
import kotlinx.coroutines.flow.first
import java.lang.Exception
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPagingApi::class)
class GraphicElementRemoteMediator(
    private val searchQuery: String,
    private val paintApi: PaintApi,
    private val paintDatabase: PaintDatabase
): RemoteMediator<Int, GraphicElement>() {

    companion object {
        private const val TAG = "GraphicElementRemyy"
        private const val STARTING_PAGE_NUMBER = 1
    }

    private val paintDao = paintDatabase.getPaintDao()
    private val paintRemoteKeysDao = paintDatabase.getPaintRemoteKeysDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, GraphicElement>
    ): MediatorResult {

        Log.d(TAG, "load: Called $loadType")

        val currentPage = when(loadType) {
            LoadType.REFRESH -> STARTING_PAGE_NUMBER
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

            LoadType.APPEND -> {
                val remoteKey = paintRemoteKeysDao.getGraphicElementSearchRemoteKey(searchQuery)
                remoteKey?.nextPageKey ?: return MediatorResult.Success(
                    endOfPaginationReached = remoteKey != null)
            }
        }

        try {
            val searchResponse = paintApi.searchOnlineDrawingElements(searchQuery,
                state.config.pageSize, currentPage)

            Log.d(TAG, "load: Search Response -- $searchResponse")
            Log.d(TAG, "load: Search Response Elements -- ${searchResponse.elements}")

//            val graphicElementsUsed = paintDao.getUsedGraphicElements().first()

//            val retrievedGraphicElements = searchResponse.elements.onEach { element ->
//                element.elementUsed = graphicElementsUsed.any { it.id == element.id }
//            }

            paintDatabase.withTransaction {
                if(loadType == LoadType.REFRESH) {
                    paintDao.deleteGraphicElementSearchResultsBySearchQuery(searchQuery)
                    paintRemoteKeysDao.deleteGraphicElementRemoteKeyBySearchQuery(searchQuery)
                }

                val lastItemPosition =
                    paintDao.getLastItemPositionOfGraphicElementSearchResult(searchQuery) ?: 0
                var currentItemPosition = lastItemPosition + 1

                val graphicElementSearchResults = searchResponse.elements.map {
                    GraphicElementSearchResult(searchQuery, it.id, currentItemPosition++)
                }

                paintDao.insertGraphicElementList(searchResponse.elements)
                paintDao.insertGraphicElementSearchResults(graphicElementSearchResults)

                val remoteKey = GraphicElementSearchRemoteKey(
                    searchQuery = searchQuery,
                    nextPageKey = currentPage + 1,
                    queryTimestamp = System.currentTimeMillis()
                )
                paintRemoteKeysDao.insertGraphicElementSearchRemoteKey(remoteKey)
            }

            return MediatorResult.Success(endOfPaginationReached = searchResponse.elements.isEmpty())

        } catch(e: Exception) {
            Log.d(TAG, "Error $e")
            return MediatorResult.Error(e)
        }
    }

    override suspend fun initialize(): InitializeAction {
        val searchQueryRemoteKey = paintRemoteKeysDao.getGraphicElementSearchRemoteKey(searchQuery)

        val shouldRefresh = if(paintDao.getUserGraphicElements().first().isNotEmpty()) {
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