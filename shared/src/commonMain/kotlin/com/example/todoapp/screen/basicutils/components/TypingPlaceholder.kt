package com.example.todoapp.screen.basicutils.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

/** A placeholder that types each example in, holds it, erases it and moves on to the next. */
@Composable
fun TypingPlaceholder(examples: List<String>, style: TextStyle, modifier: Modifier = Modifier) {
    // A random start so fields side by side don't type the same thing.
    val order = remember(examples) { examples.shuffled() }
    // The first example starts whole, so a quick look never catches half a word.
    var shown by remember { mutableStateOf(order.firstOrNull().orEmpty()) }
    LaunchedEffect(order) {
        if (order.isEmpty()) return@LaunchedEffect
        var i = 0
        while (true) {
            val text = order[i % order.size]
            if (i > 0) for (n in 1..text.length) { shown = text.take(n); delay(65) }
            delay(1600)
            for (n in text.length - 1 downTo 0) { shown = text.take(n); delay(30) }
            delay(350)
            i++
        }
    }
    Text(shown, style = style, modifier = modifier)
}

/** Focuses a field once it is laid out and opens the keyboard, which iOS won't do from focus alone. */
suspend fun focusAndShowKeyboard(focus: FocusRequester, keyboard: SoftwareKeyboardController?) {
    withFrameNanos { }
    focus.requestFocus()
    keyboard?.show()
}
