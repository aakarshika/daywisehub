package com.example.todoapp.screen.diary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.Blue180
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.Yellow180
import com.example.todoapp.screen.basicutils.pillarStrong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Drawn on a Canvas in common code: the Lottie view painted an opaque white background on iOS.
// A plain Canvas with no pointer input lets touches through to the Diary below.

private const val CONFETTI_MS = 1200
private const val BUBBLES_MS = 1000

private val confettiColors = listOf(
    pillarStrong(PillarType.HEALTH), pillarStrong(PillarType.WEALTH), pillarStrong(PillarType.LOVE),
    pillarStrong(PillarType.LIFE), Yellow180, CrownGold,
)

private class Confetto(
    val x0: Float, val vx: Float, val vy: Float, // fractions of the screen, per second
    val w: Float, val h: Float,                    // dp
    val rot0: Float, val spin: Float,              // degrees, degrees per second
    val flip: Float, val wobble: Float, val phase: Float,
    val color: Color,
)

@Composable
fun ConfettiBurst(onDone: () -> Unit) {
    val bits = remember {
        val r = Random
        List(54) {
            Confetto(
                x0 = 0.3f + r.nextFloat() * 0.4f,
                vx = (r.nextFloat() - 0.5f) * 1.3f,
                vy = -0.5f - r.nextFloat() * 0.9f,
                w = 6f + r.nextFloat() * 5f, h = 3f + r.nextFloat() * 3f,
                rot0 = r.nextFloat() * 360f, spin = (r.nextFloat() - 0.5f) * 900f,
                flip = 4f + r.nextFloat() * 8f, wobble = 0.01f + r.nextFloat() * 0.03f, phase = r.nextFloat() * 6.28f,
                color = confettiColors[r.nextInt(confettiColors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        val t = p * CONFETTI_MS / 1000f
        val alpha = if (p < 0.65f) 1f else (1f - (p - 0.65f) / 0.35f).coerceIn(0f, 1f)
        val gravity = 1.6f
        bits.forEach { b ->
            val x = (b.x0 + b.vx * t + sin(t * 6f + b.phase) * b.wobble) * size.width
            val y = (0.32f + b.vy * t + 0.5f * gravity * t * t) * size.height
            val w = b.w.dp.toPx()
            val h = b.h.dp.toPx()
            val center = Offset(x, y)
            rotate(b.rot0 + b.spin * t, pivot = center) {
                // Squash one side so the paper seems to turn over as it falls.
                scale(scaleX = cos(t * b.flip + b.phase), scaleY = 1f, pivot = center) {
                    drawRect(b.color, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), alpha = alpha)
                }
            }
        }
    }
}

private class Bubble(
    val x: Float,      // fraction of the width
    val rise: Float,   // dp over the whole run
    val r: Float,      // dp
    val pop: Float,    // progress at which it pops
    val delay: Float,  // progress at which it shows
    val wobble: Float, val phase: Float,
)

/** Soft blue bubbles that rise from [originY] (px from the top, or near the bottom when null) and pop. */
@Composable
fun WaterBubbles(originY: Float?, onDone: () -> Unit) {
    val bubbles = remember {
        val r = Random
        List(14) {
            val delay = r.nextFloat() * 0.25f
            Bubble(
                x = 0.12f + r.nextFloat() * 0.76f,
                rise = 70f + r.nextFloat() * 110f,
                r = 4f + r.nextFloat() * 7f,
                pop = delay + 0.45f + r.nextFloat() * (0.42f - delay),
                delay = delay,
                wobble = 3f + r.nextFloat() * 5f, phase = r.nextFloat() * 6.28f,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(BUBBLES_MS, easing = LinearEasing))
        onDone()
    }
    val fill = Blue180
    val rim = pillarStrong(PillarType.LIFE)
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        val baseY = originY ?: (size.height * 0.8f)
        bubbles.forEach { b ->
            if (p < b.delay) return@forEach
            val life = (p - b.delay) / (b.pop - b.delay)
            val popLen = 0.12f
            val local = life.coerceAtMost(1f)
            val x = b.x * size.width + sin(local * 2f * PI.toFloat() + b.phase) * b.wobble.dp.toPx()
            val y = baseY - b.rise.dp.toPx() * (1f - (1f - local) * (1f - local))
            val radius = b.r.dp.toPx() * (0.6f + 0.4f * local)
            val center = Offset(x, y)
            if (p < b.pop) {
                drawCircle(fill, radius, center, alpha = 0.7f)
                drawCircle(rim, radius, center, style = Stroke(1.5.dp.toPx()))
                drawCircle(Color.White, radius * 0.28f, center + Offset(-radius * 0.35f, -radius * 0.35f), alpha = 0.8f)
            } else {
                // Pop: a ring that grows and fades, plus a few droplets flung out.
                val q = ((p - b.pop) / popLen).coerceIn(0f, 1f)
                if (q >= 1f) return@forEach
                val a = 1f - q
                drawCircle(rim, radius * (1f + 0.8f * q), center, alpha = a, style = Stroke(1.2.dp.toPx()))
                repeat(5) { i ->
                    val ang = b.phase + i * (2f * PI.toFloat() / 5f)
                    val d = radius * (1.1f + 1.2f * q)
                    drawCircle(rim, 1.4.dp.toPx(), center + Offset(cos(ang) * d, sin(ang) * d), alpha = a)
                }
            }
        }
    }
}
