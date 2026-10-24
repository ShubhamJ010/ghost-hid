package com.sj010.ghid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class HidCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val serviceIntent = Intent(context, HidService::class.java).apply {
            action = intent.action
            putExtras(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
