package com.example.todoapp.db.models

/** Status values stored on milestones. Daily tasks use domain.TaskState instead. */
enum class TaskStatus(val value: String) {
    COMPLETED("COMPLETED"),
    IN_PROGRESS("IN_PROGRESS");
}
