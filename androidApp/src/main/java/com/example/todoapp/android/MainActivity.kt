package com.example.todoapp.android

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.example.todoapp.app.App
import com.example.todoapp.db.sample.SampleSeed
import com.example.todoapp.di.KoinF
import com.example.todoapp.reminders.AndroidReminderScheduler
import com.example.todoapp.reminders.ReminderLinks
import com.example.todoapp.android.widget.AppWidgets
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is light-only, so the bars keep dark icons on its yellow and white screens.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        KoinF.setupKoin {
            androidContext(applicationContext)
        }
        // Dev-only sample data, from scripts/seed-sample.sh. A release build ignores it.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val sample = if (debuggable) SampleSeed.from(intent.getStringExtra("sample")) else null
        // Reminders need this from Android 13; a no only means no notifications. Asked after the first
        // mission or from Reminders (ReminderSync), not on first open.
        AndroidReminderScheduler.prompt = {
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ReminderLinks.open(intent.getStringExtra(ReminderLinks.EXTRA_DATE))
        ReminderLinks.openTab(intent.getStringExtra(ReminderLinks.EXTRA_TAB))
        AppWidgets.install(applicationContext)
        setContent {
            App(sample, devTools = debuggable)
        }
    }

    override fun onDestroy() {
        AndroidReminderScheduler.prompt = null
        super.onDestroy()
    }

    /** A tapped reminder or widget while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ReminderLinks.open(intent.getStringExtra(ReminderLinks.EXTRA_DATE))
        ReminderLinks.openTab(intent.getStringExtra(ReminderLinks.EXTRA_TAB))
    }
}
