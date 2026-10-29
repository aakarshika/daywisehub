package com.example.todoapp.di

import com.example.todoapp.db.backup.BackupRepository
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.reminders.ReminderSync
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.ReminderRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.DayLogRepository
import com.example.todoapp.repo.WaterIntakeRepository
import com.example.todoapp.repo.WeekRepository
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.init.InitViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.metrics.MetricsViewModel
import com.example.todoapp.screen.metrics.allmonths.AllMonthsViewModel
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.EditorTarget
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
import com.example.todoapp.widget.WidgetData
import com.example.todoapp.widget.WidgetSync
import kotlinx.datetime.LocalDate
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun platformModule(): Module

fun vmModule(): Module {
    return module {
        // Repositories
        single { MissionRepository(get()) }
        single { MilestoneRepository(get()) }
        single { TodayTaskRepository(get()) }
        single { TodayMoodRepository(get()) }
        single { WaterIntakeRepository(get()) }
        single { WeekRepository(get()) }
        single { DayLogRepository(get()) }
        single { PlanRepository(get(), get(), get(), get(), get()) }
        single { ReminderRepository(get()) }
        single { BackupRepository(get()) }
        single { ReminderSync(get(), get(), get(), get(), get()) }
        single { WidgetData(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
        single { WidgetSync(get(), get(), get(), get()) }

        // ViewModels
        single { InitViewModel(get(), get(), get(), get(), get()) }
        single { CalDiaryViewModel() }
        factory { (day: LocalDate) -> DiaryViewModel(get(), get(), get(), get(), get(), get(), day) }
        factory { (day: LocalDate) -> MoodViewModel(get()) }
        factory { (day: LocalDate) -> WaterIntakeViewModel(get()) }
        factory { MissionsViewModel(get(), get(), get(), get(), get(), get()) }
        factory { (target: EditorTarget) ->
            MissionEditorViewModel(get(), get(), get(), get(), target.missionId, target.seed, target.ideaTitle)
        }
        factory { MetricsViewModel(get(), get(), get(), get(), get(), get()) }
        factory { AllMonthsViewModel(get(), get(), get(), get(), get(), get()) }
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(platformModule(), vmModule())
    }

object KoinF {
    var di: Koin? = null

    fun setupKoin(appDeclaration: KoinAppDeclaration = {}) {
        if (di == null) {
            di = initKoin(appDeclaration).koin
        }
    }
}
