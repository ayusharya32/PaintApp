package com.appsbyayush.paintspace.utils

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import timber.log.Timber

fun launchOneTapSignInUI(
    credentialManager: CredentialManager,
    signInRequest: GetCredentialRequest,
    context: Context,
    viewLifecycleOwner: LifecycleOwner,
    onResultRetrieved: (Credential) -> Unit,
    onErrorOccurred: () -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        try {
            Timber.d("launchOneTapSignInUI: Called")
            context.let {
                val result = credentialManager.getCredential(
                    context = it,
                    request = signInRequest
                )

                onResultRetrieved(result.credential)
            }
        } catch(e: GetCredentialCancellationException) {
            onErrorOccurred()
        }
    }
}
