package com.appsbyayush.paintspace.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun getNetworkStatus(context: Context): Int {
    var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: VPN
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val capabilities = connectivityManager
        .getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            result = 1
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            result = 2
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            result = 3
        }
    }

    return result
}