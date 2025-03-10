package com.example.todoapp.screen.diary.diaryitem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mood.WaterIntake
import com.example.todoapp.screen.basicutils.components.DiaryLineText
import com.example.todoapp.screen.basicutils.components.NotesLineText
import com.example.todoapp.screen.basicutils.components.NotesTextField
import com.example.todoapp.screen.basicutils.components.WritingTextField
import com.example.todoapp.screen.missions.Blue80
import kotlinx.datetime.LocalDate

@Composable
fun NotesItem(
                RowHeight: Int,
                water: WaterIntake?,
                selection:LocalDate,
                textArranged: (Int) -> Unit,
                upsertWaterIntake: (String) -> Unit
) {
    val nn = remember { mutableStateOf(water?.notes?:"") }
    val editing = remember { mutableStateOf(false ) }

    LaunchedEffect(selection){
        editing.value = false
    }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height((26+RowHeight).dp)){
        Box {
            Box(modifier = Modifier.fillMaxWidth()){
                Column(modifier = Modifier
                    .padding(top = (26+8).dp)
                    .height((RowHeight+5).dp)
                    .fillMaxWidth()
                ) {
                    (1..(RowHeight/26)-1).forEach {
                        NotebookLine()
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth().clickable {
                        editing.value = true
                    }
                ) {
                    if(editing.value) {
                        NotesEdit(nn, editing.value, upsertWaterIntake, textArranged)
                    } else {
                        NotesView(water,upsertWaterIntake, textArranged)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesView(
    water: WaterIntake?,
    upsertWaterIntake: (String) -> Unit,
    textArranged: (Int) -> Unit
) {
    Box(modifier = Modifier) {
        NotesLineText(text =water?.notes?:"",
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.lineCount > 1) {
                    textArranged(
                        textLayoutResult.lineCount
                    )
                }
            }
        )
    }
}
@Composable
private fun NotesEdit(
    nn: MutableState<String>,
    editing: Boolean,
    upsertWaterIntake: (String) -> Unit,
    textArranged: (Int) -> Unit
) {
    Box(modifier = Modifier) {
        val layout: MutableState<TextLayoutResult?> = mutableStateOf(null)
        BasicTextField(
            value = nn.value,
            onValueChange = { it: String ->
                nn.value = it
                upsertWaterIntake(nn.value)
            },
            textStyle = TextStyle(
                // fontFamily = anniFontFamily, // Uncomment if you have this defined
                fontWeight = FontWeight.Light,
                fontStyle = FontStyle.Italic,
                color = Color.Black,
                lineHeight = TextUnit(27f, TextUnitType.Sp),
                letterSpacing = TextUnit(1f, TextUnitType.Sp),
                fontSize = TextUnit(18f, TextUnitType.Sp),
                textAlign = TextAlign.Start
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .border(1.dp, Blue80, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .wrapContentHeight(),
            cursorBrush = SolidColor(Blue80),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences
            ),
            minLines = 6,
            maxLines = 100,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                layout.value = textLayoutResult

                if (textLayoutResult.lineCount > 1) {
                    textArranged(
                        textLayoutResult.lineCount
                    )
                }
            },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    if (nn.value.isEmpty()) {
                        Text(
                            text = " ",
                            style = TextStyle(
                                // fontFamily = anniFontFamily, // Uncomment if needed
                                fontWeight = FontWeight.Light,
                                fontStyle = FontStyle.Italic,
                                color = Color.Gray.copy(alpha = 0.6f),
                                lineHeight = TextUnit(27f, TextUnitType.Sp),
                                letterSpacing = TextUnit(1f, TextUnitType.Sp),
                                fontSize = TextUnit(18f, TextUnitType.Sp)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )


//                        NotesTextField(
//                            taskName = "${nn.value}",
//                            modifier = Modifier
//                                .fillMaxWidth(),
//                            placeHolder = "",
//                            valueChanged = { it ->
//                                nn.value = it
//                                upsertWaterIntake(nn.value)
//                            },
//                        )
    }
}
