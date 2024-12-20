// data/User.kt
package com.example.todoapp.db.data.loginstatus

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "login_status")
data class LoginStatus(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val isLoggedIn: Boolean = false
)

