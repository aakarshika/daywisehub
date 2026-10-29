package com.example.todoapp.screen

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The bottom tabs' hand-drawn line icons, 48 units square, from docs/badge-stickers/tab-icons.html.
 * A path with `true` is filled; the rest are strokes. Black, for [androidx.compose.material3.Icon] to tint.
 */
private fun lineIcon(name: String, vararg paths: Pair<String, Boolean>): ImageVector =
    ImageVector.Builder(name, 48.dp, 48.dp, 48f, 48f).apply {
        paths.forEach { (d, filled) ->
            if (filled) addPath(addPathNodes(d), fill = SolidColor(Color.Black))
            else addPath(
                addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

private fun line(d: String) = d to false
private fun fill(d: String) = d to true
private fun dot(x: Float, y: Float) = fill("M$x $y m-1.3 0 a1.3 1.3 0 1 0 2.6 0 a1.3 1.3 0 1 0 -2.6 0")

/** A spiral notebook with a heart on its label. */
val DiaryTabIcon: ImageVector by lazy {
    lineIcon(
        "diary",
        line("M13.2 6.4 C21 6.1 29.5 5.9 37.4 6.5 Q39.4 6.7 39.4 8.7 L39.7 39.6 Q39.7 41.8 37.5 41.8 C29.6 42.1 21.3 42.2 13.5 42.1 Q12.6 24 13.2 6.4 Z"),
        line("M17 11.4 L10.6 11.6 C8 11.8 8.1 15.4 10.6 15.3 L13 15.2"),
        line("M17 19.6 L10.4 19.7 C7.9 19.9 8 23.5 10.5 23.4 L13 23.3"),
        line("M17 27.8 L10.5 27.9 C8 28.1 8 31.7 10.6 31.6 L13.1 31.5"),
        line("M17.1 36 L10.7 36.1 C8.2 36.3 8.3 39.9 10.8 39.8 L13.3 39.7"),
        line("M21.2 11.3 C25.4 11 30 11 34.3 11.4 L34.1 21.4 C29.8 21.6 25.4 21.6 21.4 21.4 Z"),
        fill("M27.8 19.2 C24.4 16.9 24.4 13.8 26.2 13.7 C27.1 13.6 27.6 14.4 27.8 15 C28 14.4 28.5 13.6 29.4 13.7 C31.2 13.9 31.1 16.9 27.8 19.2 Z"),
        line("M21 27.2 C25 26.9 29.6 27 34.2 27.3"),
        line("M21.2 32.6 C24.2 32.4 27.2 32.5 30.2 32.7"),
    )
}

/** A target with an arrow in the bullseye. */
val MissionsTabIcon: ImageVector by lazy {
    lineIcon(
        "missions",
        line("M36.6 17.6 A16.8 16.4 0 1 1 30.4 11.4"),
        line("M31 20.9 A10.4 10.2 0 1 1 27.2 17"),
        fill("M22 26 m-3.6 0 a3.6 3.5 0 1 0 7.2 0 a3.6 3.5 0 1 0 -7.2 0"),
        line("M23.6 24.3 L39.2 9.1"),
        line("M26.8 24.6 L23.4 24.5 L23.6 21.2"),
        line("M39.2 9.1 L39.6 4.6 M39.2 9.1 L43.8 8.8"),
        line("M36.2 12 L36.6 7.6 M36.2 12 L40.7 11.7"),
    )
}

/** A desk calendar with one day starred. */
val MetricsTabIcon: ImageVector by lazy {
    lineIcon(
        "metrics",
        line("M8.3 13.1 C18.4 12.5 29.8 12.4 39.8 12.9 Q41.6 13 41.6 14.8 L41.1 39.4 Q41 41.2 39.2 41.2 L10 41.5 Q8.2 41.5 8.2 39.7 Z"),
        line("M8.4 19.4 C19.2 18.9 30.1 19 41.4 19.3"),
        line("M15 16 C13.3 13.6 13.6 9.6 15.7 9.2 C17.2 9 17.9 10.6 17.6 12.3"),
        line("M22.4 15.9 C20.7 13.5 21 9.5 23.1 9.1 C24.6 8.9 25.3 10.5 25 12.2"),
        line("M29.8 15.9 C28.1 13.5 28.4 9.5 30.5 9.1 C32 8.9 32.7 10.5 32.4 12.2"),
        dot(14.2f, 25f), dot(20.4f, 25f), dot(26.6f, 25.2f), dot(32.8f, 25f),
        dot(14f, 31.2f), dot(20.4f, 31f), dot(26.8f, 31.2f),
        dot(14.2f, 37.2f), dot(20.6f, 37f), dot(26.6f, 37.2f), dot(33f, 37f),
        fill("M33 26.6 L34.1 29.5 L37.2 29.6 L34.8 31.6 L35.6 34.6 L33 32.9 L30.4 34.6 L31.2 31.6 L28.8 29.6 L31.9 29.5 Z"),
    )
}
