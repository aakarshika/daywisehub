package com.example.todoapp.db.data.mood

import androidx.room.Embedded
import com.example.todoapp.db.data.mission.Mission
import com.example.todoapp.db.data.mission.missionstuff.MissionFrequency
import com.example.todoapp.db.data.pillar.Pillar
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.data.todotask.TodayTaskReminder

data class TodayMoodWithDetails(
    @Embedded val mood: Mood,
    @Embedded val todayMood: TodayMood?
)