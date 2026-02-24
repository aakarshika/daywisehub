package com.example.todoapp.screen.basicutils.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType


@Composable
fun DiaryLineText(
    text: String,
    modifier: Modifier = Modifier,
    fontStyle:FontStyle = FontStyle.Italic,
    color:Color = Color.Black,
    strikeColor:Color = Color.Red,
    strikePercentage: Float = 0f,
    letterSpacing:Float = 1f,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    fontSize:Float= 18f,
    maxLines: Int = Int.MAX_VALUE,
    textDecoration:TextDecoration = TextDecoration.None,
) {
    val lineHight = TextUnit(27f, TextUnitType.Sp)
    val layout: MutableState<TextLayoutResult?> = mutableStateOf(null)
    Text(
        text = text,
        modifier = modifier,
//        fontFamily = anniFontFamily,
        fontWeight = FontWeight.Light,
        fontStyle = fontStyle,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        lineHeight = lineHight,
        onTextLayout = {it->
            layout.value = it
            onTextLayout(it)
        },
        maxLines = maxLines,
        letterSpacing = TextUnit(letterSpacing, TextUnitType.Sp),
        textDecoration = textDecoration,
        fontSize = TextUnit( fontSize, type = TextUnitType.Sp),
    )
}
