package com.example.todoapp.reminders

import com.example.todoapp.domain.Reminder

/** The platform's local notifications. [replaceAll] drops what was scheduled before and schedules [reminders]. */
interface ReminderScheduler {
    fun askPermission()
    fun replaceAll(reminders: List<Reminder>)
}
