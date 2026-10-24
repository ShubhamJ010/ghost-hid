package com.sj010.ghid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class HidCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val active = HidService.instance
        if (active != null) {
            active.handleIntent(intent)
            return
        }

        val serviceIntent = Intent(context, HidService::class.java).apply {
            action = intent.action
            putExtras(intent)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            try {
                context.startService(serviceIntent)
            } catch (e2: Exception) {
                Log.e("HidCommandReceiver", "Failed to dispatch intent to HidService", e2)
            }
        }
    }
}
