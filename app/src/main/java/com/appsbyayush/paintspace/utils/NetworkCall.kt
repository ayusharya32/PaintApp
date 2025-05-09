package com.appsbyayush.paintspace.utils

sealed class NetworkCall<T>(
    val data: T? = null,
    val error: Throwable? = null
) {
    class Success<T>(data: T) : NetworkCall<T>(data)
    class Loading<T>(data: T? = null) : NetworkCall<T>(data)
    class Error<T>(error: Throwable, data: T? = null) : NetworkCall<T>(data, error)
    class Idle<T> : NetworkCall<T>()
}