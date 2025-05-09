package com.appsbyayush.paintspace.utils

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.appsbyayush.paintspace.repo.PaintRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    private val repository: PaintRepository,
    private val app: Application
) {
    companion object {
        private const val TAG = "PermissionManagyy"
    }

    private val userPermissionsNeededByApp = listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    init {
        updateDeniedPermissionsList()
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(app.applicationContext, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun getAppPermission(permissionName: String): AppPermission {
        return repository.getAppPermissions().find { it.name == permissionName }
            ?: AppPermission(permissionName)
    }

    fun updatePermission(permissionName: String, permissionGranted: Boolean) {
        val appPermissions = repository.getAppPermissions().toMutableList()

        var permission = appPermissions.find { it.name == permissionName }

        if(permission == null) {
            permission = AppPermission(name = permissionName)
            appPermissions.add(permission)
        }

        permission.apply {
            isGranted = permissionGranted

            if(permissionGranted) {
                timesDenied = 0
            } else {
                timesDenied += 1
            }
        }

        repository.updateAppPermissions(appPermissions)
    }

    fun getAppPermissionSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", app.applicationContext.packageName, null)
        }
    }

    private fun updateDeniedPermissionsList() {
        var appPermissions = repository.getAppPermissions()

        if(appPermissions.isEmpty()) {
            appPermissions = userPermissionsNeededByApp.map { permissionString ->
                AppPermission(name = permissionString)
            }
        }

        appPermissions.onEach { permission ->
            val permissionGranted = ContextCompat.checkSelfPermission(app.applicationContext,
                permission.name) == PackageManager.PERMISSION_GRANTED
            permission.isGranted = permissionGranted

            if(permissionGranted) {
                permission.timesDenied = 0
            }
        }

        repository.updateAppPermissions(appPermissions)
    }
}

data class AppPermission(
    val name: String,
    var isGranted: Boolean = false,
    var timesDenied: Int = 0
)