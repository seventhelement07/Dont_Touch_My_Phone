package com.seventhelement.donttouchmyphone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MyBroadCastReviver : BroadcastReceiver() {
    private val mySharedPreference = MySharedPreference()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        // Only resume monitoring if the user actually had it armed before the reboot —
        // otherwise every restart would silently turn the alarm back on.
        if (!mySharedPreference.isArmed(context)) return

        // Android 15+ blocks starting a mediaPlayback foreground service directly from a
        // BOOT_COMPLETED receiver (ForegroundServiceStartNotAllowedException). Instead, post
        // a plain notification — tapping it is a user-initiated action, which is exempt, and
        // MainActivity resumes the service itself once genuinely in the foreground.
        val channelId = "Boot Resume"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Resume Protection", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Don't Touch My Phone")
            .setContentText("Protection paused after restart. Tap to resume.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(1002, notification)
    }
}