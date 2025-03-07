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
fun PlanningLineText(
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
    Text(text= text,
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
        modifier = modifier.drawWithContent {
            drawContent() // Draw the text
//            val textLayoutResult = layoutResult ?: return@drawWithContent
            val lay = layout.value
            if(layout.value!= null) {
                if(lay!!.lineCount >0) {
                    val lineHeight = lay.size.height / lay.lineCount
                    var totalLengthOfStrike = 0f
                    (0..lay.lineCount-1).forEach {
                        totalLengthOfStrike += lay.getLineRight(it)
                    }
                    var highlightLength = strikePercentage * totalLengthOfStrike

                    var lineEndX = 0f
                    (0..lay.lineCount-1).forEach { i->
                        if(highlightLength > lay.getLineRight(i)){
                            //draw coloreded line full
                            lineEndX = lay.getLineRight(i)
                        } else {
                            //draw colored upto highlightLength
                            lineEndX = highlightLength
                        }
                        if(lineEndX>0f) {
                            drawLine(
                                color = strikeColor,
                                strokeWidth = 3f,
                                start = Offset(0f, (lineHeight * i) + (lineHeight / 2f)),
                                end = Offset(lineEndX, (lineHeight * i) + (lineHeight / 2f))
                            )
                        }
                        highlightLength = highlightLength - lay.getLineRight(i)
                    }

                    (0..lay.lineCount-1).forEach {
                        drawLine(
                            color = Color.Transparent,
                            strokeWidth = 2f,
                            start = Offset(0f, (lineHeight*it)+(lineHeight / 2f)),
                            end = Offset(lay.getLineRight(it), (lineHeight*it)+(lineHeight / 2f))
                        )
                    }
                }
            }
        })
}
