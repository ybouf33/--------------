package com.example.data.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object LiveNotificationManager {
  const val CHANNEL_ID = "syndic_live_alerts"

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        val channel =
          NotificationChannel(
            CHANNEL_ID,
            "تنبيهات السانديك المباشرة / Notifications Syndic",
            NotificationManager.IMPORTANCE_HIGH,
          ).apply {
            description = "تنبيهات فورية للإعلانات والأشغال وواجبات السانديك وطلبات السكان"
            enableVibration(true)
          }
        manager.createNotificationChannel(channel)
      }
    }
  }

  fun postLiveNotification(
    context: Context,
    notificationKey: String,
    title: String,
    body: String,
    buildingName: String,
  ) {
    if (!hasNotificationPermission(context)) return
    ensureChannel(context)

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        notificationKey.hashCode(),
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val displayTitle = if (buildingName.isNotBlank()) "$buildingName • $title" else title
    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(displayTitle)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationKey.hashCode(), notification)
    } catch (_: SecurityException) {
      // Permission was revoked; fall back silently to in-app notification feed
    }
  }
}
