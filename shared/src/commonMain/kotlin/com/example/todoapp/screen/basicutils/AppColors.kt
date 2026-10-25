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
val CardCream = Color(0xFFFBF6E6)
val CrownGold = Color(0xFFD9A93A)
val LineBlueBorder = Color(0xFFCFE3F7)
val ChipBorder = Color(0xFFD9CDB8)
val CardBorder = Color(0xFFEDE6D6)

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
