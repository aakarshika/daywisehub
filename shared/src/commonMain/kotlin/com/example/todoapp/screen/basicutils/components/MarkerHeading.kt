package com.example.todoapp.screen.basicutils.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.block_b

/** A heading on the block_b marker swipe, sized to its text so long questions wrap instead of clipping. */
@Composable
fun MarkerHeading(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 34.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.heightIn(min = 32.dp), contentAlignment = Alignment.Center) {
            Image(
                painterResource(Res.drawable.block_b),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Text(
                text,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
                color = Color(0xFF2A2A2A),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
        }
    }
}
