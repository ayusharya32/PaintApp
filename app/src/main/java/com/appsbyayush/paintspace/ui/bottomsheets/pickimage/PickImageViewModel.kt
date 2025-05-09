package com.appsbyayush.paintspace.ui.bottomsheets.pickimage

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.appsbyayush.paintspace.models.GraphicElement
import com.appsbyayush.paintspace.models.UserGraphicElement
import com.appsbyayush.paintspace.repo.PaintRepository
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.enums.GraphicElementType
import com.appsbyayush.paintspace.utils.getNetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PickImageViewModel @Inject constructor(
    private val repository: PaintRepository,
    private val app: Application
): ViewModel() {
    companion object {
        private const val TAG = "PickImageViewMoyyy"
    }

    var internetAvailable = getNetworkStatus(app.applicationContext) != 0
    var searchMode: Boolean = false
    private var searchQueryJob: Job? = null

    private val _eventsStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventsStateFlow.asStateFlow()

    var searchGraphicElementsFlow: Flow<PagingData<GraphicElement>> = emptyFlow()
    var userGraphicElementsFlow = repository.getUserGraphicElements()

    private val _searchQueryFlow = MutableStateFlow(Pair("", GraphicElementType.ELEMENT_SIMPLE))
    val searchQueryFlow = _searchQueryFlow.asStateFlow()

    fun onFragmentStarted() {
        searchGraphicElementsFlow = _searchQueryFlow.flatMapLatest { (searchQuery, elementType) ->
            val finalSearchQuery = if(!searchMode) {
                "$searchQuery $elementType"
            } else {
                searchQuery
            }

            Log.d(TAG, "onFragmentStarted: Changing Flow $finalSearchQuery")
            repository.searchForGraphicElements(finalSearchQuery)
        }.cachedIn(viewModelScope)
    }

    fun onSearchQueryChanged(searchQuery: String) {
        searchQueryJob?.let { job ->
            job.cancel()
            searchQueryJob = null
        }

        searchQueryJob = viewModelScope.launch {
            delay(500)
            _searchQueryFlow.update { (_, currentElementType) ->
                Pair(searchQuery, currentElementType)
            }
        }
    }

    fun onElementTypeChanged(elementType: GraphicElementType) {
        _searchQueryFlow.update { (currentSearchQuery, _) ->
            Pair(currentSearchQuery, elementType)
        }
    }

    fun onGraphicElementClicked(graphicElement: GraphicElement) = viewModelScope.launch {
        val currentAuthUser = repository.getAuthenticatedUser()
        val timeStamp = CommonMethods.getFormattedDateTime(Calendar.getInstance().time,
            Constants.DATE_TIME_FORMAT_1)

        val elementImagePath = app.applicationContext
            .getExternalFilesDir(Constants.DIR_ELEMENTS)?.path + "/ELEMENT$timeStamp.png"

        val elementLocalUri = CommonMethods.saveImageFromUrl(app.applicationContext,
            graphicElement.elementUrl, elementImagePath)

        val addedMaskLocalUri = if(graphicElement.addedMaskUrl.isNotEmpty()) {
            val maskImagePath = app.applicationContext
                .getExternalFilesDir(Constants.DIR_ELEMENTS)?.path + "/MASK$timeStamp.png"

            CommonMethods.saveImageFromUrl(app.applicationContext, graphicElement.addedMaskUrl, maskImagePath)
        } else {
            null
        }

        val userGraphicElement = UserGraphicElement(
            id = graphicElement.id,
            userId = currentAuthUser?.uid,
            name = graphicElement.name,
            elementUrl = graphicElement.elementUrl,
            addedMaskUrl = graphicElement.addedMaskUrl,
            type = graphicElement.type,

            savedElementUri = elementLocalUri,
            savedAddedMaskUri = addedMaskLocalUri
        )

        repository.insertUserGraphicElement(userGraphicElement)
        _eventsStateFlow.emit(Event.UserGraphicElementSaved(userGraphicElement))
    }

    fun onUserGraphicElementClicked(userGraphicElement: UserGraphicElement) = viewModelScope.launch {
        val timeStamp = CommonMethods.getFormattedDateTime(Calendar.getInstance().time,
            Constants.DATE_TIME_FORMAT_1)

        if(userGraphicElement.savedElementUri == null) {
            val elementImagePath = app.applicationContext
                .getExternalFilesDir(Constants.DIR_ELEMENTS)?.path + "/ELEMENT$timeStamp.png"

            userGraphicElement.savedElementUri = CommonMethods.saveImageFromUrl(app.applicationContext,
                userGraphicElement.elementUrl, elementImagePath)
        }

        if(userGraphicElement.addedMaskUrl.isNotEmpty() && userGraphicElement.savedAddedMaskUri == null) {
            val maskImagePath = app.applicationContext
                .getExternalFilesDir(Constants.DIR_ELEMENTS)?.path + "/MASK$timeStamp.png"

            userGraphicElement.savedAddedMaskUri = CommonMethods.saveImageFromUrl(app.applicationContext,
                userGraphicElement.addedMaskUrl, maskImagePath)
        }

        userGraphicElement.apply {
            modifiedAt = Calendar.getInstance().time
            isSynced = false
        }

        repository.insertUserGraphicElement(userGraphicElement)
        _eventsStateFlow.emit(Event.UserGraphicElementSaved(userGraphicElement))
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventsStateFlow.emit(Event.Idle)
    }

    sealed class Event {
        class UserGraphicElementSaved(val userGraphicElement: UserGraphicElement): Event()
        data object Idle: Event()
    }
}