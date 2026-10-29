package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.BadgeInk
import com.example.todoapp.screen.basicutils.pillarStrong

private val MinRadius = 6.dp
private val MaxRadius = 12.dp
private val Red = Color(0xFFE2556B)
private val Gold = Color(0xFFF6C445)
private val Leaf = Color(0xFFA6CC76)
private val Stem = Color(0xFF6FA65A)
private val Blush = Color(0xFFF08497)

/**
 * A pillar as the app icon's little friend: a ball in the pillar's colour that grows with [grown] (by
 * default [share]), from
 * [MinRadius] at nothing to [MaxRadius] at the target, dozing under 45%, content after, beaming once full.
 * Health wears a sweatband, Wealth holds a coin, Love has a heart and Life a sprout. [extra] adds a
 * sparkle. It always takes the full size's room, centred, so the row doesn't shift. Drawn as in
 * docs/app-icon/balance.html.
 */
@Composable
fun PillarFriend(pillar: PillarType, share: Float, extra: Boolean, modifier: Modifier = Modifier, grown: Float = share) {
    Canvas(modifier.size(32.dp, 34.dp)) {
        pillarFriend(pillar, share, extra, grown, Offset(14.dp.toPx(), 21.dp.toPx()))
    }
}

/** A friend's radius at [grown], as [PillarFriend] draws it, times [scale]. */
fun DrawScope.friendRadius(grown: Float, scale: Float = 1f): Float =
    (MinRadius + (MaxRadius - MinRadius) * grown.coerceIn(0f, 1f)).toPx() * scale

/** [PillarFriend]'s drawing, centred on [c], so the widgets can draw the same friend into a bitmap. */
fun DrawScope.pillarFriend(pillar: PillarType, share: Float, extra: Boolean, grown: Float, c: Offset, scale: Float = 1f) {
    run {
        val r = friendRadius(grown, scale)
        val sw = maxOf(r * 0.15f, 1.2.dp.toPx() * scale)
        if (pillar == PillarType.LIFE) sprout(c, r, sw)
        drawCircle(pillarStrong(pillar), r, c)
        if (pillar == PillarType.HEALTH) {
            clipPath(Path().apply { addOval(Rect(c, r)) }) {
                val band = Path().apply {
                    moveTo(c.x - r, c.y - r * 0.45f)
                    quadraticTo(c.x, c.y - r * 0.7f, c.x + r, c.y - r * 0.45f)
                }
                drawPath(band, Red, style = Stroke(r * 0.22f))
            }
        }
        drawCircle(BadgeInk, r, c, style = Stroke(sw))
        face(c, r, sw, mood = when { share >= 1f -> 2; share >= 0.45f -> 1; else -> 0 })
        when (pillar) {
            PillarType.WEALTH -> {
                val coin = Offset(c.x + r * 0.84f, c.y + r * 0.55f)
                drawCircle(Gold, r * 0.36f, coin)
                drawCircle(BadgeInk, r * 0.36f, coin, style = Stroke(sw * 0.75f))
                drawCircle(BadgeInk.copy(alpha = 0.5f), r * 0.17f, coin, style = Stroke(sw * 0.5f))
            }
            PillarType.LOVE -> {
                val heart = heart(c.x, c.y - r * 1.12f, r * 0.3f)
                drawPath(heart, Red)
                drawPath(heart, BadgeInk, style = Stroke(sw * 0.55f))
            }
            else -> Unit
        }
        if (extra) sparkle(c.x + r * 1.05f, c.y - r * 0.95f, r * 0.38f)
    }
}

