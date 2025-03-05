package com.example.todoapp.screen.metrics.metriccomponents
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
val options = listOf("HEALTH", "WEALTH", "LOVE", "LIFE")

@Composable
fun TaskTypeDropdown(optionsSelected: (List<String>) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // Create separate state variables for each option for direct updates
    val optionStates = options.associateWith { remember { mutableStateOf(true) } }

    // Function to create a mutable map from current state values

    val getSelectedOptionsMap    = {
        optionStates.filter { it.value.value }.keys.toList()
    }

    // Compute whether all items are selected
    val allSelected = optionStates.values.all { it.value }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display selected options or placeholder text
            Text(
                text = if (optionStates.any { it.value.value }) {
                    if (allSelected) "Overall Progress" else
                        optionStates.filter { it.value.value }.keys.joinToString(", ")
                } else {
                    "Select Pillar(s) to view metrics"
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Arrow"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // Select All / Unselect All option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val newValue = !allSelected
                        optionStates.forEach { (_, state) ->
                            state.value = newValue
                        }
                        optionsSelected(getSelectedOptionsMap())
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                    contentDescription = if (allSelected) "Checked" else "Unchecked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (allSelected) "Unselect All" else "Select All",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Divider()

            // Individual options
            options.forEach { option ->
                val isChecked = optionStates[option]?.value ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            optionStates[option]?.value = !isChecked
                            optionsSelected(getSelectedOptionsMap())
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                        contentDescription = if (isChecked) "Checked" else "Unchecked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = option,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Done button
            Divider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = false }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}