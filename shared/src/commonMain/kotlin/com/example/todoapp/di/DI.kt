package com.example.todoapp.di

import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.LoginRepository
import com.example.todoapp.repo.MilestoneRepository
import com.example.todoapp.repo.MissionRepository
import com.example.todoapp.repo.TodayTaskRepository
import com.example.todoapp.repo.UserRepository
import com.example.todoapp.screen.globalViewModels.UserViewModel
import com.example.todoapp.screen.init.InitViewModel
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import com.example.todoapp.screen.missions.MissionItemViewModel
import com.example.todoapp.screen.missions.MissionsControllerViewModel
import com.example.todoapp.screen.missions.calendar.progress.DayMissionProgressViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

expect fun platformModule(): Module

fun vmModule(): Module {
    return module {
        // Repositories
        single { UserRepository(get()) }
        single { LoginRepository(get()) }
        single { MissionRepository(get()) }
        single { MilestoneRepository(get()) }
        single { TodayTaskRepository(get()) }

        // ViewModels base
        factory { UserViewModel(get()) }
        single { InitViewModel(get(), get(), get()) }

        // New ViewModels
        factory { MissionsControllerViewModel(get()) }
        factory { (missionId: Long) -> MissionItemViewModel(get(), missionId) } // for dynamic MissionItemViewModel

        factory { (day: MyDate, missionId: Long) -> DayMissionProgressViewModel(get(), day, missionId) }

        factory { CalDiaryViewModel(get()) }

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