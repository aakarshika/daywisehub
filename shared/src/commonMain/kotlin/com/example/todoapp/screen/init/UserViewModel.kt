package com.example.todoapp.screen.init


import androidx.lifecycle.ViewModel
import com.example.todoapp.db.data.user.User
import com.example.todoapp.di.KoinF
import com.example.todoapp.repo.UserRepository
import com.example.todoapp.screen.metrics.CalDiaryViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.SharedFlow


class UserViewModel  constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableSharedFlow<User?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val user: SharedFlow<User?> get() = _user


    suspend fun loadUserByIdOnce(userId: Long){
        userRepository.getUserValueByUserId(userId)
            .collect { it ->
                val calDiaryVm = KoinF.di?.get<CalDiaryViewModel>()
                calDiaryVm?.userId = userId
                calDiaryVm?.username = it?.username ?: "gggg"
                _user.tryEmit(it)
            }
    }

}
