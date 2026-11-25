package com.example.jarvis

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.actions.PhoneActionManager
import com.example.data.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JarvisActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_TORCH = "com.madusanka.assistantvoice.ACTION_TOGGLE_TORCH"
        const val ACTION_STOP_STANDBY = "com.madusanka.assistantvoice.ACTION_STOP_STANDBY"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_TOGGLE_TORCH -> {
                val manager = PhoneActionManager(context.applicationContext)
                val result = manager.toggleFlashlight()
                manager.cleanup()
                Toast.makeText(context, result.messageEn, Toast.LENGTH_SHORT).show()
            }

            ACTION_STOP_STANDBY -> {
                JarvisSystemHelper.stopJarvisStandbyService(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppSettingsRepository(context.applicationContext)
                            .setJarvisStandbyEnabled(false)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
