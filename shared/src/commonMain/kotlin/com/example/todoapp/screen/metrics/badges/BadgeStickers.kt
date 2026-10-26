package com.example.todoapp.screen.metrics.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.BadgeTier
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.domain.EarnedBadge
import com.example.todoapp.screen.basicutils.BadgeEveryday
import com.example.todoapp.screen.basicutils.BadgeGoodDay
import com.example.todoapp.screen.basicutils.BadgeInk
import com.example.todoapp.screen.basicutils.BadgeQuiet
import com.example.todoapp.screen.basicutils.BadgeRare
import com.example.todoapp.screen.basicutils.BadgeRarest
import com.example.todoapp.screen.basicutils.CardHeadInk
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.NoteInk
import com.example.todoapp.screen.basicutils.PeachText
import com.example.todoapp.screen.basicutils.StickerRim
import com.example.todoapp.screen.diary.plan.ChipText
import com.example.todoapp.screen.diary.plan.PlanButton
import kotlinx.datetime.LocalDate

/** A sticker's fill: its tier's, or "Recharging day"'s own quiet grey-lilac. */
fun badgeFill(badge: Badge): Color = if (badge == Badge.RECHARGING_DAY) BadgeQuiet else when (badge.tier) {
    BadgeTier.EVERYDAY -> BadgeEveryday
    BadgeTier.GOOD_DAY -> BadgeGoodDay
    BadgeTier.RARE -> BadgeRare
    BadgeTier.RAREST -> BadgeRarest
}

/**
 * A badge as a small round sticker in the notebook's style (spec 9.3): its fill inside a 2dp light rim, a
 * soft shadow, and a slight tilt that follows [date] and the badge, so a row looks placed by hand. The
 * icon takes 70% of [size] in dark ink. It says nothing to a screen reader; whatever shows it names it.
 */
@Composable
fun BadgeSticker(badge: Badge, date: LocalDate, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size)
            .rotate(tilt(badge, date))
            .shadow(if (size < 24.dp) 1.5.dp else 3.dp, CircleShape)
            .background(StickerRim, CircleShape)
            .padding(2.dp)
            .background(badgeFill(badge), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(badgeIcon(badge), contentDescription = null, tint = BadgeInk, modifier = Modifier.size(size * 0.7f))
    }
}

/** -7° to 7°, the same each time for a day and badge. */
private fun tilt(badge: Badge, date: LocalDate): Float = ((date.dayOfYear * 7 + badge.ordinal * 5) % 11 - 5) * 1.4f

/** "Recharging day. A quiet day." A row of stickers reads each one this way. */
private fun EarnedBadge.spoken(): String = "${badge.title}. ${reason.joinToString("") { it.text }}."

/**
 * Spec 9.3: a calendar day's badges, rarest first, each with its line. "Open this day" does what a tap on
 * a day without badges does.
 */
@Composable
fun DayBadgesDialog(day: CalendarDay, onOpen: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PlanButton("Open this day", filled = true, onClick = onOpen) },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = PeachText)) { Text("Close") }
        },
        title = { Text(day.title, modifier = Modifier.semantics { heading() }) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(day.badges, key = { it.badge.name }) { earned ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgeSticker(earned.badge, day.date, 32.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(earned.badge.title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                            ChipText(earned.reason, fontSize = 13.sp, color = InkGray, italic = true)
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}

/**
 * Spec 9.3: the stickers under the diary's day card, rarest first, each named underneath. Today's row is
 * "TODAY SO FAR". A sticker's line is what a screen reader says for it; the Metrics calendar shows it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayBadgesRow(badges: List<EarnedBadge>, date: LocalDate, isToday: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Text(
            if (isToday) "TODAY SO FAR" else "BADGES",
            fontSize = 11.sp, letterSpacing = 1.2.sp, color = CardHeadInk,
            modifier = Modifier.padding(start = 12.dp).semantics { heading() }
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { earned ->
                key(earned.badge) {
                    Column(
                        modifier = Modifier.width(76.dp).clearAndSetSemantics { contentDescription = earned.spoken() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BadgeSticker(earned.badge, date, 36.dp)
                        Text(
                            earned.badge.title,
                            fontSize = 12.sp, lineHeight = 15.sp, color = NoteInk, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
