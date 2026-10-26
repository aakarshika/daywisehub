package com.example.todoapp.screen.basicutils

import androidx.compose.ui.graphics.Color
import com.example.todoapp.domain.PillarType

// Orange shades
val Orange20 = Color(0xFFFDF5F1)
val Orange40 = Color(0xFFFFF1EB)
val Orange80 = Color(0xFFFFD4B8)
val Orange180 = Color(0xFFFCBE98)

// Red shades
val Red20 = Color(0xFFFCF1F3)
val Red40 = Color(0xFFFFEFF1)
val Red80 = Color(0xFFFFCBD2)
val Red180 = Color(0xFFFFB9C3)
val LightRed80 = Color(0xFFFFDAD5)

// Pink shades
val Pink20 = Color(0xFFF9F4FC)
val Pink40 = Color(0xFFFBF1FF)
val Pink80 = Color(0xFFF6D3FF)
val Pink180 = Color(0xFFF4CDFF)

// Blue shades
val Blue20 = Color(0xFFF4F8FD)
val Blue40 = Color(0xFFF0F7FF)
val Blue80 = Color(0xFFD4EBFF)
val Blue180 = Color(0xFFC7E4FF)

// Yellow shades
val Yellow80 = Color(0xFFFAE8BC)
val Yellow180 = Color(0xFFFAE0A0)
val Light_Yellowww = Color(0xFFFCF9ED)

// Green shades
val GREEN60 = Color(0xFFEAF3E7)
val GREEN80 = Color(0xFFE1F5D1)
val DARKGREEN60 = Color(0xFFD5E5CD)
val DARKGREEN180 = Color(0xFFCADEB1)
val DARKGREEN200 = Color(0xFFA6CC76)

// Gray shades
val Gray40 = Color(0xFFF6F6F6)
val Gray60 = Color(0xFFF1F1F1)
val Gray80 = Color(0xFFEDEDED)
val Gray100 = Color(0xFFE2E2E2)
val LightGray = Color(0xEEEEEEEE)

// Calendar gradients
val CalendarGradientA = Color(0xFFF0FFFB)
val CalendarGradientB = Color(0xFFF2E7FC)

// Diary ink and surfaces used by the redesigned screens
val InkBlack = Color(0xFF1F1F1F)
val InkGray = Color(0xFF6E6E6E)
val PeachText = Color(0xFF7A4A2C)
val PeachInk = Color(0xFF4A2A18)
val TrackBeige = Color(0xFFEFE9DA)
val StickyYellow = Color(0xFFFDF0C6)
val StickyBlue = Color(0xFFE3F1FF)
val CardCream = Color(0xFFFBF6E6)
val CrownGold = Color(0xFFD9A93A)
val LineBlueBorder = Color(0xFFCFE3F7)
val ChipBorder = Color(0xFFD9CDB8)
val CardBorder = Color(0xFFEDE6D6)

// The Metrics tab (plan 4)
/** Card headings, the icons in a card's rows and the ring numbers, as on the diary's highlights card. */
val CardHeadInk = Color(0xFF3A3A3A)
/** The icon in a card's corner, and the wand. */
val IconInk = Color(0xFF6B6B6B)
/** The line under a card's heading. */
val CardRule = Color(0xFFDDD6C2)
/** The rows of a highlights card. */
val CardRow = Color(0xFFFDFAF1)
/** Sentences in the diary's hand, and the calendar's day numbers. */
val NoteInk = Color(0xFF2A2A2A)
/** A mission line's progress text. */
val DetailInk = Color(0xFF5A5A5A)
/** The MISSIONS heading's marker, from the mockup. */
val MissionsMarker = Color(0xFFE8DEF8)
/** The month calendar's flat tints (spec 9.1): the card, and the week on screen. */
val CalendarTint = Color(0xFFF4F0FA)
val SelectedWeekTint = Color(0xFFE9E1F6)
val CalendarTitleInk = Color(0xFF4A4A4A)
/** The edge of a day ring with points or with nothing done: about 3:1 on both tints and on white. */
val DayRingEdge = Color(0xFF887CA1)
/** A past day with nothing on it: one thin line, plainer than an edged ring but visible on the week on screen. */
val EmptyDayRing = Color(0xFFB3A6C8)
/** The outline around today on the calendar, at least 3:1 on both tints. */
val TodayOutline = Color(0xFF7A6E94)

// Day badges (spec 9.3)
/** Sticker fills by tier: everyday cream, good-day peach, rare lilac, rarest gold. */
val BadgeEveryday = Color(0xFFEFE6CF)
val BadgeGoodDay = Color(0xFFFFD9BF)
val BadgeRare = Color(0xFFE3D7F7)
val BadgeRarest = Color(0xFFF8D98A)
/** "Recharging day" only: calm and set apart, so it comes to read as a day with nothing on it. */
val BadgeQuiet = Color(0xFFE4E0EC)
/** A sticker's light rim. */
val StickerRim = Color(0xFFFFFDF6)
/** A sticker's icon: at least 6:1 on every fill. */
val BadgeInk = Color(0xFF3B3226)

/** Headings, chips and highlights. */
fun pillarLight(pillar: PillarType): Color = when (pillar) {
    PillarType.HEALTH -> Color(0xFFF6D3FF)
    PillarType.WEALTH -> Color(0xFFFFD4B8)
    PillarType.LOVE -> Color(0xFFFFCBD2)
    PillarType.LIFE -> Color(0xFFD4EBFF)
    PillarType.RECHARGE -> Color(0xFFE2E2E2)
}

/** Checkboxes, rings, bars and strike-throughs. */
fun pillarStrong(pillar: PillarType): Color = when (pillar) {
    PillarType.HEALTH -> Color(0xFFE7B5F5)
    PillarType.WEALTH -> Color(0xFFF9B98F)
    PillarType.LOVE -> Color(0xFFF7A9B5)
    PillarType.LIFE -> Color(0xFFA9D1F5)
    PillarType.RECHARGE -> Color(0xFFC4C4C4)
}

/** Text on a pillar-light background. */
fun pillarInk(pillar: PillarType): Color = when (pillar) {
    PillarType.HEALTH -> Color(0xFF6B3D7A)
    PillarType.WEALTH -> Color(0xFF7A4A2C)
    PillarType.LOVE -> Color(0xFF7A3341)
    PillarType.LIFE -> Color(0xFF2F5677)
    PillarType.RECHARGE -> Color(0xFF555555)
}
