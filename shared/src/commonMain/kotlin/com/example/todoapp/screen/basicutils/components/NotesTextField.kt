package com.example.todoapp.screen.basicutils.components
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTextField(
    taskName: String,
    placeHolder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences),
    modifier : Modifier = Modifier,
    valueChanged : (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = taskName,
        keyboardOptions = keyboardOptions,
        onValueChange = {vc->
            valueChanged(vc)},
        textStyle= TextStyle(
//            fontFamily = anniFontFamily,
            fontWeight = FontWeight.Light,
            color = Color.Black,
            lineHeight = TextUnit(27f, TextUnitType.Sp),
            fontStyle = FontStyle.Italic,
            letterSpacing = TextUnit(1f, TextUnitType.Sp),
            fontSize = TextUnit( 18f, type = TextUnitType.Sp)
        ),
        singleLine = false,
        modifier = modifier,
        interactionSource = interactionSource
    ){ b->
        TextFieldDefaults.DecorationBox(
            value = taskName,
            placeholder = { DiaryLineText("$placeHolder", color = Color.Gray) },
            innerTextField = b,
            enabled = true,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Gray,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            container = {
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    b()
                }
            }
        )
    }
}
