package com.example.todoapp.di

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.getDatabase
import com.example.todoapp.reminders.IosReminderScheduler
import com.example.todoapp.reminders.ReminderScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single<AppDatabase> { getDatabase() }
    single<ReminderScheduler> { IosReminderScheduler() }
}
