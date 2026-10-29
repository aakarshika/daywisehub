package com.example.todoapp.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * The app's "now". Dev builds can move it ahead (see DevClockPill) to try later days without waiting;
 * it starts at zero on every launch, so a normal start always reads the real clock.
 * Reminders keep the real clock, since they fire at real times.
 */
object AppClock {

    private val _ahead = MutableStateFlow(Duration.ZERO)
    val ahead: StateFlow<Duration> = _ahead

    fun now(): LocalDateTime = (Clock.System.now() + _ahead.value).toLocalDateTime(TimeZone.currentSystemDefault())

    fun today(): LocalDate = now().date

    fun moveAhead(by: Duration) {
        _ahead.value += by
    }

    /** Dev builds: makes [date] today, at the real time of day. The screenshot scenes start on a fixed day. */
    fun startOn(date: LocalDate) {
        val real = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        _ahead.value = real.daysUntil(date).days
    }

    fun reset() {
        _ahead.value = Duration.ZERO
    }
}
