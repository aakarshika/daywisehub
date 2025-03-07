package com.example.todoapp.screen.basicutils.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    maxLines: Int = Int. MAX_VALUE,
    textDecoration:TextDecoration = TextDecoration.None,
    fontWeight:FontWeight = FontWeight.Light,

) {
    Text(text= text,
//        fontFamily = anniFontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        maxLines = maxLines,
        letterSpacing = TextUnit(letterSpacing, TextUnitType.Sp),
        textDecoration = textDecoration,
        fontSize = TextUnit( fontSize, type = TextUnitType.Sp),
        modifier = modifier)
}
