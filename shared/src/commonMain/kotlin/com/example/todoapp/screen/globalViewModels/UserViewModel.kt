package com.example.todoapp.screen.globalViewModels


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.user.User
import com.example.todoapp.repo.UserRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch



object UserInitManager {
    private var userId: Long = -1
    private var username: String = ""

    fun setUserDetails(id: Long, name: String) {
        userId = id
        username = name
    }

    fun getUserId(): Long = userId
    fun getUsername(): String = username
}
class UserViewModel  constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableSharedFlow<User?>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val user: SharedFlow<User?> get() = _user


    suspend fun loadUserByIdOnce(userId: Long){
        userRepository.getUserValueByUserId(userId)
            .collect { it ->
                UserInitManager.setUserDetails(userId, it?.username?:"gggg") // Update UserManager
                _user.tryEmit(it)
            }
    }

}
