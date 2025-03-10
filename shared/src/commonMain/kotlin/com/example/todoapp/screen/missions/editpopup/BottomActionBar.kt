package com.example.todoapp.screen.missions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bottom action bar with save and cancel buttons
 */
@Composable
fun BottomActionBar(
    page1: Boolean,
    onNextClicked: () -> Unit,
    onPrevClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    pillarColor: Color
) {
    Surface(
        color = pillarColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            Icon(
                Icons.Filled.Close,
                contentDescription = "Cancel",
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
                    .padding(end = 5.dp)
                    .clickable(onClick = onCloseClicked)
            )

            Spacer(modifier = Modifier.width(16.dp))
            if(page1){
                // Continue button
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Continue",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .clickable(onClick = onNextClicked)
                )
                // Continue button
            }
            else {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "back",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .clickable(onClick = onPrevClicked)
                )
                // Save button
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Save",
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .clickable(onClick = onSaveClicked)
                )
            }
        }
    }
}