/** 0 dozing, 1 content, 2 beaming. */
private fun DrawScope.face(c: Offset, r: Float, sw: Float, mood: Int) {
    val ex = r * 0.34f
    val ey = c.y - r * 0.1f
    val my = c.y + r * 0.28f
    val eye = Stroke(sw * 0.85f, cap = StrokeCap.Round)
    when (mood) {
        2 -> for (x in listOf(c.x - ex, c.x + ex)) {
            val p = Path().apply {
                moveTo(x - r * 0.14f, ey + r * 0.05f)
                quadraticTo(x, ey - r * 0.15f, x + r * 0.14f, ey + r * 0.05f)
            }
            drawPath(p, BadgeInk, style = eye)
        }
        1 -> {
            drawCircle(BadgeInk, r * 0.1f, Offset(c.x - ex, ey))
            drawCircle(BadgeInk, r * 0.1f, Offset(c.x + ex, ey))
        }
        else -> for (x in listOf(c.x - ex, c.x + ex)) {
            drawLine(BadgeInk, Offset(x - r * 0.13f, ey), Offset(x + r * 0.13f, ey), sw * 0.85f, StrokeCap.Round)
        }
    }
    if (mood == 0) {
        drawCircle(BadgeInk, r * 0.08f, Offset(c.x, my + r * 0.05f), style = Stroke(sw * 0.7f))
    } else {
        val mouth = Path().apply {
            moveTo(c.x - r * 0.2f, my)
            quadraticTo(c.x, my + r * (if (mood == 2) 0.26f else 0.15f), c.x + r * 0.2f, my)
        }
        drawPath(mouth, BadgeInk, style = Stroke(sw * 0.8f, cap = StrokeCap.Round))
        drawCircle(Blush.copy(alpha = 0.55f), r * 0.13f, Offset(c.x - r * 0.58f, c.y + r * 0.18f))
        drawCircle(Blush.copy(alpha = 0.55f), r * 0.13f, Offset(c.x + r * 0.58f, c.y + r * 0.18f))
    }
}

private fun DrawScope.sprout(c: Offset, r: Float, sw: Float) {
    drawLine(Stem, Offset(c.x, c.y - r), Offset(c.x, c.y - r * 1.4f), sw, StrokeCap.Round)
    for ((dx, angle) in listOf(-0.24f to 25f, 0.24f to -25f)) {
        val leaf = Offset(c.x + r * dx, c.y - r * 1.42f)
        rotate(angle, leaf) {
            val topLeft = Offset(leaf.x - r * 0.24f, leaf.y - r * 0.12f)
            val size = Size(r * 0.48f, r * 0.24f)
            drawOval(Leaf, topLeft, size)
            drawOval(BadgeInk, topLeft, size, style = Stroke(sw * 0.5f))
        }
    }
}

private fun heart(x: Float, y: Float, s: Float) = Path().apply {
    moveTo(x, y + s * 0.9f)
    cubicTo(x - s * 1.6f, y - s * 0.1f, x - s * 0.9f, y - s * 1.1f, x, y - s * 0.35f)
    cubicTo(x + s * 0.9f, y - s * 1.1f, x + s * 1.6f, y - s * 0.1f, x, y + s * 0.9f)
    close()
}

private fun DrawScope.sparkle(x: Float, y: Float, s: Float) {
    val p = Path().apply {
        moveTo(x, y - s)
        quadraticTo(x + s * 0.12f, y - s * 0.12f, x + s, y)
        quadraticTo(x + s * 0.12f, y + s * 0.12f, x, y + s)
        quadraticTo(x - s * 0.12f, y + s * 0.12f, x - s, y)
        quadraticTo(x - s * 0.12f, y - s * 0.12f, x, y - s)
        close()
    }
    drawPath(p, Gold)
}

/**
 * The verdict's tiny see-saw: Health's friend on the left, Life's on the right, tipped [tilt] degrees
 * (positive dips the right).
 */
@Composable
fun MiniSeeSaw(tilt: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp, 14.dp)) {
        val u = 1.dp.toPx()
        rotate(tilt, Offset(11 * u, 8 * u)) {
            drawRoundRect(BadgeInk, Offset(u, 6 * u), Size(20 * u, 2.4f * u), CornerRadius(1.2f * u))
            drawCircle(pillarStrong(PillarType.HEALTH), 2.4f * u, Offset(4 * u, 3.6f * u))
            drawCircle(pillarStrong(PillarType.LIFE), 2.4f * u, Offset(18 * u, 3.6f * u))
        }
        val stand = Path().apply {
            moveTo(11 * u, 8.5f * u); lineTo(8.5f * u, 13 * u); lineTo(13.5f * u, 13 * u); close()
        }
        drawPath(stand, BadgeInk)
    }
}
