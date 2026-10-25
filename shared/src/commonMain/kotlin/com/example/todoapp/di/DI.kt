package com.example.todoapp.di

import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.PlanRepository
import com.example.todoapp.repo.TodayMoodRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.WaterIntakeRepository
import com.example.todoapp.screen.diary.DiaryViewModel
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodViewModel
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeViewModel
import com.example.todoapp.screen.init.InitViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.MissionsViewModel
import com.example.todoapp.screen.missions.editor.MissionEditorViewModel
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
        single { PlanRepository(get(), get(), get(), get()) }

        // ViewModels
        single { InitViewModel(get(), get(), get(), get()) }
        single { CalDiaryViewModel() }
        factory { (day: LocalDate) -> DiaryViewModel(get(), get(), get(), get(), day) }
        factory { (day: LocalDate) -> MoodViewModel(get()) }
        factory { (day: LocalDate) -> WaterIntakeViewModel(get()) }
        factory { MissionsViewModel(get(), get(), get(), get()) }
        factory { (missionId: Long) -> MissionEditorViewModel(get(), get(), get(), get(), missionId) }
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
