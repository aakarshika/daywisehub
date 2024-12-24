package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.loginstatus.LoginStatus
import com.example.todoapp.db.data.loginstatus.LoginStatusDao
import kotlinx.coroutines.flow.Flow

class LoginRepository(private val database: AppDatabase) {

    private val loginStatusDao: LoginStatusDao by lazy {
        database.getLoginStatusDao()
    }
    suspend fun insertLoginStatus(loginStatus: LoginStatus): String {
        val loginStatusId = loginStatusDao.insert(loginStatus)
        return "success"
    }
    suspend fun getLoginStatus(): Flow<LoginStatus?> {
        return loginStatusDao.getLoginStatus()
    }
}
