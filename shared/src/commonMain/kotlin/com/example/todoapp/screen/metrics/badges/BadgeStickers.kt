package com.example.todoapp.screen.metrics.badges

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.BadgeTier
import com.example.todoapp.domain.CalendarDay
import com.example.todoapp.screen.basicutils.BadgeInk
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.Orange180
import com.example.todoapp.screen.basicutils.PeachInk
import com.example.todoapp.screen.diary.plan.ChipText
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.diary_heart_closed

/**
 * How much of its space a badge fills: the rarer, the bigger. Everyday and good-day stickers stay small,
 * Milestone is the one rare sticker, and the framed badges take the most room.
 */
private fun drawnShare(badge: Badge): Float = when {
    badge == Badge.RECHARGING_DAY -> 0.58f
    badge.tier == BadgeTier.EVERYDAY -> 0.62f
    badge.tier == BadgeTier.GOOD_DAY -> 0.7f
    badge.tier == BadgeTier.RARE -> if (isSticker(badge)) 0.76f else 0.88f
    else -> 1f
}

/**
 * A badge as a kid's reward sticker: a die-cut toon with a white edge and a soft shadow, or for the rare
 * ones a framed badge. It takes [size] and draws inside it by its rarity, unless [byRarity] is false; a
 * slight tilt follows [date] and the badge, so a row looks placed by hand. It says nothing to a screen
 * reader; whatever shows it names it.
 */
@Composable
fun BadgeSticker(badge: Badge, date: LocalDate, size: Dp, modifier: Modifier = Modifier, byRarity: Boolean = true) {
    val drawn = if (byRarity) size * drawnShare(badge) else size
    Sticker(badgeArt(badge), badgeOutline(badge), tilt(badge, date), size, drawn, modifier)
}

/** Water glass [variant] as a sticker, tilted by [tilt] degrees. */
@Composable
fun WaterGlassSticker(variant: Int, tilt: Float, size: Dp, modifier: Modifier = Modifier) {
    Sticker(waterGlassArt(variant), waterGlassOutline(variant), tilt, size, size, modifier)
}

/** A day factor ("guests") as a sticker, tilted by [tilt] degrees. Nothing for a name with no drawing. */
@Composable
fun DayFactorSticker(icon: String, tilt: Float, size: Dp, modifier: Modifier = Modifier) {
    val art = dayFactorArt(icon) ?: return
    Sticker(art, dayFactorOutline(icon), tilt, size, size, modifier)
}

/** [art] at [drawn] inside [size], tilted, over [outline]'s soft shadow when it has one. */
@Composable
private fun Sticker(art: ImageVector, outline: ImageVector?, tilt: Float, size: Dp, drawn: Dp, modifier: Modifier) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(drawn).rotate(tilt)) {
            outline?.let {
                Image(
                    rememberVectorPainter(it), contentDescription = null,
                    modifier = Modifier.matchParentSize().offset(y = drawn * (1.6f / 52f)),
                    alpha = 0.16f, colorFilter = ColorFilter.tint(BadgeInk)
                )
            }
            Image(rememberVectorPainter(art), contentDescription = null, modifier = Modifier.matchParentSize())
        }
    }
}

/** -7° to 7°, the same each time for a day and badge. */
internal fun tilt(badge: Badge, date: LocalDate): Float = ((date.dayOfYear * 7 + badge.ordinal * 5) % 11 - 5) * 1.4f

/**
 * Spec 9.3: a calendar day's badges. The rarest shows as its sticker with its line; the rest follow as a
 * plain bulleted list, rarest first. The diary icon ("Open this day") does what a tap on a day without
 * badges does; a tap outside closes it.
 */
@Composable
fun DayBadgesDialog(day: CalendarDay, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val top = day.badges.firstOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        // No buttons: a tap outside closes it, and the diary icon by the title opens the day.
        confirmButton = {},
        properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(day.title, modifier = Modifier.weight(1f).semantics { heading() })
                Box(
                    modifier = Modifier.padding(start = 12.dp).size(44.dp).clip(CircleShape).background(Orange180)
                        .clickable(role = Role.Button, onClickLabel = "Open this day") { onOpen() }
                        .semantics { contentDescription = "Open this day" },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painterResource(Res.drawable.diary_heart_closed), contentDescription = null,
                        colorFilter = ColorFilter.tint(PeachInk), modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                if (top != null) item(key = "top") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BadgeSticker(top.badge, day.date, 96.dp)
                        Text(top.badge.title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = InkBlack, modifier = Modifier.padding(top = 6.dp))
                        ChipText(top.reason, fontSize = 13.sp, color = InkGray, italic = true)
                    }
                }
                items(day.badges.drop(1), key = { it.badge.name }) { earned ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).semantics(mergeDescendants = true) {}) {
                        Text("•", fontSize = 14.sp, color = InkBlack, modifier = Modifier.width(16.dp))
                        Column {
                            Text(earned.badge.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkBlack)
                            ChipText(earned.reason, fontSize = 12.sp, color = InkGray, italic = true)
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}
