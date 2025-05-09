package com.appsbyayush.paintspace.models

import java.util.*

data class AppSettings(
    var signupPopupLastShownTimestamp: Long = 0,
    var lastSyncTime: Date? = null
)