package com.example.todoapp.screen.basicblocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp


@Composable
fun FrequencyPeriodButton(
//    text: @Composable () -> Unit
    text: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(6.dp).wrapContentSize()) {
        Text(
            text = text,
//            fontFamily = anniFontFamily,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            textDecoration = TextDecoration.Underline,
            letterSpacing = TextUnit(1F, TextUnitType.Sp),
            fontSize = TextUnit(17f, type = TextUnitType.Sp),
            modifier = modifier.align(Alignment.Center)
        )
    }

//    Text(text=text, modifier = modifier,fontSize = TextUnit( 20f, type = TextUnitType.Sp))
}
