package com.example.todoapp.db.data.loginstatus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface  LoginStatusDao {
    @Insert
    suspend fun insert(loginStatus: LoginStatus): Long

    @Query("SELECT * FROM login_status")
    fun getLoginStatus(): Flow<LoginStatus?>

}