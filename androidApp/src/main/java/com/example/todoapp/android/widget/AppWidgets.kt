package com.example.todoapp.android.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.todoapp.android.MainActivity
import com.example.todoapp.android.R
import com.example.todoapp.di.KoinF
import com.example.todoapp.reminders.ReminderLinks
import com.example.todoapp.widget.NextUp
import com.example.todoapp.widget.WidgetData
import com.example.todoapp.widget.WidgetHooks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import java.util.Calendar

/** What every widget shares: the data, the refresh, the clock that switches them, and the taps. */
object AppWidgets {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The widgets run in the app's process, with or without the app open; Koin starts once either way. */
    fun data(context: Context): WidgetData {
        KoinF.setupKoin { androidContext(context.applicationContext) }
        return KoinF.di!!.get()
    }

    /** Called when the app starts: its own changes reload the widgets. */
    fun install(context: Context) {
        val app = context.applicationContext
        WidgetHooks.refresh = { scope.launch { refreshAll(app) } }
    }

    suspend fun refreshAll(context: Context) {
        all.forEach { runCatching { it.updateAll(context) } }
        scheduleTick(context)
    }

    private val all: List<GlanceAppWidget> get() = listOf(
        TodayWidget(), WaterMoodWidget(), NextUpWidget(), StickersWidget(), WeekAheadWidget(), DiaryWidget(), MonthWidget(),
    )

    /** Pixels per dp for the art, capped so a large widget's bitmaps stay inside RemoteViews' memory limit. */
    fun scale(context: Context): Float = context.resources.displayMetrics.density.coerceAtMost(3f)

    /**
     * The Today widget changes at 8 pm, and every widget at midnight, so an alarm reloads them at the next
     * of those two. It's set again on every reload.
     */
    fun scheduleTick(context: Context) {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 5); set(Calendar.MILLISECOND, 0)
            if (now.get(Calendar.HOUR_OF_DAY) < WidgetData.EVENING_HOUR) {
                set(Calendar.HOUR_OF_DAY, WidgetData.EVENING_HOUR)
            } else {
                set(Calendar.HOUR_OF_DAY, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val pending = PendingIntent.getBroadcast(
            context, 0, Intent(context, WidgetTickReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, next.timeInMillis, pending)
    }

    /** Opens the app on [dateKey]'s diary page, or on today's. */
    fun openDay(context: Context, dateKey: String?): Action = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ReminderLinks.EXTRA_DATE, dateKey)
            // Each day its own intent, so two rows don't share one PendingIntent.
            .setData(android.net.Uri.parse("inbalance://day/${dateKey ?: "today"}"))
    )

    fun openMetrics(context: Context): Action = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ReminderLinks.EXTRA_TAB, ReminderLinks.TAB_METRICS)
            .setData(android.net.Uri.parse("inbalance://metrics"))
    )

    fun toggleTask(id: Long): Action = actionRunCallback<ToggleTaskAction>(actionParametersOf(TaskKey to id))
    fun addGlass(): Action = actionRunCallback<AddGlassAction>()
    fun toggleMood(icon: String): Action = actionRunCallback<ToggleMoodAction>(actionParametersOf(MoodKey to icon))
    fun stepNextUp(by: Int, dateKey: String, count: Int): Action =
        actionRunCallback<StepNextUpAction>(actionParametersOf(StepKey to by, DayKey to dateKey, CountKey to count))

    val TaskKey = ActionParameters.Key<Long>("task")
    val MoodKey = ActionParameters.Key<String>("mood")
    val StepKey = ActionParameters.Key<Int>("step")
    val DayKey = ActionParameters.Key<String>("day")
    val CountKey = ActionParameters.Key<Int>("count")

    /** Next up's saved place, per widget: NextUp's "yyyy-MM-dd#index". */
    val NextUpPlace = stringPreferencesKey("next_up")
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[AppWidgets.TaskKey] ?: return
        AppWidgets.data(context).toggleTask(id)
        AppWidgets.refreshAll(context)
    }
}

class AddGlassAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        AppWidgets.data(context).addGlass()
        AppWidgets.refreshAll(context)
    }
}

class ToggleMoodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val icon = parameters[AppWidgets.MoodKey] ?: return
        AppWidgets.data(context).toggleMood(icon)
        AppWidgets.refreshAll(context)
    }
}

class StepNextUpAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val by = parameters[AppWidgets.StepKey] ?: return
        val day = parameters[AppWidgets.DayKey] ?: return
        val count = parameters[AppWidgets.CountKey] ?: return
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[AppWidgets.NextUpPlace] = NextUp.step(prefs[AppWidgets.NextUpPlace], day, count, by)
        }
        NextUpWidget().update(context, glanceId)
    }
}

/** The 8 pm and midnight alarm. */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AppWidgets.refreshAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

// The look shared by every widget (docs/widgets/mockups.html).
internal val Paper = Color(0xFFFFF8E6)
internal val Ink = Color(0xFF2A2A2A)
internal val Gray = Color(0xFF6E6E6E)
internal val Faint = Color(0xFFB4B2A9)
internal val Rule = Color(0xFFEDE6D6)
internal val Blue = Color(0xFF2F5677)
internal val Sticky = Color(0xFFFDF0C6)
internal val DoneGreen = Color(0xFFA6CC76)
internal val BarYellow = Color(0xFFFAE0A0)
internal val RestBar = Color(0xFFD3D1C7)

/** The yellow card every widget sits on, rounded on every Android version. */
internal fun GlanceModifier.card(rest: Boolean = false): GlanceModifier =
    background(ImageProvider(if (rest) R.drawable.widget_bg_rest else R.drawable.widget_bg)).padding(12.dp)

@Composable
internal fun Words(
    text: String,
    size: TextUnit = 12.sp,
    color: Color = Ink,
    bold: Boolean = false,
    lines: Int = 1,
    struck: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text, modifier = modifier, maxLines = lines,
        style = TextStyle(
            color = ColorProvider(color), fontSize = size,
            fontWeight = if (bold) FontWeight.Medium else FontWeight.Normal,
            textDecoration = if (struck) TextDecoration.LineThrough else null,
        ),
    )
}

/** The small grey caps over each widget: "THIS WEEK". */
@Composable
internal fun Kicker(text: String, modifier: GlanceModifier = GlanceModifier) = Words(text, 11.sp, Gray, modifier = modifier)

/** An outlined pill button; [filled] uses the diary's peach. */
@Composable
internal fun Pill(text: String, action: Action, filled: Boolean = false, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier
            .background(ImageProvider(if (filled) R.drawable.widget_pill_filled else R.drawable.widget_pill))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Words(text, bold = true)
    }
}

