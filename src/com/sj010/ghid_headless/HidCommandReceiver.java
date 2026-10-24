package com.sj010.ghid_headless;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class HidCommandReceiver extends BroadcastReceiver {

    private static final String TAG = "GHOST_HID";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "HidCommandReceiver onReceive: " + action);

        Intent serviceIntent = new Intent(context, HidService.class);
        serviceIntent.setAction(action);
        serviceIntent.putExtras(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
