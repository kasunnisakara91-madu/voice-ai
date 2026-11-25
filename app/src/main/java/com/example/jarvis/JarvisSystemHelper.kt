package com.example.jarvis

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.R
import java.util.concurrent.Executors

object JarvisSystemHelper {

    private const val TAG = "JarvisSystemHelper"

    fun isDefaultAssistant(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check assistant role", e)
            false
        }
    }

    fun openAssistantSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed opening assistant settings", e)
            false
        }
    }

    fun canRequestAddTile(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @SuppressLint("WrongConstant")
    fun requestAddQuickSettingsTile(
        context: Context,
        onResult: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult("Swipe down Quick Settings -> Edit -> drag 'Jarvis Voice' to your active tiles.")
            return
        }
        try {
            val statusBarManager = context.getSystemService(android.app.StatusBarManager::class.java)
            if (statusBarManager == null) {
                onResult("Open Quick Settings -> Edit -> drag 'Jarvis Voice' into your tiles.")
                return
            }
            statusBarManager.requestAddTileService(
                ComponentName(context, JarvisTileService::class.java),
                context.getString(R.string.jarvis_tile_label),
                Icon.createWithResource(context, R.drawable.ic_jarvis_mic),
                Executors.newSingleThreadExecutor()
            ) { resultCode ->
                val msg = when (resultCode) {
                    android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        "Jarvis Voice tile added to Quick Settings!"
                    android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        "Jarvis Voice tile is already in Quick Settings."
                    else ->
                        "You can add 'Jarvis Voice' manually from Quick Settings -> Edit."
                }
                onResult(msg)
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestAddTileService failed", e)
            onResult("Open Quick Settings -> Edit -> drag 'Jarvis Voice' into your tiles.")
        }
    }

    fun requestPinHomeWidget(context: Context): Boolean {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (!appWidgetManager.isRequestPinAppWidgetSupported) {
                return false
            }
            val provider = ComponentName(context, JarvisWidgetProvider::class.java)
            appWidgetManager.requestPinAppWidget(provider, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "requestPinAppWidget failed", e)
            false
        }
    }

    fun hasMicrophonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun startJarvisStandbyService(context: Context): Boolean {
        if (!hasMicrophonePermission(context)) return false
        return try {
            val intent = Intent(context, JarvisStandbyService::class.java)
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting JarvisStandbyService", e)
            false
        }
    }

    fun stopJarvisStandbyService(context: Context) {
        try {
            context.stopService(Intent(context, JarvisStandbyService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "Failed stopping JarvisStandbyService", e)
        }
    }

    fun launchJarvisOverlay(context: Context, autoListen: Boolean = true) {
        val intent = Intent(context, JarvisOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(JarvisOverlayActivity.EXTRA_AUTO_LISTEN, autoListen)
        }
        context.startActivity(intent)
    }
}
