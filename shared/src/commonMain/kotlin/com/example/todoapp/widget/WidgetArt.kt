package com.example.todoapp.widget

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.todoapp.domain.Badge
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.BadgeInk
import com.example.todoapp.screen.basicutils.CrownGold
import com.example.todoapp.screen.basicutils.SketchInk
import com.example.todoapp.screen.diary.plan.friendRadius
import com.example.todoapp.screen.diary.plan.pillarFriend
import com.example.todoapp.screen.metrics.badges.badgeArt
import com.example.todoapp.screen.metrics.badges.badgeLines
import com.example.todoapp.screen.metrics.badges.badgeOutline
import com.example.todoapp.screen.metrics.badges.tilt
import com.example.todoapp.screen.metrics.badges.waterGlassArt
import com.example.todoapp.screen.metrics.badges.waterGlassLines
import com.example.todoapp.screen.metrics.badges.waterGlassOutline
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

/**
 * The app's own drawings as bitmaps, for widgets that can't run a Compose Canvas: the pillar friends
 * and the see-saw from PillarFriend.kt, and the stickers, glasses and mood faces from BadgeArt.kt.
 * Sizes are in dp; [scale] is pixels per dp.
 */
object WidgetArt {

    /** The balance see-saw with the four friends, 126 × 64 dp, tipped by [tilt] degrees. */
    fun seeSaw(pillars: List<WidgetPillar>, tilt: Float, scale: Float): ImageBitmap = render(126f, 64f, scale) {
        val u = 1.dp.toPx()
        rotate(tilt, Offset(63 * u, 46 * u)) {
            drawRoundRect(BadgeInk, Offset(4 * u, 44 * u), Size(118 * u, 5 * u), CornerRadius(2.5f * u))
            listOf(14f, 40f, 86f, 112f).zip(pillars) { x, p ->
                val r = friendRadius(p.grown, SEAT)
                pillarFriend(p.pillar, p.share, p.extra, p.grown, Offset(x * u, 44 * u - r), SEAT)
            }
        }
        val stand = Path().apply { moveTo(63 * u, 47 * u); lineTo(55 * u, 62 * u); lineTo(71 * u, 62 * u); close() }
        drawPath(stand, BadgeInk)
    }

    /** All done: the four friends beaming in a row, with a little confetti. 120 × 46 dp. */
    fun cheer(scale: Float): ImageBitmap = render(120f, 46f, scale) {
        val u = 1.dp.toPx()
        PillarType.scoredPillars.forEachIndexed { i, pillar ->
            pillarFriend(pillar, 1f, extra = false, grown = 1f, c = Offset((18 + i * 28) * u, 28 * u))
        }
        val gold = Color(0xFFF6C445)
        listOf(Offset(8f, 6f) to Offset(10f, 10f), Offset(60f, 1f) to Offset(60f, 6f), Offset(112f, 6f) to Offset(110f, 10f))
            .forEach { (a, b) -> drawLine(gold, a * u, b * u, 2 * u, StrokeCap.Round) }
    }

    /** A recharging day: one grey friend, dozing. [size] dp square. */
    fun rest(size: Float, scale: Float): ImageBitmap = render(size, size, scale) {
        val r = friendRadius(1f)
        val s = size.dp.toPx() * 0.36f / r
        pillarFriend(PillarType.RECHARGE, 0f, extra = false, grown = 1f, c = center.copy(y = center.y + r * s * 0.2f), scale = s)
    }

    /** One pillar's friend, as big as it has grown, in a [size] dp square that fits a full-grown one. */
    fun friend(pillar: WidgetPillar, size: Float, scale: Float): ImageBitmap = friend(pillar.pillar, pillar.share, pillar.extra, pillar.grown, size, scale)

    fun friend(pillar: PillarType, share: Float, extra: Boolean, grown: Float, size: Float, scale: Float): ImageBitmap = render(size, size, scale) {
        val s = size.dp.toPx() * 0.32f / friendRadius(1f)
        pillarFriend(pillar, share, extra, grown, c = center.copy(y = center.y + size.dp.toPx() * 0.1f), scale = s)
    }

    /** A badge as its sticker, with its shadow and the calendar's tilt for [date] (yyyy-MM-dd). */
    fun sticker(badge: Badge, date: String, size: Float, scale: Float): ImageBitmap = render(size, size, scale) {
        val day = runCatching { LocalDate.parse(date) }.getOrNull()
        sticker(badgeArt(badge), badgeOutline(badge), day?.let { tilt(badge, it) } ?: 0f)
    }

