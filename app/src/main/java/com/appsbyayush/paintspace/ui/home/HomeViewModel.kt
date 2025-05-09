package com.appsbyayush.paintspace.ui.home

import android.Manifest
import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.models.AppSettings
import com.appsbyayush.paintspace.models.Drawing
import com.appsbyayush.paintspace.repo.PaintRepository
import com.appsbyayush.paintspace.utils.*
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PaintRepository,
    private val permissionManager: PermissionManager,
    private val app: Application
): ViewModel() {

    companion object {
        private const val TAG = "HomeViewModelyy"
    }

    var loggedInUser = repository.getAuthenticatedUser()
//    var signInClient: SignInClient? = null
    var onboardingDone: Boolean = repository.isOnboardingDone()

    var drawingPendingForCopyingToDevice: Drawing? = null

    var drawingsFlow: StateFlow<Resource<List<Drawing>>> = MutableStateFlow(Resource.Loading())

    private val _unsavedDrawingPresent: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val unsavedDrawingPresent = _unsavedDrawingPresent.asStateFlow()

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    private val _appSettingsStateFlow = MutableStateFlow(AppSettings())
    val appSettings = _appSettingsStateFlow.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")
    val searchQuery = _searchQueryFlow.asStateFlow()

    init {
        Log.d(TAG, "User: ${loggedInUser?.email}: ")
    }

    fun onFragmentStarted() {
        drawingsFlow = _searchQueryFlow.flatMapLatest { searchQuery ->
            Log.d(TAG, "onFragmentStarted: Search Drawings $searchQuery")
            repository.getDrawings(searchQuery).map {
                Resource.Success(it)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading())

        _appSettingsStateFlow.value = repository.getAppSettings()
        loggedInUser = repository.getAuthenticatedUser()
    }

    fun checkUnsavedDrawing() = viewModelScope.launch {
        _unsavedDrawingPresent.value = repository.isDrawingDraftPresent()
    }

    fun setOnboardingDone() {
        repository.setOnboardingDone()
    }

    fun updateSearchQuery(searchQuery: String) {
        _searchQueryFlow.value = searchQuery
    }

    fun onDrawingClicked(currentDrawing: Drawing) = viewModelScope.launch {
        if(repository.isDrawingDraftPresent()) {
            sendUIEvent(Event.UnsavedDrawingStatus(
                unsavedDrawingFound = true,
                drawing = currentDrawing
            ))
            return@launch
        }

        sendUIEvent(Event.LoadDrawing(currentDrawing))
    }

    fun onBtnDiscardAndContinueClick(drawing: Drawing?) {
        drawing?.let {
            sendUIEvent(Event.LoadDrawing(it))
        }
    }

    fun onAddDrawingBtnClicked() = viewModelScope.launch {
        if(repository.isDrawingDraftPresent()) {
            sendUIEvent(Event.UnsavedDrawingStatus(true))
            return@launch
        }

        sendUIEvent(Event.UnsavedDrawingStatus(false))
    }

    fun onBtnNewDrawingClick() = viewModelScope.launch {
        repository.clearDrawingItemsTable()
        repository.deleteAllTemporaryMediaFiles()

        sendUIEvent(Event.UnsavedDrawingStatus(false))
    }

    fun onDrawingCopyToDeviceBtnClick(drawing: Drawing) = viewModelScope.launch(Dispatchers.IO) {
        val contentResolver = app.applicationContext.contentResolver

        val deviceFileUri = if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if(!writeExternalStoragePermissionGranted()) {
                drawingPendingForCopyingToDevice = drawing
                return@launch
            }

            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: Below Android Q")
            val dirPath = "${Environment.getExternalStorageDirectory().absolutePath}/PaintSpace"
            val dir = File(dirPath)

            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: DIR path: $dirPath")
            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: DIR Created: ${dir.mkdir()}")

            val file = File("$dirPath/${drawing.name}.png")
            Uri.fromFile(file)
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, drawing.name)
                put(MediaStore.MediaColumns.MIME_TYPE, Constants.IMAGE_TYPE_PNG)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PaintSpace")
            }

            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        if(deviceFileUri == null || drawing.localDrawingImgUri == null) {
            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: Some Error Occurred")
            sendUIEvent(Event.ErrorOccurred(Exception("Error copying drawing to device")))
            return@launch
        }


        try {
            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: Starting TRY")
            val deviceFileOutputStream = contentResolver.openOutputStream(deviceFileUri)
            val localFileBitmap = CommonMethods.getImageBitmapFromUri(app.applicationContext,
                drawing.localDrawingImgUri!!)

            deviceFileOutputStream?.let {
                localFileBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
                it.close()
            }

            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: TRY DONE Sending Event")
            sendUIEvent(Event.DrawingCopiedToDevice(deviceFileUri))
        } catch(e: Exception) {
            Log.d(TAG, "onDrawingCopyToDeviceBtnClick: $e")
            sendUIEvent(Event.ErrorOccurred(e))
        }
    }

    fun updateDrawing(drawing: Drawing) = viewModelScope.launch {
        drawing.apply {
            isSynced = false
        }
        repository.insertDrawing(drawing)
    }

    fun trashDrawing(drawing: Drawing) = viewModelScope.launch {
        drawing.apply {
            isDeleted = true
            isSynced = false
        }

        repository.insertDrawing(drawing)
        _eventStateFlow.emit(Event.TrashDrawingSuccess)
    }

    fun loginUserWithGoogle() = viewModelScope.launch {
        Timber.tag(TAG).d("loginUserWithGoogle: Called")
        try {
            sendUIEvent(Event.Loading)
            if(getNetworkStatus(app) == 0) {
                throw NoInternetException()
            }

            val googleIdTokenRequestOptions = GetGoogleIdOption.Builder()
                .setServerClientId(app.applicationContext.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .build()

            val signInRequest = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdTokenRequestOptions)
                .build()

            sendUIEvent(Event.BeginOneTapSignInProcess(signInRequest))

        } catch(e: Exception) {
            Timber.tag(TAG).d("loginUserWithGoogle: ${e.message}")
            if(e is ApiException || e is NoInternetException) {
                sendUIEvent(Event.BeginOneTapSignInFailure(e))
            }
        }
    }

    fun onOneTapSignInResultRetrieved(credential: Credential) = viewModelScope.launch {
        Timber.d("onOneTapSignInResultRetrieved: Called")
        sendUIEvent(Event.Loading)

        if (credential is CustomCredential
            && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            signInUserWithCredentials(googleIdTokenCredential.idToken)
        } else {
            Timber.tag(TAG).w("Credential is not of type Google ID!")
            _eventStateFlow.emit(Event.ErrorOccurred(Exception("Some error occurred")))
        }
    }

    private suspend fun signInUserWithCredentials(idToken: String) {
        try {
            repository.firebaseSignInWithCredentials(idToken)
            loggedInUser = repository.getAuthenticatedUser()

            sendUIEvent(Event.SignInSuccess)
        } catch(e: Exception) {
            Timber.tag(TAG).d("signInUserWithCredentials: ${e.message}")
            sendUIEvent(Event.ErrorOccurred(e))
        }
    }

    fun showSignupMessage(): Boolean {
        val timeLapsedSinceSignupMessageShown = (System.currentTimeMillis()
                - _appSettingsStateFlow.value.signupPopupLastShownTimestamp)

        Log.d(TAG, "showSignupMessage: timeLapsedSinceSignupMessageShown: $timeLapsedSinceSignupMessageShown")
        Log.d(TAG, "App Settings: ${_appSettingsStateFlow.value}")

        return (timeLapsedSinceSignupMessageShown > TimeUnit.HOURS.toMillis(2)
                && loggedInUser == null)
    }

    fun updateSignupPopupLastShownTime() {
        _appSettingsStateFlow.update {
            val updatedAppSettings = it.copy(signupPopupLastShownTimestamp = System.currentTimeMillis())
            repository.saveAppSettings(updatedAppSettings)
            updatedAppSettings
        }
    }

    fun saveCurrentSyncProcessId(processId: String) {
        repository.saveCurrentSyncProcessId(processId)
    }

    fun getCurrentSyncProcessId(): String {
        return repository.getCurrentSyncProcessId()
    }

    fun getUpdatedAppSettings(): AppSettings {
        _appSettingsStateFlow.update {
            repository.getAppSettings()
        }

        return repository.getAppSettings()
    }

    fun getAppPermissionSettingsIntent(): Intent {
        return permissionManager.getAppPermissionSettingsIntent()
    }

    fun onPermissionResult() {
        val writePermissionGranted = permissionManager
            .isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        permissionManager.updatePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            writePermissionGranted)

        if(writePermissionGranted) {
            drawingPendingForCopyingToDevice?.let {
                onDrawingCopyToDeviceBtnClick(it)
            }
        }
    }

    private fun sendUIEvent(event: Event) = viewModelScope.launch {
        _eventStateFlow.emit(event)
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventStateFlow.emit(Event.Idle)
    }

    private fun writeExternalStoragePermissionGranted(): Boolean {
        if(!permissionManager.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val appPermission = permissionManager
                .getAppPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            sendUIEvent(Event.WriteExternalStoragePermissionNotGranted(appPermission))
            return false
        }

        return true
    }

    sealed class Event {
        class UnsavedDrawingStatus(
            var unsavedDrawingFound: Boolean,
            var drawing: Drawing? = null
        ) : Event()
        class LoadDrawing(val currentDrawing: Drawing) : Event()
        data object TrashDrawingSuccess: Event()

        data object SignInSuccess: Event()
        data class BeginOneTapSignInProcess(val signInRequest: GetCredentialRequest): Event()
        class BeginOneTapSignInFailure(val exception: Exception): Event()

        class WriteExternalStoragePermissionNotGranted(val appPermission: AppPermission) : Event()
        class DrawingCopiedToDevice(val deviceFileUri: Uri): Event()

        class ErrorOccurred(val exception: Throwable): Event()
        data object Loading: Event()
        data object Idle : Event()
    }
}