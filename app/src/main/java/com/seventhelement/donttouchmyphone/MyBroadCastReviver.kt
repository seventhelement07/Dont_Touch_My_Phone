package com.seventhelement.donttouchmyphone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

class MyBroadCastReviver:BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(p0: Context?, p1: Intent?) {
        if(p1?.action.equals(Intent.ACTION_BOOT_COMPLETED))
        {
            val serviceIntent=Intent(p0,FourgroundService::class.java)
            p0?.startForegroundService(serviceIntent)
        }
    }
}