package com.example.todoapp.reminders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

/** A tapped notification's or widget's day (yyyy-MM-dd), for NavScreens to open the diary on, or the Metrics tab. */
object ReminderLinks {
    const val EXTRA_DATE = "reminder_open_date"
    /** Set to [TAB_METRICS] by the month widget. */
    const val EXTRA_TAB = "open_tab"
    const val TAB_METRICS = "metrics"

    private val _metrics = MutableStateFlow(false)
    val metrics: StateFlow<Boolean> = _metrics

    fun openTab(tab: String?) {
        if (tab == TAB_METRICS) _metrics.value = true
    }

    fun metricsConsumed() {
        _metrics.value = false
    }

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date

    fun open(date: String?) {
        _date.value = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
    }

    fun consumed() {
        _date.value = null
    }
}
