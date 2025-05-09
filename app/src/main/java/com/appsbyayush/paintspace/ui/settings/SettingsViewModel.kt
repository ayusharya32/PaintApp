package com.appsbyayush.paintspace.ui.settings

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.models.AppSettings
import com.appsbyayush.paintspace.repo.PaintRepository
import com.appsbyayush.paintspace.utils.NoInternetException
import com.appsbyayush.paintspace.utils.getNetworkStatus
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PaintRepository,
    private val app: Application
): ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModelyy"
    }

    private val _eventStateFlow = MutableStateFlow<Event>(Event.Idle)
    val events = _eventStateFlow.asStateFlow()

    var loggedInUser = repository.getAuthenticatedUser()
//    var signInClient: GoogleSignInClient? = null

    private val _appSettingsStateFlow = MutableStateFlow(repository.getAppSettings())
    val appSettings = _appSettingsStateFlow.asStateFlow()

    fun loginUserWithGoogle() = viewModelScope.launch {
        Timber.tag(TAG).d("loginUserWithGoogle: Called")
        try {
            sendEvent(Event.Loading)
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

            sendEvent(Event.BeginOneTapSignInProcess(signInRequest))

        } catch(e: Exception) {
            Timber.tag(TAG).d("loginUserWithGoogle: ${e.message}")
            if(e is ApiException || e is NoInternetException) {
                sendEvent(Event.BeginOneTapSignInFailure(e))
            }
        }
    }

    fun onOneTapSignInResultRetrieved(credential: Credential) = viewModelScope.launch {
        Timber.d("onOneTapSignInResultRetrieved: Called")
        sendEvent(Event.Loading)

        if (credential is CustomCredential
            && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            signInUserWithCredentials(googleIdTokenCredential.idToken)
        } else {
            Timber.tag(TAG).w("Credential is not of type Google ID!")
            sendEvent(Event.ErrorOccurred(Exception("Some error occurred while logging in")))
        }
    }

    private suspend fun signInUserWithCredentials(idToken: String) {
        try {
            repository.firebaseSignInWithCredentials(idToken)
            loggedInUser = repository.getAuthenticatedUser()

            sendEvent(Event.SignInSuccess)
        } catch(e: Exception) {
            Timber.tag(TAG).d("signInUserWithCredentials: ${e.message}")
            sendEvent(Event.ErrorOccurred(e))
        }
    }

    fun saveCurrentSyncProcessId(processId: String) {
        repository.saveCurrentSyncProcessId(processId)
    }

    fun getCurrentSyncProcessId(): String {
        return repository.getCurrentSyncProcessId()
    }

    fun signOut() = viewModelScope.launch {
        _eventStateFlow.emit(Event.LogoutLoading)

        try {
            repository.syncDrawings()
            repository.saveAppSettings(AppSettings())
            repository.resetAllSyncTimestamps()

            repository.clearDrawingsTable()
            repository.deleteAllLocalMediaFiles()

            WorkManager.getInstance(app.applicationContext).cancelAllWork()
            repository.logoutUser()

            Log.d(TAG, "signOut: Logout Success")
            _eventStateFlow.emit(Event.LogoutSuccess)

        } catch(e: Exception) {
            Log.d(TAG, "signOut: Logout Error ${e.message}")

            _eventStateFlow.emit(Event.LogoutError(e))
        }
    }

    fun getUpdatedAppSettings(): AppSettings {
        _appSettingsStateFlow.update {
            repository.getAppSettings()
        }

        return repository.getAppSettings()
    }

    private fun sendEvent(event: Event) = viewModelScope.launch {
        _eventStateFlow.emit(event)
    }

    fun onEventOccurred() = viewModelScope.launch {
        _eventStateFlow.emit(Event.Idle)
    }

    sealed class Event {
        data object SignInSuccess: Event()
        data class BeginOneTapSignInProcess(val signInRequest: GetCredentialRequest): Event()
        data class BeginOneTapSignInFailure(val exception: Exception): Event()
        data object LogoutLoading: Event()
        data object LogoutSuccess: Event()
        class LogoutError(val exception: Throwable): Event()
        class ErrorOccurred(val exception: Throwable): Event()
        data object Loading: Event()
        data object Idle : Event()
    }
}