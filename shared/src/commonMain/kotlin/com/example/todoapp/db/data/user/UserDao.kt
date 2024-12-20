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
   suspend fun getUserByUserId(userId: Long): User?

    @Query("SELECT * FROM users WHERE id = :userId")
   suspend fun getUserValueByUserId(userId: Long): User?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun getUser(username: String, password: String):  User?

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUser(username: String)
}
