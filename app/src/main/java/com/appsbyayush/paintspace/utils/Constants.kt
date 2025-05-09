package com.appsbyayush.paintspace.utils

import com.appsbyayush.paintspace.R
import com.appsbyayush.paintspace.models.GradientColor
import com.appsbyayush.paintspace.models.GradientTypeItem
import com.appsbyayush.paintspace.models.GraphicElementTypeItem
import com.appsbyayush.paintspace.models.OnboardingItem
import com.appsbyayush.paintspace.utils.enums.GradientType
import com.appsbyayush.paintspace.utils.enums.GraphicElementType

object Constants {
    const val BASE_URL = "https://asia-south1-paintspace-82e41.cloudfunctions.net/app/"

    const val COLLECTION_DRAWINGS = "drawings"
    const val COLLECTION_FONTS = "fonts"
    const val COLLECTION_GRADIENTS = "gradients"

    const val COLLECTION_USERS = "users"
    const val COLLECTION_USER_GRAPHIC_ELEMENTS = "user_graphic_elements"
    const val COLLECTION_USER_GRADIENTS = "user_gradients"

    const val FIELD_USER_ID = "userId"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_MODIFIED_AT = "modifiedAt"

    const val DATE_FORMAT_1 = "dd/MMM"
    const val DATE_FORMAT_2 = "dd MMM hh:mm a"
    const val DATE_FORMAT_3 = "dd MMM YYYY"

    const val TIME_FORMAT_1 = "hh:mm a"

    const val DATE_TIME_FORMAT_1 = "yyyyMMddHHmmssSSS"

    const val APP_SHARED_PREFS = "APP_SHARED_PREFS"

    const val PREFS_KEY_USER_GRAPHIC_ELEMENTS_SYNC_TIMESTAMP = "PREFS_KEY_USER_GRAPHIC_ELEMENTS_SYNC_TIMESTAMP"
    const val PREFS_KEY_USER_GRADIENTS_SYNC_TIMESTAMP = "PREFS_KEY_USER_GRADIENTS_SYNC_TIMESTAMP"
    const val PREFS_KEY_FONTS_SYNC_TIMESTAMP = "PREFS_KEY_FONTS_SYNC_TIMESTAMP"
    const val PREFS_KEY_APP_SETTINGS = "PREFS_KEY_APP_SETTINGS"
    const val PREFS_KEY_CURRENT_SYNC_PROCESS_ID = "PREFS_KEY_CURRENT_SYNC_PROCESS_ID"
    const val PREFS_KEY_ONBOARDING_DONE = "PREFS_KEY_ONBOARDING_DONE"
    const val PREFS_KEY_APP_PERMISSIONS = "PREFS_KEY_APP_PERMISSIONS"

    const val IMAGE_TYPE_JPEG = "image/jpeg"
    const val IMAGE_TYPE_JPG = "image/jpg"
    const val IMAGE_TYPE_PNG = "image/png"
    const val IMAGE_TYPE_WEBP = "image/webp"

    const val FILE_EXT_JPG = "jpg"
    const val FILE_EXT_PNG = "png"

    const val DIR_ELEMENTS = "elements"
    const val DIR_DRAWINGS = "drawings"

    const val DEFAULT_GRADIENT_RADIUS = 400F

    const val NOTIFICATION_CHANNEL_LOW = "NOTIFICATION_CHANNEL_LOW"
    const val NOTIFICATION_CHANNEL_HIGH = "NOTIFICATION_CHANNEL_HIGH"

    const val WORK_RESULT = "WORK_RESULT"
    const val WORK_RESULT_SUCCESS = "WORK_RESULT_SUCCESS"
    const val WORK_RESULT_FAILURE = "WORK_RESULT_FAILURE"

    val GRADIENT_TYPE_LIST = listOf(
        GradientTypeItem("Linear Gradient", GradientType.LINEAR),
        GradientTypeItem("Radial Gradient", GradientType.RADIAL),
        GradientTypeItem("Sweep Gradient", GradientType.SWEEP)
    )

    val GRAPHIC_ELEMENT_TYPE_LIST = listOf(
        GraphicElementTypeItem("Icons & Illustrations", GraphicElementType.ELEMENT_SIMPLE),
        GraphicElementTypeItem("Backgrounds", GraphicElementType.ELEMENT_BACKGROUND),
        GraphicElementTypeItem("Frames", GraphicElementType.ELEMENT_MASK_FRAME),
        GraphicElementTypeItem("Images with Frame", GraphicElementType.ELEMENT_SIMPLE_WITH_MASK)
    )

    val DEFAULT_GRADIENT_COLORS_LIST = listOf(
        GradientColor(1, "#004AAD", true),
        GradientColor(2, "#112B3C", true),
        GradientColor(3, "#39a30f", false),
        GradientColor(4, "#DC3535", false)
    )

    val ONBOARDING_ITEMS_LIST = listOf(
        OnboardingItem("Put your imagination onto a canvas", R.drawable.ic_onboarding_1),
        OnboardingItem("Create anything using graphics and gradients", R.drawable.ic_onboarding_2),
        OnboardingItem("Add any type of image/graphic to your drawing", R.drawable.ic_onboarding_3),
        OnboardingItem("Create eye catching gradients for your drawing", R.drawable.ic_onboarding_4),
        OnboardingItem("Choose from multiple fonts to add text", R.drawable.ic_onboarding_5)
    )
}