package com.example.todoapp.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.todoapp.domain.Reminder
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * One alarm per reminder; ReminderReceiver shows it. The list is kept in prefs too, so the previous alarms
 * can be cancelled and BootReceiver can set them again after a restart. Exact when the system allows it,
 * otherwise a few minutes late at worst.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    // The prompt needs an activity: MainActivity sets [prompt] while it's around.
    override fun askPermission() {
        prompt?.invoke()
    }

    override fun replaceAll(reminders: List<Reminder>) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        stored(context).forEach { alarms.cancel(pending(context, it)) }
        reminders.forEach { schedule(context, alarms, Stored(it.id, millis(it.at), it.title, it.body, it.openDate.toString())) }
        val json = buildJsonArray {
            reminders.forEach { r ->
                add(buildJsonObject {
                    put("id", r.id); put("at", millis(r.at)); put("title", r.title); put("body", r.body); put("date", r.openDate.toString())
                })
            }
        }
        prefs(context).edit().putString(KEY, json.toString()).apply()
    }

    /** What an alarm carries. */
    data class Stored(val id: String, val at: Long, val title: String, val body: String, val date: String)

    companion object {
        /** Shows Android's notification prompt; null without an activity. */
        var prompt: (() -> Unit)? = null

        private const val PREFS = "reminders"
        private const val KEY = "scheduled"
        const val ACTION = "com.example.todoapp.REMINDER"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        private fun millis(at: LocalDateTime) = at.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        fun stored(context: Context): List<Stored> = runCatching {
            (Json.parseToJsonElement(prefs(context).getString(KEY, null) ?: return emptyList()) as JsonArray).map {
                val o = it.jsonObject
                Stored(
                    o["id"]!!.jsonPrimitive.content, o["at"]!!.jsonPrimitive.content.toLong(),
                    o["title"]!!.jsonPrimitive.content, o["body"]!!.jsonPrimitive.content, o["date"]!!.jsonPrimitive.content,
                )
            }
        }.getOrDefault(emptyList())

        /** After a restart: the alarms still ahead come back. */
        fun restore(context: Context) {
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            val now = Clock.System.now().toEpochMilliseconds()
            stored(context).filter { it.at > now }.forEach { schedule(context, alarms, it) }
        }

        private fun schedule(context: Context, alarms: AlarmManager, r: Stored) {
            val pi = pending(context, r)
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, pi)
            }
        }

        /** The data URI keeps each reminder's alarm apart, so cancelling one leaves the rest. */
        private fun pending(context: Context, r: Stored): PendingIntent {
            val intent = Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION)
                .setData(Uri.parse("daywise://reminder/${r.id}"))
                .putExtra(EXTRA_ID, r.id)
                .putExtra(EXTRA_TITLE, r.title)
                .putExtra(EXTRA_BODY, r.body)
                .putExtra(ReminderLinks.EXTRA_DATE, r.date)
            return PendingIntent.getBroadcast(
                context, r.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
