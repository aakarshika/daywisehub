package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.user.User
import com.example.todoapp.db.data.user.UserDao
import kotlinx.coroutines.flow.Flow

class UserRepository(private val database: AppDatabase) {

    private val userDao: UserDao by lazy {
        database.getUserDao()
    }

     suspend fun insertUser(user: User): Long {
        return userDao.insert(user)
    }

    suspend fun getUserValueByUserId(userId: Long): Flow<User?> {
        return userDao.getUserValueByUserId(userId)
    }
    suspend fun getUserByUserId(userId: Long): Flow<User> {
        return userDao.getUserByUserId(userId)
    }

    suspend  fun getUserByUsername(username: String):  Flow<User> {
        return userDao.getUserByUsername(username)
    }

    suspend  fun getUserByUsernameAndPassword(username: String, password: String): Flow<User> {
        return userDao.getUser(username, password)
    }

     suspend fun deleteUser(username: String) {
        userDao.deleteUser(username)
    }

}