// data/UserDao.kt
package com.example.todoapp.db.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByUserId(userId: Long): Flow<User>

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserValueByUserId(userId: Long): Flow<User>

    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserByUsername(username: String): Flow<User>

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    fun getUser(username: String, password: String):  Flow<User>

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUser(username: String)
}
