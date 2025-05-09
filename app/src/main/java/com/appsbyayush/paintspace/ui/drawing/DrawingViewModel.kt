package com.appsbyayush.paintspace.ui.drawing

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbyayush.paintspace.models.Drawing
import com.appsbyayush.paintspace.models.DrawingItem
import com.appsbyayush.paintspace.models.FontItem
import com.appsbyayush.paintspace.models.UserGradient
import com.appsbyayush.paintspace.models.UserGraphicElement
import com.appsbyayush.paintspace.models.UserTextElement
import com.appsbyayush.paintspace.repo.PaintRepository
import com.appsbyayush.paintspace.utils.CommonMethods
import com.appsbyayush.paintspace.utils.Constants
import com.appsbyayush.paintspace.utils.Resource
import com.appsbyayush.paintspace.utils.getNetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val repository: PaintRepository,
    private val app: Application
): ViewModel() {

    companion object {
        private const val TAG = "DrawingViewModelyy"
    }

    var showBrushOptions = true
    var showSelectOptions = true
    var currentUiMode = UIMode.MODE_IDLE

    var currentDrawing: Drawing? = null

    var fontsFlow: Flow<Resource<List<FontItem>>> = emptyFlow()
    private var fontDownloadJob: Job? = null

    private val _internetAvailableFlow: MutableStateFlow<Boolean> =
        MutableStateFlow(getNetworkStatus(app.applicationContext) != 0)
    var internetAvailableFlow = _internetAvailableFlow.asStateFlow()

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    fun onFragmentStarted(currentDrawing: Drawing?, loadDraft: Boolean) {
        this.currentDrawing = currentDrawing

        if(currentDrawing != null) {
            loadCurrentDrawing()
        } else if(loadDraft) {
            loadSavedDrawingDraft()
        }

        fontsFlow = internetAvailableFlow.flatMapLatest { internetAvailable ->
            if(internetAvailable) {
                repository.getFonts()
            } else {
                emptyFlow()
            }
        }
    }

    fun onImagePickedFromGallery(contentUri: Uri) = viewModelScope.launch {
        val compressedFileUri = CommonMethods.getCompressedImageUri(app.applicationContext,
            contentUri, Constants.DIR_ELEMENTS)

        compressedFileUri?.let {
            sendUIEvent(Event.SelectedImageCompressed(it))
        }
    }

    fun onFontItemClicked(fontItem: FontItem) {
        fontItem.fontFileLocalUri?.let {
            sendUIEvent(Event.DownloadFontSuccess(fontItem))
        } ?: startFontDownload(fontItem)
    }

    fun onGraphicElementClicked(userGraphicElement: UserGraphicElement) {
        sendUIEvent(Event.UserGraphicElementReceived(userGraphicElement))
    }

    fun onGradientCreateBtnClicked(userGradient: UserGradient)  {
        sendUIEvent(Event.UserGradientReceived(userGradient))
    }

    fun onEditTextBtnClicked(textElementId: String?) = viewModelScope.launch {
        textElementId?.let { elementId ->
            val textElement = repository.getUserTextElement(elementId)

            if(textElement != null) {
                sendUIEvent(Event.EditUserTextElement(textElement))
            }
        }
    }

    fun onBtnTextDoneClicked(currentText: String) = viewModelScope.launch {
        if(currentText.isEmpty()) {
            sendUIEvent(Event.EmptyTextDone)
            return@launch
        }

        sendUIEvent(Event.SaveTextImage)
    }

    fun onSaveDrawingBtnClicked(drawingEmpty: Boolean) {
        if(drawingEmpty) {
            sendUIEvent(Event.EmptyDrawing)
            return
        }

        sendUIEvent(Event.ProceedToSaveDrawing)
    }

    fun onBackPressedWithCurrentTextElementPresent(userTextElement: UserTextElement) {
        sendUIEvent(Event.UserTextElementSaved(userTextElement))
    }

    fun onTextImageSaved(userTextElement: UserTextElement) = viewModelScope.launch {
        repository.insertUserTextElement(userTextElement)
        sendUIEvent(Event.UserTextElementSaved(userTextElement))
    }

    fun updateInternetStatus() {
        _internetAvailableFlow.value = getNetworkStatus(app.applicationContext) != 0
    }

    fun saveDrawing() {
        sendUIEvent(Event.SaveDrawingImage)
    }

    fun saveDrawingAsDraft(drawingItems: List<DrawingItem>) = viewModelScope.launch {
        drawingItems.forEachIndexed { index, drawingItem ->
            drawingItem.itemPosition = index
        }

        repository.insertDrawingItems(drawingItems)
        sendUIEvent(Event.DrawingSavedAsDraft)
    }

    fun onDrawingImageSaved(drawingImageUri: Uri) = viewModelScope.launch {
        if(currentDrawing != null) {
            currentDrawing?.let {
                it.localDrawingImgUri = drawingImageUri
                it.localShareableDrawingImgContentUri = null

                it.isImageUploaded = false
                it.isSynced = false

                it.modifiedAt = Calendar.getInstance().time
            }

        } else {
            val currentDate = CommonMethods.getFormattedDateTime(Calendar.getInstance().time,
                Constants.DATE_FORMAT_3)

            currentDrawing = Drawing(
                id = UUID.randomUUID().toString(),
                userId = repository.getAuthenticatedUser()?.uid,
                name = "DRAWING $currentDate",
                drawingImgUrl = null,
                localDrawingImgUri = drawingImageUri,
            )
        }

        repository.insertDrawing(currentDrawing!!)
        sendUIEvent(Event.DrawingSaved)
    }

    fun discardChanges() = viewModelScope.launch {
        repository.clearDrawingItemsTable()
        sendUIEvent(Event.ChangesDiscarded)
    }

    fun onEventOccurred() {
        sendUIEvent(Event.Idle)
    }

    private fun loadCurrentDrawing() = viewModelScope.launch {
        currentDrawing?.let { drawing ->
            if(drawing.localDrawingImgUri == null && !drawing.drawingImgUrl.isNullOrEmpty()) {
                repository.saveDrawingImage(drawing)
                repository.insertDrawing(drawing)
            }

            drawing.localDrawingImgUri?.let {
                repository.clearDrawingItemsTable()
                sendUIEvent(Event.LoadCurrentDrawing)
            }
        }
    }

    private fun loadSavedDrawingDraft() = viewModelScope.launch {
        val draftDrawingItems = repository.getAllDrawingItems()
        repository.clearDrawingItemsTable()
        sendUIEvent(Event.LoadDrawingDraft(draftDrawingItems))
    }

    private fun startFontDownload(fontItem: FontItem) {
        if(fontDownloadJob != null) {
            fontDownloadJob?.cancel()
            fontDownloadJob = null
        }

        fontDownloadJob = viewModelScope.launch {
            sendUIEvent(Event.DownloadingFont)

            try {
                repository.downloadFontFile(fontItem)
                sendUIEvent(Event.DownloadFontSuccess(fontItem))

            } catch(e: Exception) {
                sendUIEvent(Event.DownloadFontError(e))
            }
        }
    }

    private fun sendUIEvent(event: Event) = viewModelScope.launch {
        _eventStateFlow.emit(event)
    }

    sealed class Event {
        data object LoadCurrentDrawing: Event()
        class LoadDrawingDraft(val drawingItems: List<DrawingItem>): Event()

        class SelectedImageCompressed(val imageUri: Uri): Event()

        data object DownloadingFont : Event()
        class DownloadFontSuccess(val fontItem: FontItem): Event()
        class DownloadFontError(val exception: Throwable): Event()

        data object EmptyTextDone: Event()
        data object SaveTextImage: Event()
        class UserTextElementSaved(val userTextElement: UserTextElement): Event()
        class EditUserTextElement(val userTextElement: UserTextElement): Event()

        class UserGraphicElementReceived(val userGraphicElement: UserGraphicElement): Event()
        class UserGradientReceived(val userGradient: UserGradient): Event()

        data object EmptyDrawing: Event()
        data object ProceedToSaveDrawing: Event()
        data object SaveDrawingImage: Event()
        data object DrawingSaved: Event()
        data object DrawingSavedAsDraft: Event()

        data object ChangesDiscarded: Event()

        class ErrorOccurred(val exception: Throwable): Event()
        data object Idle : Event()
    }

    enum class UIMode {
        MODE_IDLE,
        MODE_BRUSH,
        MODE_SELECTION,
        MODE_TEXT
    }
}