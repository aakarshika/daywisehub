package com.example.todoapp.domain

import kotlinx.datetime.LocalDate

/**
 * Spec section 7. Lines are built only from stored data, never from what a title means.
 * Code names are stored in the database: add new codes, but never rename or remove one.
 */
enum class ReasonCode {
    STREAK, QUOTA, RHYTHM_DUE, RHYTHM_FIRST, RHYTHM_OVERDUE, MOVED, MILESTONE,
    RECHARGE_HEAVY, RECHARGE_USUAL, BLOCK, PILLAR_LIGHT, ADDED
}

data class Reason(val code: ReasonCode, val args: List<String> = emptyList()) {

    /** Args are joined with [SEPARATOR]; a separator inside an arg becomes a space. */
    fun encodeArgs(): String = args.joinToString(SEPARATOR) { it.replace(SEPARATOR, " ") }

    companion object {
        const val SEPARATOR = "|"

        /** Null for a code this version of the app doesn't know. */
        fun decode(code: String, args: String): Reason? {
            val reasonCode = ReasonCode.entries.firstOrNull { it.name == code } ?: return null
            return Reason(reasonCode, if (args.isEmpty()) emptyList() else args.split(SEPARATOR))
        }
    }
}

object ReasonLines {

    /** Renders a stored reason. An unreadable row gives an empty line instead of crashing the diary. */
    fun render(code: String, args: String, on: LocalDate? = null): String =
        Reason.decode(code, args)?.let { render(it, on) }.orEmpty()

    /**
     * [on] is the day of the item the reason belongs to, when there is one. A move that lands on the
     * weekday it came from reads "Moved from last Tuesday", so it isn't taken for the item's own day.
     */
    fun render(reason: Reason, on: LocalDate? = null): String {
        fun arg(i: Int): String = reason.args.getOrNull(i).orEmpty()
        return when (reason.code) {
            ReasonCode.STREAK -> {
                val days = arg(0).toIntOrNull() ?: 0
                if (days > 0) "Every day · $days-day streak" else "Every day"
            }
            ReasonCode.QUOTA ->
                "${arg(0)} of ${arg(1)} this week · " + if (arg(2) == "1") "last day" else "${arg(2)} days left"
            ReasonCode.RHYTHM_DUE ->
                (if (arg(0) == "1") "Last done yesterday" else "Last done ${arg(0)} days ago") + " · you aim for every ${arg(1)}"
            ReasonCode.RHYTHM_FIRST -> "First one · you aim for every ${arg(0)}"
            ReasonCode.RHYTHM_OVERDUE -> if (arg(0) == "1") "Due yesterday" else "Due ${arg(0)} days ago"
            ReasonCode.MOVED ->
                if (on != null && arg(0) == on.weekdayName()) "Moved from last ${arg(0)}" else "Moved from ${arg(0)}"
            ReasonCode.MILESTONE -> "Milestone ${arg(0)} of ${arg(1)} · due ${arg(2)}"
            ReasonCode.RECHARGE_HEAVY -> "${arg(0)} hours of fixed blocks today. This counts too."
            ReasonCode.RECHARGE_USUAL ->
                if (arg(0) == "1") "You usually take one break a week." else "You usually take ${arg(0)} breaks a week."
            ReasonCode.BLOCK -> if (arg(0).isBlank() || arg(1).isBlank()) "Fixed block" else "${arg(0)} to ${arg(1)}"
            ReasonCode.PILLAR_LIGHT -> "${arg(0)} is light this week"
            ReasonCode.ADDED -> "Added by you"
        }
    }
}
