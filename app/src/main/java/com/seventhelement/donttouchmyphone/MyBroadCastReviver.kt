package com.seventhelement.donttouchmyphone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyBroadCastReviver : BroadcastReceiver() {
    private val mySharedPreference = MySharedPreference()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        // Only resume monitoring if the user actually had it armed before the reboot —
        // otherwise every restart would silently turn the alarm back on.
        if (!mySharedPreference.isArmed(context)) return

        val position = mySharedPreference.getInt(context).let { if (it == -1) 0 else it }
        val soundTitle = Constants.getlist().getOrNull(position)?.title ?: ""
        val soundResId = context.resources.getIdentifier(soundTitle, "raw", context.packageName)

        val serviceIntent = Intent(context, FourgroundService::class.java)
            .putExtra("message_key2", soundTitle)
            .putExtra("sound_res_id", soundResId)
        context.startForegroundService(serviceIntent)
    }
}