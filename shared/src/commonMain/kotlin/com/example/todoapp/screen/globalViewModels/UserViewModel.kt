package com.example.todoapp.screen.globalViewModels


import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.db.data.user.User
import com.example.todoapp.repo.UserRepository

import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _userId = MutableStateFlow<Long?>(null)
    val userId: StateFlow<Long?> = _userId

    suspend fun loadUserByIdOnce(userId: Long) : User?{
        _userId.value = userId
        _user.value = userRepository.getUserValueByUserId(userId)
        UserInitManager.setUserDetails(userId, _user.value?.username?:"gggg") // Update UserManager
        return _user.value
    }

    fun loadUser() {
        viewModelScope.launch {
            try {
                _user.value = _userId.value?.let { userRepository.getUserValueByUserId(it) }
            }
            catch (e: Exception) {
            }
        }
    }

    fun clearUser() {
        _user.value = null
        _userId.value = null
    }



    fun updateUser(user: User) {
        _user.value = user
    }
}
