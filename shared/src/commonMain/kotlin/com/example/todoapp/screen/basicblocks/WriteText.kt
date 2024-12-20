package com.example.todoapp.screen.basicblocks

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType


@Composable
fun WriteText(
    text: String,
    modifier: Modifier = Modifier,
    fontStyle:FontStyle = FontStyle.Italic,
    color:Color = Color.Black,
    letterSpacing:Float = 1f,
    fontSize:Float= 18f,
    textDecoration:TextDecoration = TextDecoration.None,
) {
    Text(text= text,
//        fontFamily = anniFontFamily,
        fontWeight = FontWeight.Light,
        fontStyle = fontStyle,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        letterSpacing = TextUnit(letterSpacing, TextUnitType.Sp),
        textDecoration = textDecoration,
        fontSize = TextUnit( fontSize, type = TextUnitType.Sp),
        modifier = modifier)
}
