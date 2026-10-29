package com.example.todoapp.db.data.mood

import androidx.room.Embedded

data class TodayMoodWithDetails(
    @Embedded val mood: Mood,
    @Embedded val todayMood: TodayMood?
)