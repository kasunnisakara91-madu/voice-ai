package com.example.jarvis

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Foreground service keeping a persistent Jarvis quick-trigger notification
 * accessible from the notification shade and lock screen over any app.
 */
class JarvisStandbyService : Service() {

    companion object {
        private const val CHANNEL_ID = "jarvis_standby_channel"
        private const val NOTIFICATION_ID = 4102
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Jarvis Standby Mode",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Quick access notification to launch Jarvis voice commands over any app"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val speakIntent = Intent(this, JarvisOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(JarvisOverlayActivity.EXTRA_AUTO_LISTEN, true)
        }
        val speakPendingIntent = PendingIntent.getActivity(
            this,
            2001,
            speakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val torchIntent = Intent(this, JarvisActionReceiver::class.java).apply {
            action = JarvisActionReceiver.ACTION_TOGGLE_TORCH
        }
        val torchPendingIntent = PendingIntent.getBroadcast(
            this,
            2002,
            torchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisActionReceiver::class.java).apply {
            action = JarvisActionReceiver.ACTION_STOP_STANDBY
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            2003,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            2004,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_jarvis_mic)
            .setContentTitle(getString(R.string.jarvis_notification_title))
            .setContentText(getString(R.string.jarvis_notification_body))
            .setContentIntent(speakPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_jarvis_mic, "Speak", speakPendingIntent)
            .addAction(R.drawable.ic_jarvis_torch, "Flashlight", torchPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setDeleteIntent(openAppPendingIntent)
            .build()
    }
}
