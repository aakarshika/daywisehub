package com.example.todoapp.domain

/** Spec section 7. Lines are built only from stored data, never from what a title means. */
enum class ReasonCode {
    STREAK, QUOTA, RHYTHM_DUE, RHYTHM_FIRST, RHYTHM_OVERDUE, MOVED, MILESTONE,
    RECHARGE_HEAVY, RECHARGE_USUAL, BLOCK, PILLAR_LIGHT, ADDED
}

data class Reason(val code: ReasonCode, val args: List<String> = emptyList()) {

    fun encodeArgs(): String = args.joinToString(SEPARATOR)

    companion object {
        const val SEPARATOR = "|"

        fun decode(code: String, args: String): Reason =
            Reason(ReasonCode.valueOf(code), if (args.isEmpty()) emptyList() else args.split(SEPARATOR))
    }
}

object ReasonLines {

    fun render(reason: Reason): String {
        val a = reason.args
        return when (reason.code) {
            ReasonCode.STREAK ->
                if (a[0].toInt() > 0) "Every day · ${a[0]}-day streak" else "Every day"
            ReasonCode.QUOTA ->
                "${a[0]} of ${a[1]} this week · " + if (a[2] == "1") "last day" else "${a[2]} days left"
            ReasonCode.RHYTHM_DUE ->
                (if (a[0] == "1") "Last done yesterday" else "Last done ${a[0]} days ago") + " · you aim for every ${a[1]}"
            ReasonCode.RHYTHM_FIRST -> "First one · you aim for every ${a[0]}"
            ReasonCode.RHYTHM_OVERDUE -> if (a[0] == "1") "Due yesterday" else "Due ${a[0]} days ago"
            ReasonCode.MOVED -> "Moved from ${a[0]}"
            ReasonCode.MILESTONE -> "Milestone ${a[0]} of ${a[1]} · due ${a[2]}"
            ReasonCode.RECHARGE_HEAVY -> "${a[0]} hours of fixed blocks today. This counts too."
            ReasonCode.RECHARGE_USUAL -> "You usually take ${a[0]} breaks a week."
            ReasonCode.BLOCK -> "${a[0]} to ${a[1]}"
            ReasonCode.PILLAR_LIGHT -> "${a[0]} is light this week"
            ReasonCode.ADDED -> "Added by you"
        }
    }
}
