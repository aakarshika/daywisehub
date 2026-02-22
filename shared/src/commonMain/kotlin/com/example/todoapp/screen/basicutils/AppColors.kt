package com.example.todoapp.screen.basicutils

import androidx.compose.ui.graphics.Color

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

fun Color.darken(amount: Float = 0.2f): Color {
    val red = (this.red * (1 - amount)).coerceIn(0f, 1f)
    val green = (this.green * (1 - amount)).coerceIn(0f, 1f)
    val blue = (this.blue * (1 - amount)).coerceIn(0f, 1f)
    return Color(red, green, blue, this.alpha)
}

fun getPillarColor(pillarName: String?): Color {
    return if (pillarName == "HEALTH") Pink80
    else if (pillarName == "WEALTH") Orange80
    else if (pillarName == "LOVE") Red80
    else if (pillarName == "LIFE") Blue80
    else Color(0xFFCCC2DC)
}

fun getDarkPillarColor(pillarName: String?): Color {
    return if (pillarName == "HEALTH") Pink180
    else if (pillarName == "WEALTH") Orange180
    else if (pillarName == "LOVE") Red180
    else if (pillarName == "LIFE") Blue180
    else Color(0xFFCCC2DC)
}