    /** A mood face: its sticker once picked, its grey line drawing until then. */
    fun mood(mood: WidgetMood, size: Float, scale: Float): ImageBitmap = render(size, size, scale) {
        if (mood.active) sticker(badgeArt(mood.badge), badgeOutline(mood.badge), 0f)
        else drawVector(badgeLines(mood.badge), this.size, ColorFilter.tint(SketchInk))
    }

    /** Water glass [index]: a sticker when [filled], else grey lines. */
    fun glass(index: Int, filled: Boolean, size: Float, scale: Float): ImageBitmap = render(size, size, scale) {
        val variant = index % 6
        if (filled) sticker(waterGlassArt(variant), waterGlassOutline(variant), (index * 5 % 7 - 3) * 1.5f)
        else drawVector(waterGlassLines(variant), this.size, ColorFilter.tint(SketchInk))
    }

    /** A balanced week's crown. */
    fun crown(size: Float, scale: Float): ImageBitmap = render(size, size * 0.75f, scale) {
        val k = this.size.width / 24f
        val crown = Path().apply {
            moveTo(2 * k, 16 * k); lineTo(4 * k, 4 * k); lineTo(9 * k, 10 * k); lineTo(12 * k, 2 * k)
            lineTo(15 * k, 10 * k); lineTo(20 * k, 4 * k); lineTo(22 * k, 16 * k); close()
        }
        drawPath(crown, CrownGold)
        drawPath(crown, BadgeInk, style = Stroke(1.4f * k, join = StrokeJoin.Round))
    }

    /** A day still to come: a dashed ring where its sticker will go. */
    fun emptySlot(size: Float, scale: Float): ImageBitmap = render(size, size, scale) {
        val u = 1.dp.toPx()
        drawCircle(
            Color(0xFFB4B2A9), radius = this.size.minDimension * 0.38f,
            style = Stroke(1.4f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * u, 3 * u)))
        )
    }

    private const val SEAT = 0.95f

    /** Draws into a new bitmap [widthDp] × [heightDp] dp, at [scale] pixels per dp. */
    fun render(widthDp: Float, heightDp: Float, scale: Float, draw: DrawScope.() -> Unit): ImageBitmap {
        val w = (widthDp * scale).roundToInt().coerceAtLeast(1)
        val h = (heightDp * scale).roundToInt().coerceAtLeast(1)
        val bitmap = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(scale), LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat()), draw)
        return bitmap
    }

    /** BadgeStickers' Sticker: the outline's soft shadow a touch lower, then the art, tilted. */
    private fun DrawScope.sticker(art: ImageVector, outline: ImageVector?, tilt: Float) {
        rotate(tilt) {
            if (outline != null) {
                translate(top = size.height * (1.6f / 52f)) { drawVector(outline, size, ColorFilter.tint(BadgeInk), alpha = 0.16f) }
            }
            drawVector(art, size)
        }
    }

    /** Draws [vector] to fill [size], the way a VectorPainter would. */
    private fun DrawScope.drawVector(vector: ImageVector, size: Size, filter: ColorFilter? = null, alpha: Float = 1f) {
        scale(size.width / vector.viewportWidth, size.height / vector.viewportHeight, Offset.Zero) {
            drawGroup(vector.root, filter, alpha)
        }
    }

    private fun DrawScope.drawGroup(group: VectorGroup, filter: ColorFilter?, alpha: Float) {
        withTransform({
            translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
            rotate(group.rotation, Offset.Zero)
            scale(group.scaleX, group.scaleY, Offset.Zero)
            translate(-group.pivotX, -group.pivotY)
            if (group.clipPathData.isNotEmpty()) clipPath(group.clipPathData.toPath())
        }) {
            for (node in group) {
                when (node) {
                    is VectorGroup -> drawGroup(node, filter, alpha)
                    is VectorPath -> {
                        val path = node.pathData.toPath().apply { fillType = node.pathFillType }
                        node.fill?.let { drawPath(path, it, alpha = node.fillAlpha * alpha, colorFilter = filter) }
                        val stroke = node.stroke
                        if (stroke != null && node.strokeLineWidth > 0f) {
                            drawPath(
                                path, stroke, alpha = node.strokeAlpha * alpha, colorFilter = filter,
                                style = Stroke(node.strokeLineWidth, node.strokeLineMiter, node.strokeLineCap, node.strokeLineJoin),
                            )
                        }
                    }
                }
            }
        }
    }
}
