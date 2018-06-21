package com.aoitek.logtool

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Messenger
import android.os.SystemClock
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) || intent.action.equals(ALARM_SETUP_ACTION)) {
            context.startService(Intent(context, WorkerService::class.java))
        }
    }
}