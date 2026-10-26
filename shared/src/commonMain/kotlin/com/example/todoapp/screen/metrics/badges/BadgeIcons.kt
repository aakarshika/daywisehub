package com.example.todoapp.screen.metrics.badges

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.example.todoapp.domain.Badge

/**
 * One small icon per badge (spec 9.3), drawn here since only material-icons-core is available. Each sits
 * on a 24-unit grid as SVG path data, mostly round 2-unit strokes, plain enough to read at 14dp. Its color
 * is whatever the sticker tints it.
 */
fun badgeIcon(badge: Badge): ImageVector = icons.getValue(badge)

/** Built once, on first use. */
private val icons: Map<Badge, ImageVector> by lazy { Badge.entries.associateWith { draw(it) } }

private fun draw(badge: Badge): ImageVector = when (badge) {
    Badge.RECHARGING_DAY -> icon("moon") {
        line("M20 13.5A8.5 8.5 0 1 1 10.5 4A7.5 7.5 0 0 0 20 13.5Z")
    }
    Badge.DID_SOME -> icon("halfFullCircle") {
        line("M4 12A8 8 0 1 1 20 12A8 8 0 1 1 4 12Z")
        solid("M4 12A8 8 0 0 0 20 12Z")
    }
    Badge.FIRST_ONE -> icon("check") {
        line("M5 12.5L10 17.5L19 7", width = 2.4f)
    }
    Badge.STRONG_START -> icon("sunrise") {
        line("M3 18H21M7 18A5 5 0 0 1 17 18M12 7.8V10.3M5.3 11.3L6.7 12.7M18.7 11.3L17.3 12.7")
    }
    Badge.RESTED -> icon("leaf") {
        line("M5 19C5 11 10 5 19 5C19 14 13 19 5 19ZM5 19L12.5 11.5")
    }
    Badge.HALFWAY -> icon("halfPie") {
        line("M4 12A8 8 0 1 1 20 12A8 8 0 1 1 4 12Z")
        solid("M12 4A8 8 0 0 1 12 20Z")
    }
    Badge.TWO_IN_A_ROW -> icon("linkedDots") {
        solid("M3.5 12A3 3 0 1 1 9.5 12A3 3 0 1 1 3.5 12ZM14.5 12A3 3 0 1 1 20.5 12A3 3 0 1 1 14.5 12Z")
        line("M9 12H15")
    }
    Badge.SHOWED_UP -> icon("clock") {
        line("M3.5 12A8.5 8.5 0 1 1 20.5 12A8.5 8.5 0 1 1 3.5 12ZM12 7.5V12L15 14")
    }
    Badge.BONUS_ROUND -> icon("plus") {
        line("M12 5.5V18.5M5.5 12H18.5", width = 2.6f)
    }
    Badge.SECOND_CHANCE -> icon("redoArrow") {
        line("M5 13A7 7 0 0 1 18.6 10.6M14.7 8.4L18.6 10.6L20.1 6.4")
    }
    Badge.HAT_TRICK -> icon("threeDots") {
        solid("M9.2 6.5A2.8 2.8 0 1 1 14.8 6.5A2.8 2.8 0 1 1 9.2 6.5ZM3.7 16.5A2.8 2.8 0 1 1 9.3 16.5A2.8 2.8 0 1 1 3.7 16.5ZM14.7 16.5A2.8 2.8 0 1 1 20.3 16.5A2.8 2.8 0 1 1 14.7 16.5Z")
    }
    Badge.CROWNED -> icon("crown") {
        line("M4 18L5 8L9.5 12L12 6L14.5 12L19 8L20 18Z")
    }
    Badge.HEAVY_LIFTING -> icon("dumbbell") {
        line("M3.5 12H20.5M3.5 9.5V14.5M20.5 9.5V14.5")
        line("M7 7.5V16.5M17 7.5V16.5", width = 3.2f)
    }
    Badge.FULL_ENOUGH -> icon("pie80") {
        line("M4 12A8 8 0 1 1 20 12A8 8 0 1 1 4 12Z")
        solid("M12 12L12 4A8 8 0 1 1 4.39 9.53Z")
    }
    Badge.COUNT_IT -> icon("tally") {
        line("M6 6V18M9.5 6V18M13 6V18M16.5 6V18M3.5 15.5L19.5 8.5")
    }
    Badge.WEEKEND_WARRIOR -> icon("sun") {
        line("M8.2 12A3.8 3.8 0 1 1 15.8 12A3.8 3.8 0 1 1 8.2 12ZM18.3 12H20.8M3.2 12H5.7M12 18.3V20.8M12 3.2V5.7M16.45 16.45L18.22 18.22M7.55 16.45L5.78 18.22M7.55 7.55L5.78 5.78M16.45 7.55L18.22 5.78")
    }
    Badge.BACK_AT_IT -> icon("arrowIntoDoor") {
        line("M14 4H17.5A1.5 1.5 0 0 1 19 5.5V18.5A1.5 1.5 0 0 1 17.5 20H14M4 12H14M10.5 8.5L14 12L10.5 15.5")
    }
    Badge.OVERDUE_NO_MORE -> icon("bellWithCheck") {
        line("M6.5 16.5V11A5.5 5.5 0 0 1 17.5 11V16.5L19 18H5ZM12 5.5V3.8M10.3 20.6H13.7")
        line("M9.4 12.2L11.3 14.1L14.6 10.4", width = 1.8f)
    }
    Badge.GENTLE_WIN -> icon("feather") {
        line("M18.5 5.5C12 6 8 10.5 7.5 16.5C13.5 16 18 12 18.5 5.5ZM4.5 19.5L15 9")
    }
    Badge.ALL_FOUR -> icon("fourSquares") {
        line("M5 5H10.5V10.5H5ZM13.5 5H19V10.5H13.5ZM5 13.5H10.5V19H5ZM13.5 13.5H19V19H13.5Z")
    }
    Badge.ALL_THREE_CROWNS -> icon("crownThreeDots") {
        line("M5 14L6 6.5L9.5 9.5L12 5L14.5 9.5L18 6.5L19 14Z")
        solid("M5.7 18.5A1.8 1.8 0 1 1 9.3 18.5A1.8 1.8 0 1 1 5.7 18.5ZM10.2 18.5A1.8 1.8 0 1 1 13.8 18.5A1.8 1.8 0 1 1 10.2 18.5ZM14.7 18.5A1.8 1.8 0 1 1 18.3 18.5A1.8 1.8 0 1 1 14.7 18.5Z")
    }
    Badge.CLEAN_SWEEP -> icon("sparkle") {
        line("M12 3.5C12.7 8.6 15.4 11.3 20.5 12C15.4 12.7 12.7 15.4 12 20.5C11.3 15.4 8.6 12.7 3.5 12C8.6 11.3 11.3 8.6 12 3.5Z")
    }
    Badge.MILESTONE -> icon("flag") {
        line("M6 21V3.5M6 4.5H17L14.5 8.5L17 12.5H6")
    }
    Badge.PILLAR_FILLED -> icon("filledRing") {
        line("M4.5 12A7.5 7.5 0 1 1 19.5 12A7.5 7.5 0 1 1 4.5 12Z", width = 3.4f)
    }
    Badge.THE_RETURN -> icon("house") {
        line("M3.5 11.5L12 4L20.5 11.5M6 9.5V20H18V9.5M10 20V15H14V20")
    }
    Badge.SEVEN_DAY_RUN -> icon("flame") {
        line("M12 3.5C13.5 7.5 18 9.5 18 14.5C18 18 15.3 20.5 12 20.5C8.7 20.5 6 18 6 14.5C6 11.5 8 9.5 9.5 8C9.5 10.5 10.5 12 12 12.5C11.5 9.5 11 6.5 12 3.5Z")
    }
    Badge.FINISH_LINE -> icon("checkeredFlag") {
        line("M5 21V4M5 4.5H19V13.5H5")
        solid("M5 4.5H9.67V7.5H5ZM14.33 4.5H19V7.5H14.33ZM9.67 7.5H14.33V10.5H9.67ZM5 10.5H9.67V13.5H5ZM14.33 10.5H19V13.5H14.33Z")
    }
    Badge.RE_RETURN -> icon("houseReturnArrow") {
        line("M5.5 15L12 9.5L18.5 15M7.5 13.5V20.5H16.5V13.5M18.5 10A7.5 7.5 0 0 0 5.5 10M4.9 6.75L5.5 10L8.6 8.87")
    }
    Badge.WEEK_MAKER -> icon("bigCrown") {
        solid("M3 18L4 6.5L9 11L12 4L15 11L20 6.5L21 18Z")
        line("M3 18L4 6.5L9 11L12 4L15 11L20 6.5L21 18ZM4 21H20")
    }
    Badge.THIRTY_DAY_RUN -> icon("flameInRing") {
        line("M2.5 12A9.5 9.5 0 1 1 21.5 12A9.5 9.5 0 1 1 2.5 12Z")
        line("M12 6C13 8.6 15.8 9.9 15.8 13.2C15.8 15.5 14.1 17.2 12 17.2C9.9 17.2 8.2 15.5 8.2 13.2C8.2 11.2 9.5 9.9 10.4 8.9C10.4 10.5 11 11.5 12 11.8C11.7 9.9 11.4 7.9 12 6Z", width = 1.8f)
    }
    Badge.PERSONAL_BEST -> icon("trophy") {
        line("M7.5 4H16.5V9A4.5 4.5 0 0 1 7.5 9ZM7.5 5.5H4.5V7A3.5 3.5 0 0 0 8 10.5M16.5 5.5H19.5V7A3.5 3.5 0 0 1 16 10.5M12 13.5V17.5M9.5 17.5H14.5M8 20.5H16")
    }
}

private fun icon(name: String, paths: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply(paths)
        .build()

/** A stroke with round ends and corners along SVG path data. */
private fun ImageVector.Builder.line(pathData: String, width: Float = 2f) {
    addPath(
        addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}

/** A filled shape along SVG path data. */
private fun ImageVector.Builder.solid(pathData: String) {
    addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
}
