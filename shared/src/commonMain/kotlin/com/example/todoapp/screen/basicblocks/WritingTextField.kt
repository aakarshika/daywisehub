package com.example.todoapp.screen.basicblocks
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
fun WritingTextField(
    taskName: String,
    placeHolder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words),
    modifier : Modifier = Modifier,
    valueChanged : (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = taskName,
        keyboardOptions = keyboardOptions,
        onValueChange = {
            valueChanged(it)},
        textStyle= TextStyle(
//            fontFamily = anniFontFamily,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            letterSpacing = TextUnit(1F, TextUnitType.Sp),
            fontSize = TextUnit( 18f, type = TextUnitType.Sp)
        ),
        singleLine = false,
        modifier = modifier,
        interactionSource = interactionSource
    ){
        TextFieldDefaults.DecorationBox(
            value = taskName,
            placeholder = { WriteText("$placeHolder", color = Color.Gray) },
            innerTextField = it,
            enabled = true,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            colors = TextFieldDefaults.colors(
                disabledTextColor = Color.Gray,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
        )
    }
}
