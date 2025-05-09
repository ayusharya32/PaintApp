package com.appsbyayush.paintspace.ui.bottomsheets.gradient

import android.app.Application
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.appsbyayush.paintspace.models.Gradient
import com.appsbyayush.paintspace.models.GradientColor
import com.appsbyayush.paintspace.models.UserGradient
import com.appsbyayush.paintspace.repo.PaintRepository
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.enums.GradientType
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
class GradientViewModel @Inject constructor(
    private val repository: PaintRepository,
    private val app: Application
): ViewModel() {
    companion object {
        private const val TAG = "GradientViewMoyyy"
    }

    private val _currentGradientTypeFlow: MutableStateFlow<GradientType> = MutableStateFlow(GradientType.LINEAR)
    var currentGradientTypeFlow = _currentGradientTypeFlow.asStateFlow()

    var internetAvailable = getNetworkStatus(app.applicationContext) != 0
    var selectedUserGradient: UserGradient? = null

    private var searchQueryJob: Job? = null

    private val _eventsStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventsStateFlow.asStateFlow()

    var searchGradientsFlow: Flow<PagingData<Gradient>> = emptyFlow()
    val usedGradientsFlow = repository.getUserGradients()

    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow = _searchQueryFlow.asStateFlow()

    fun onFragmentStarted() {
        searchGradientsFlow = _searchQueryFlow.flatMapLatest { searchQuery ->
            Log.d(TAG, "onFragmentStarted: Changing Flow $searchQuery")
            repository.searchForGradients(searchQuery)
        }.cachedIn(viewModelScope)
    }

    fun onSearchQueryChanged(searchQuery: String) {
        searchQueryJob?.let { job ->
            job.cancel()
            searchQueryJob = null
        }

        searchQueryJob = viewModelScope.launch {
            delay(500)
            _searchQueryFlow.value = searchQuery
        }
    }

    fun updateCurrentGradientType(gradientType: GradientType) {
        _currentGradientTypeFlow.value = gradientType
    }

    fun onCreateBtnClick(gradientColorList: List<GradientColor>,
                         gradientDrawable: Drawable
    ) = viewModelScope.launch {
        val currentAuthUser = repository.getAuthenticatedUser()
        val colors = gradientColorList.filter { it.enabled }
            .map { it.colorHexCodeString }

        val timeStamp = CommonMethods.getFormattedDateTime(Calendar.getInstance().time,
            Constants.DATE_TIME_FORMAT_1)
        val gradientImagePath = app.applicationContext
            .getExternalFilesDir(Constants.DIR_ELEMENTS)?.path + "/ELEMENT$timeStamp.png"

        val userGradient = UserGradient(
            id = selectedUserGradient?.id ?: UUID.randomUUID().toString(),
            userId = selectedUserGradient?.userId ?: currentAuthUser?.uid,
            gradientType = currentGradientTypeFlow.value,
            colors = colors,
            savedImageUri = CommonMethods.saveGradientImage(gradientDrawable, gradientImagePath)
        )

        repository.insertUserGradient(userGradient)
        _eventsStateFlow.emit(Event.UserGradientSaved(userGradient))
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventsStateFlow.emit(Event.Idle)
    }

    sealed class Event {
        class UserGradientSaved(val userGradient: UserGradient) : Event()
        object Idle: Event()
    }
}