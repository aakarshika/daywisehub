package com.example.todoapp.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms don't survive a restart; this sets the stored ones again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AndroidReminderScheduler.restore(context)
    }
}
