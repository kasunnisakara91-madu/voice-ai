package com.example.jarvis

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

            val launchIntent = Intent(context, JarvisOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(JarvisOverlayActivity.EXTRA_AUTO_LISTEN, true)
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                3001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mic_btn, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_prompt_area, launchPendingIntent)

            val torchIntent = Intent(context, JarvisActionReceiver::class.java).apply {
                action = JarvisActionReceiver.ACTION_TOGGLE_TORCH
            }
            val torchPendingIntent = PendingIntent.getBroadcast(
                context,
                3002,
                torchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_torch_btn, torchPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
