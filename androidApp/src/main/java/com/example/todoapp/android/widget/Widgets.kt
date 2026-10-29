package com.example.todoapp.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.example.todoapp.android.R
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.widget.DayType
import com.example.todoapp.widget.NextUp
import com.example.todoapp.widget.TodayMode
import com.example.todoapp.widget.WidgetArt
import com.example.todoapp.widget.WidgetMood
import com.example.todoapp.widget.WidgetSnapshot
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

/** A widget that draws today's snapshot, and follows the data while its session lasts. */
abstract class SnapshotWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppWidgets.scheduleTick(context)
        val flow = AppWidgets.data(context).observe().catch { }
        val first = runCatching { flow.first() }.getOrNull()
        val scale = AppWidgets.scale(context)
        provideContent {
            val snapshot by flow.collectAsState(first)
            val s = snapshot
            if (s == null) {
                Box(GlanceModifier.fillMaxSize().card().clickable(AppWidgets.openDay(context, null)), contentAlignment = Alignment.Center) { Kicker("INBALANCE") }
            } else {
                Content(context, s, Art(scale))
            }
        }
    }

    @Composable
    abstract fun Content(context: Context, s: WidgetSnapshot, art: Art)
}

/** WidgetArt at this screen's density, as Glance images. */
class Art(val scale: Float) {
    fun of(bitmap: ImageBitmap): ImageProvider = ImageProvider(bitmap.asAndroidBitmap())
}

@Composable
private fun ArtImage(provider: ImageProvider, width: Dp, height: Dp = width, modifier: GlanceModifier = GlanceModifier) =
    Image(provider, contentDescription = null, modifier = modifier.width(width).height(height))

@Composable
private fun MoodFace(context: Context, mood: WidgetMood, art: Art, size: Dp) {
    val picture = remember(mood) { art.of(WidgetArt.mood(mood, size.value, art.scale)) }
    ArtImage(picture, size, modifier = GlanceModifier.clickable(AppWidgets.toggleMood(mood.icon)))
}

// 1. Today

class TodayWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        val open = AppWidgets.openDay(context, null)
        when (s.mode) {
            TodayMode.BALANCE -> Column(
                GlanceModifier.fillMaxSize().card().clickable(open), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Kicker("THIS WEEK", GlanceModifier.fillMaxWidth())
                Spacer(GlanceModifier.defaultWeight())
                val seeSaw = remember(s.pillars, s.tilt) { art.of(WidgetArt.seeSaw(s.pillars, s.tilt, art.scale)) }
                ArtImage(seeSaw, 126.dp, 64.dp)
                Spacer(GlanceModifier.defaultWeight())
                Words(s.verdict, bold = true, color = s.verdictPillar?.let { pillarInk(it) } ?: Gray, modifier = GlanceModifier.fillMaxWidth())
            }
            TodayMode.ALL_DONE -> Column(
                GlanceModifier.fillMaxSize().card().clickable(open), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Kicker("TODAY", GlanceModifier.fillMaxWidth())
                Spacer(GlanceModifier.defaultWeight())
                ArtImage(remember { art.of(WidgetArt.cheer(art.scale)) }, 120.dp, 46.dp)
                Spacer(GlanceModifier.defaultWeight())
                Words("All done. ${s.verdict}", bold = true)
            }
            TodayMode.REST -> Column(
                GlanceModifier.fillMaxSize().card(rest = true).clickable(open), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Kicker("RECHARGING DAY", GlanceModifier.fillMaxWidth())
                Spacer(GlanceModifier.defaultWeight())
                ArtImage(remember { art.of(WidgetArt.rest(48f, art.scale)) }, 48.dp)
                Spacer(GlanceModifier.defaultWeight())
                Words("Nothing due. Rest counts", bold = true)
            }
            TodayMode.EVENING -> Column(GlanceModifier.fillMaxSize().card().clickable(open)) {
                Kicker(s.timeLabel)
                Spacer(GlanceModifier.defaultWeight())
                Words("How was today?", 14.sp, bold = true)
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    s.moods.take(3).forEachIndexed { i, mood ->
                        if (i > 0) Spacer(GlanceModifier.defaultWeight())
                        MoodFace(context, mood, art, 34.dp)
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Words("${s.doneCount} of ${s.totalCount} done", 11.sp, Gray)
            }
            TodayMode.EMPTY -> Column(GlanceModifier.fillMaxSize().card().clickable(open)) {
                Kicker("TODAY")
                Spacer(GlanceModifier.defaultWeight())
                Words("Nothing planned yet", 14.sp, bold = true, lines = 2)
                Spacer(GlanceModifier.height(8.dp))
                Pill("Plan my day", open)
                Spacer(GlanceModifier.defaultWeight())
                Words("Opens Inbalance", 11.sp, Gray)
            }
        }
    }
}

// 2. Water and mood

class WaterMoodWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        Column(GlanceModifier.fillMaxSize().card().clickable(AppWidgets.openDay(context, null))) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Kicker("WATER")
                Spacer(GlanceModifier.defaultWeight())
                Words("${s.water} / ${s.waterGoal}", bold = true)
            }
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth()) {
                (0 until s.waterGoal).forEach { i ->
                    val filled = i < s.water
                    val glass = remember(i, filled) { art.of(WidgetArt.glass(i, filled, 15f, art.scale)) }
                    ArtImage(glass, 15.dp)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Pill("+ glass", AppWidgets.addGlass())
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                s.moods.forEachIndexed { i, mood ->
                    if (i > 0) Spacer(GlanceModifier.defaultWeight())
                    MoodFace(context, mood, art, 22.dp)
                }
            }
        }
    }
}

// 3. Next up

class NextUpWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        val open = s.openTasks
        val place = currentState<Preferences>()[AppWidgets.NextUpPlace]
        val index = NextUp.index(place, s.dateKey, open.size)
        val task = open.getOrNull(index)
        Column(GlanceModifier.fillMaxSize().card().clickable(AppWidgets.openDay(context, s.dateKey))) {
            if (task == null) {
                Kicker("NEXT UP")
                Spacer(GlanceModifier.defaultWeight())
                Words(if (s.totalCount == 0) "Nothing planned yet" else "All clear for today", 14.sp, bold = true, lines = 2)
                Spacer(GlanceModifier.defaultWeight())
                Words("Opens Inbalance", 11.sp, Gray, modifier = GlanceModifier.clickable(AppWidgets.openDay(context, null)))
                return@Column
            }
            Kicker("NEXT UP · ${index + 1} OF ${open.size}")
            Spacer(GlanceModifier.defaultWeight())
            Row(
                GlanceModifier.fillMaxWidth().clickable(AppWidgets.openDay(context, s.dateKey)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pillar = s.pillars.firstOrNull { it.pillar == task.pillar }
                val friend = remember(task.pillar, pillar) {
                    art.of(WidgetArt.friend(task.pillar, pillar?.share ?: 0.5f, false, 0.6f, 24f, art.scale))
                }
                ArtImage(friend, 24.dp)
                Spacer(GlanceModifier.width(6.dp))
                Words(task.title, 14.sp, bold = true, lines = 2)
            }
            Spacer(GlanceModifier.defaultWeight())
            Pill("Done", AppWidgets.toggleTask(task.taskId), filled = true)
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Arrow("‹", AppWidgets.stepNextUp(-1, s.dateKey, open.size))
                Spacer(GlanceModifier.defaultWeight())
                Words(task.timeLabel, 11.sp, Gray)
                Spacer(GlanceModifier.defaultWeight())
                Arrow("›", AppWidgets.stepNextUp(1, s.dateKey, open.size))
            }
        }
    }

    @Composable
    private fun Arrow(text: String, action: androidx.glance.action.Action) {
        Box(
            GlanceModifier.size(28.dp).background(ImageProvider(R.drawable.widget_circle)).clickable(action),
            contentAlignment = Alignment.Center,
        ) { Words(text, 14.sp, bold = true) }
    }
}

// 4. This week's stickers

class StickersWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        Column(GlanceModifier.fillMaxSize().card().clickable(AppWidgets.openMetrics(context))) {
            Kicker("THIS WEEK'S STICKERS")
            Spacer(GlanceModifier.defaultWeight())
            listOf(s.weekStickers.take(4), s.weekStickers.drop(4)).forEach { row ->
                Row(GlanceModifier.fillMaxWidth()) {
                    row.forEach { sticker ->
                        Column(GlanceModifier.width(31.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val picture = remember(sticker) {
                                when {
                                    sticker.badge != null -> art.of(WidgetArt.sticker(sticker.badge!!, sticker.dateKey, 26f, art.scale))
                                    else -> art.of(WidgetArt.emptySlot(26f, art.scale))
                                }
                            }
                            ArtImage(picture, 26.dp)
                        }
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Words(s.streakLine, bold = true)
        }
    }
}

// 5. Week ahead

class WeekAheadWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        Column(GlanceModifier.fillMaxSize().card().clickable(AppWidgets.openDay(context, s.lightDateKey))) {
            Row(GlanceModifier.fillMaxWidth()) {
                Kicker("WEEK AHEAD")
                Spacer(GlanceModifier.defaultWeight())
                Words("Big, slow or rest", 11.sp, Gray)
            }
            Row(GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
                s.weekAhead.forEach { day ->
                    Column(
                        GlanceModifier.defaultWeight().fillMaxHeight().padding(vertical = 2.dp)
                            .then(if (day.isToday) GlanceModifier.background(ImageProvider(R.drawable.widget_today)) else GlanceModifier)
                            .clickable(AppWidgets.openDay(context, day.dateKey)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val color = when {
                            day.dayType == DayType.REST -> RestBar
                            day.done -> DoneGreen
                            else -> BarYellow
                        }
                        val height = when (day.dayType) { DayType.BIG -> 34.dp; DayType.SLOW -> 20.dp; DayType.REST -> 8.dp }
                        Box(
                            GlanceModifier.width(16.dp).height(height).background(color).cornerRadius(4.dp)
                                .then(if (day.light) GlanceModifier.background(ImageProvider(R.drawable.widget_bar_light)) else GlanceModifier),
                        ) {}
                        Spacer(GlanceModifier.height(3.dp))
                        Words(day.dayType.label, 11.sp, Gray)
                        Words(day.letter, 11.sp, if (day.light) Blue else Ink, bold = true)
                    }
                }
            }
            Row(GlanceModifier.fillMaxWidth()) {
                Words(s.lightLine ?: "No light day ahead.", lines = 1)
                s.suggestion?.let {
                    Spacer(GlanceModifier.width(4.dp))
                    Words(it, color = Blue, bold = true, lines = 1)
                }
            }
        }
    }
}

// 6. Mini diary

class DiaryWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        val openToday = AppWidgets.openDay(context, s.dateKey)
        Column(GlanceModifier.fillMaxSize().card().clickable(openToday)) {
            Row(GlanceModifier.fillMaxWidth().clickable(openToday), verticalAlignment = Alignment.CenterVertically) {
                Words(s.dateTitle, 15.sp, bold = true)
                Spacer(GlanceModifier.defaultWeight())
                Kicker("${s.dayType.label.uppercase()} DAY")
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                s.pillars.forEach { pillar ->
                    Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                        ArtImage(remember(pillar) { art.of(WidgetArt.friend(pillar, 26f, art.scale)) }, 26.dp)
                        Spacer(GlanceModifier.width(2.dp))
                        Words(pillar.pillar.title, 11.sp)
                    }
                }
            }
            Spacer(GlanceModifier.height(4.dp))
            val shown = if (s.tasks.size > 5) s.tasks.take(4) else s.tasks
            Column(GlanceModifier.fillMaxWidth().defaultWeight()) {
                if (s.tasks.isEmpty()) {
                    Words("Nothing planned yet. Tap to plan your day.", color = Gray, lines = 2, modifier = GlanceModifier.clickable(openToday))
                }
                shown.forEach { task ->
                    Row(
                        GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(openToday),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            GlanceModifier.size(26.dp).clickable(AppWidgets.toggleTask(task.taskId)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(GlanceModifier.size(15.dp).background(ImageProvider(if (task.done) R.drawable.widget_box_done else R.drawable.widget_box))) {}
                        }
                        Spacer(GlanceModifier.width(4.dp))
                        Words(task.title, color = if (task.done && !task.partial) Gray else Ink, struck = task.done && !task.partial, modifier = GlanceModifier.defaultWeight())
                        if (task.partial) Words("${task.progress}%", color = pillarInk(task.pillar))
                    }
                }
                if (s.tasks.size > shown.size) {
                    Words("+ ${s.tasks.size - shown.size} more", color = Gray, modifier = GlanceModifier.padding(start = 30.dp, top = 4.dp).clickable(openToday))
                }
            }
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(Rule)) {}
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Words("Water ${s.water}/${s.waterGoal}")
                Spacer(GlanceModifier.width(8.dp))
                Pill("+", AppWidgets.addGlass())
                Spacer(GlanceModifier.defaultWeight())
                s.moods.take(3).forEach { mood ->
                    Spacer(GlanceModifier.width(4.dp))
                    MoodFace(context, mood, art, 24.dp)
                }
            }
        }
    }
}

// 7. Month of stickers

class MonthWidget : SnapshotWidget() {
    @Composable
    override fun Content(context: Context, s: WidgetSnapshot, art: Art) {
        val month = s.month
        val crown = remember { art.of(WidgetArt.crown(14f, art.scale)) }
        Column(GlanceModifier.fillMaxSize().card().clickable(AppWidgets.openMetrics(context))) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Words(month.title, 15.sp, bold = true)
                Spacer(GlanceModifier.defaultWeight())
                Kicker(if (month.crowns == 1) "1 CROWN" else "${month.crowns} CROWNS")
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Spacer(GlanceModifier.width(16.dp))
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { letter ->
                    Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) { Words(letter, 11.sp, Gray) }
                }
            }
            month.weeks.forEach { week ->
                Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.width(16.dp), contentAlignment = Alignment.Center) {
                        if (week.crowned) ArtImage(crown, 14.dp, 10.5.dp)
                    }
                    week.days.forEach { day ->
                        Column(
                            GlanceModifier.defaultWeight().fillMaxHeight()
                                .then(if (day.isToday) GlanceModifier.background(ImageProvider(R.drawable.widget_today_outline)) else GlanceModifier),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Words("${day.day}", 11.sp, if (day.inMonth) Ink else Faint, bold = day.isToday)
                            val badge = day.badge
                            if (badge != null && day.inMonth) {
                                ArtImage(remember(day) { art.of(WidgetArt.sticker(badge, day.dateKey, 17f, art.scale)) }, 17.dp)
                            } else {
                                Spacer(GlanceModifier.height(17.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class WaterMoodWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WaterMoodWidget() }
class NextUpWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = NextUpWidget() }
class StickersWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StickersWidget() }
class WeekAheadWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeekAheadWidget() }
class DiaryWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = DiaryWidget() }
class MonthWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MonthWidget() }
