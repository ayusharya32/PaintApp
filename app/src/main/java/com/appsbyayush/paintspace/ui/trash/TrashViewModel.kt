package com.appsbyayush.paintspace.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbyayush.paintspace.models.Drawing
import com.appsbyayush.paintspace.repo.PaintRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: PaintRepository
): ViewModel() {

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    var trashDrawingsFlow: StateFlow<List<Drawing>> = MutableStateFlow(listOf())

    fun onFragmentStarted() {
        trashDrawingsFlow = repository.getAllTrashedDrawings()
            .stateIn(viewModelScope, SharingStarted.Lazily, listOf())
    }

    fun restoreDrawing(drawing: Drawing) = viewModelScope.launch {
        drawing.isDeleted = false
        repository.insertDrawing(drawing)

        _eventStateFlow.emit(Event.DrawingRestoredSuccess)
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventStateFlow.emit(Event.Idle)
    }

    sealed class Event {
        data object DrawingRestoredSuccess: Event()
        class ErrorOccurred(val exception: Throwable): Event()
        data object Loading: Event()
        data object Idle : Event()
    }
}