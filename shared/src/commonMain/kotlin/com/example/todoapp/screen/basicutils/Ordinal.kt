package com.example.todoapp.screen.basicutils

/** 1 → "1st", 2 → "2nd", 11 → "11th", 23 → "23rd". */
fun getOrdinal(index: Int): String {
    return when {
        index % 100 in 11..13 -> "${index}th"
        index % 10 == 1 -> "${index}st"
        index % 10 == 2 -> "${index}nd"
        index % 10 == 3 -> "${index}rd"
        else -> "${index}th"
    }
}
