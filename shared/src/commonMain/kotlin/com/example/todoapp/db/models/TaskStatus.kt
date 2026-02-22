package com.example.todoapp.db.models

enum class TaskStatus(val value: String) {
    COMPLETED("COMPLETED"),
    IN_PROGRESS("IN_PROGRESS"),
    ADDED("ADDED");
}

enum class TaskType(val value: String) {
    TOP3("TOP3"),
    CHECK("CHECK"),
    AG_RANDOM("AG_RANDOM"),
    TODO("TODO");
}

enum class PillarName(val value: String) {
    HEALTH("HEALTH"),
    WEALTH("WEALTH"),
    LOVE("LOVE"),
    LIFE("LIFE");
}

enum class ImportanceLevel(val value: String, val numericValue: Float) {
    LOW("LOW", 0.25f),
    MEDIUM("MEDIUM", 0.50f),
    HIGH("HIGH", 0.70f);
}

enum class FrequencyPeriod(val value: String) {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY");
}
