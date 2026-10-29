package com.example.todoapp.db.sample

/**
 * Dev-only knobs for ScreenshotSample, one scene per fresh install. Each platform's debug build sets
 * them from launch arguments before the app starts (see scripts/shot-ios.sh).
 */
object ShotOptions {

    /** What October's calendar shows: pies and moons only, then milestone flags too, then one cake too. */
    enum class Stage { PIES, FLAGS, CAKE }

    /** How far today has got: the morning card, a few ticks, or nearly done. REST makes today a Recharging day. */
    enum class Today { MORNING, MIDDAY, EVENING, REST }

    /** Which pace-offer stickies today's diary shows. */
    enum class Offer { NONE, LIGHTER, STEPUP, BOTH }

    var stage = Stage.FLAGS
    var today = Today.MIDDAY
    var offer = Offer.NONE
    /** Today is the first day opened this week, so the diary shows last week's card and what the app moved. */
    var weekNotes = false
    /** Learn Spanish's last milestone is due today. */
    var milestoneDue = false

    /** Reads one launch argument by name; [get] returns null for an argument that wasn't passed. */
    fun read(get: (String) -> String?) {
        fun <T : Enum<T>> pick(key: String, entries: List<T>, default: T): T =
            get(key)?.let { value -> entries.firstOrNull { it.name.equals(value, ignoreCase = true) } } ?: default
        fun flag(key: String): Boolean = get(key)?.lowercase() in setOf("yes", "true", "1")
        stage = pick("shotStage", Stage.entries, stage)
        today = pick("shotToday", Today.entries, today)
        offer = pick("shotOffer", Offer.entries, offer)
        weekNotes = flag("shotWeekNotes")
        milestoneDue = flag("shotMilestoneDue")
    }
}
