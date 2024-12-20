package com.example.todoapp.repo

import com.example.todoapp.db.AppDatabase
import com.example.todoapp.db.data.user.User
import com.example.todoapp.db.data.user.UserDao

class UserRepository(private val database: AppDatabase) {

    private val userDao: UserDao by lazy {
        database.getUserDao()
    }

     suspend fun insertUser(user: User): User {
        val userId =  userDao.insert(user)
        return userDao.getUserValueByUserId(userId)!!
    }

    suspend fun getUserValueByUserId(userId: Long): User? {
        return userDao.getUserValueByUserId(userId)!!
    }
    suspend fun getUserByUserId(userId: Long): User? {
        return userDao.getUserByUserId(userId)
    }

    suspend  fun getUserByUsername(username: String):  User? {
        return userDao.getUserByUsername(username)
    }

    suspend  fun getUserByUsernameAndPassword(username: String, password: String): User? {
        return userDao.getUser(username, password)
    }

     suspend fun deleteUser(username: String) {
        userDao.deleteUser(username)
    }

}