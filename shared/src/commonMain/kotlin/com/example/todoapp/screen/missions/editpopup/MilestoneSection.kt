package com.example.todoapp.screen.missions.editpopup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.milestone.Milestone
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicutils.components.WritingTextField
import com.example.todoapp.screen.missions.getOrdinal
import kotlinx.datetime.number

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Composable for managing mission milestones
 */
@Composable
fun MilestonesSection(
    milestones: MutableState<List<Milestone>?>,
    mission: MutableState<Mission?>,
    fieldBackground: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MILESTONES:",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
            )

            // Add milestone button
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Milestone",
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        val newMilestone = Milestone(
                            text = "My ${getOrdinal((milestones.value?.size ?: 0) + 1)} Milestone",
                            priority = "HIGH",
                            expectedCompletionDate = MyDate("2025-3-13"),
                            milestoneOrder = milestones.value?.size ?: 1,
                            status = "ACTIVE",
                            missionId = mission.value?.missionId ?: 0L,
                            progressValue = 0f,
                            progressUnit = 0f,
                            progressText = "",
                            mileStartDate = MyDate("2025-1-1"),
                            actualCompletionDate = null,
                            completionTaskId = null,
                            milestoneId = 0L
                        )
                        milestones.value = milestones.value?.plus(newMilestone)
                    }
            )
        }

        // Milestones list
        milestones.value?.forEachIndexed { index, milestone ->
            MilestoneItem(
                milestone = milestone,
                fieldBackground = fieldBackground,
                onTextChange = { newText ->
                    milestones.value = milestones.value?.mapIndexed { i, m ->
                        if (i == index) milestone.copy(text = newText) else m
                    }
                },
                onDateChange = { newDate ->
                    milestones.value = milestones.value?.mapIndexed { i, m ->
                        if (i == index) milestone.copy(expectedCompletionDate = MyDate(newDate)) else m
                    }
                },
                onRemove = {
                    milestones.value = milestones.value?.filterIndexed { i, _ -> i != index }
                }
            )
        }

        // Empty state
        if (milestones.value.isNullOrEmpty()) {
            Surface(
                color = fieldBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "No milestones yet. Add your first one with the + button above.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Individual milestone item with text and due date
 */
@Composable
fun MilestoneItem(
    milestone: Milestone,
    fieldBackground: Color,
    onTextChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = fieldBackground,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Milestone text
                WritingTextField(
                    taskName = milestone.text,
                    valueChanged = onTextChange,
                    placeHolder = "Looking forward to",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Due on: ",
                        fontSize = 14.sp
                    )
                    DateSelector(milestone.expectedCompletionDate, onDateSelected={d->onDateChange(MyDate.fromLocalDate(d).dateString)})
                }
                // Due date row
            }

            // Remove button
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Milestone",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

/**
 * A date selector component that displays the selected date and opens a date picker dialog
 * when clicked.
 */
@Composable
fun DateSelector(
    selectedDate: MyDate,
    onDateSelected: (LocalDate) -> Unit,
    accentColor: Color = Color(0xFF4A6572)
) {
    var showDialog by remember { mutableStateOf(false) }

    // Date display with calendar icon
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Calendar",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${selectedDate.dateString}",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Open date picker",
                tint = Color.Gray
            )
        }
    }

    // Date picker dialog
    if (showDialog) {
        DatePickerDialog(
            initialDate = selectedDate.toLocalDate(),
            onDateSelected = {
                onDateSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            accentColor = accentColor
        )
    }
}

/**
 * A date picker dialog that allows selecting a date.
 */
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    var currentMonth by remember { mutableStateOf(initialDate.month) }
    var currentYear by remember { mutableStateOf(initialDate.year) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Calendar header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month and year display
                    Text(
                        text = "${currentMonth.name.lowercase().capitalize()} $currentYear",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Previous month button
                    IconButton(
                        onClick = {
                            if (currentMonth == Month.JANUARY) {
                                currentMonth = Month.DECEMBER
                                currentYear--
                            } else {
                                currentMonth = Month(currentMonth.number - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = accentColor
                        )
                    }

                    // Next month button
                    IconButton(
                        onClick = {
                            if (currentMonth == Month.DECEMBER) {
                                currentMonth = Month.JANUARY
                                currentYear++
                            } else {
                                currentMonth = Month(currentMonth.number + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week header
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Calendar grid
                val daysInMonth = getDaysInMonth(currentYear, currentMonth)
                val firstDayOfMonth = getFirstDayOfMonth(currentYear, currentMonth)

                // Calculate grid cells
                val totalDays = daysInMonth
                val firstDayOffset = firstDayOfMonth
                val totalCells = if (totalDays + firstDayOffset > 35) 42 else 35

                // Calendar rows
                for (i in 0 until totalCells / 7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        for (j in 0 until 7) {
                            val dayIndex = i * 7 + j
                            val dayNumber = dayIndex - firstDayOffset + 1

                            if (dayNumber in 1..totalDays) {
                                val date = LocalDate(currentYear, currentMonth, dayNumber)
                                val isSelected = date == selectedDate
                                val isToday = isDateToday(date)

                                // Day cell
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> accentColor
                                                isToday -> accentColor.copy(alpha = 0.2f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        color = when {
                                            isSelected -> Color.White
                                            else -> Color.Black
                                        },
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            } else {
                                // Empty cell for padding days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(36.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Cancel button
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { onDismiss() },
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "CANCEL",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // OK button
                    Surface(
                        modifier = Modifier
                            .clickable { onDateSelected(selectedDate) },
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "OK",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper functions

/**
 * Formats a LocalDate as a readable string (e.g., "Jan 15, 2023")
 */
private fun formatDate(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().capitalize()
    val day = date.dayOfMonth
    val year = date.year
    return "$month $day, $year"
}

/**
 * Capitalizes the first letter of a string
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}

/**
 * Gets the number of days in a month
 */
private fun getDaysInMonth(year: Int, month: Month): Int {
    return when (month) {
        Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
}

/**
 * Determines if a year is a leap year
 */
private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

/**
 * Gets the day of week (0-6, Sunday = 0) for the first day of a month
 */
private fun getFirstDayOfMonth(year: Int, month: Month): Int {
    // Zeller's Congruence algorithm
    val m = if (month.number < 3) month.number + 12 else month.number
    val y = if (month.number < 3) year - 1 else year

    val h = (1 + ((13 * (m + 1)) / 5) + y + (y / 4) - (y / 100) + (y / 400)) % 7

    // Convert to Sunday = 0 format (Zeller's gives Saturday = 0)
    return (h + 6) % 7
}

/**
 * Checks if a date is today
 */
private fun isDateToday(date: LocalDate): Boolean {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return date == today
}