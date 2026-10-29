package com.example.todoapp.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The inline field of an idea whose title has a `{slot}` (spec 8.1). */
data class IdeaBlank(val label: String, val examples: List<String>)

data class IdeaTheme(val id: String, val name: String)

/** One entry of the bundled ideas library (spec 8.1), with its kind and pace already in the app's terms. */
data class Idea(
    val id: String,
    /** As written, possibly with one `{slot}`. */
    val title: String,
    val blank: IdeaBlank?,
    val theme: String,
    val pillar: PillarType,
    val kind: MissionKind,
    val count: Int = 1,
    val goalCount: Int? = null,
    val period: Period = Period.WEEK,
    val intervalCount: Int = 0,
    val intervalUnit: Period = Period.WEEK,
    val weekDays: Set<Int> = emptySet(),
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val durationMinutes: Int,
    val effort: Effort,
    val weight: Int,
    val finishWeeks: Int? = null,
    val milestones: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    /** Labelled keyword sets: "relative" → uncle, aunt… */
    val groups: Map<String, List<String>> = emptyMap(),
) {
    /** The title with its blank shown as "…". */
    val displayTitle: String get() = title.replace(SLOT, "…")

    /** The title with its blank filled in. */
    fun filled(text: String): String = title.replace(SLOT, text.trim())

    /** Every word that finds this idea: title words, keywords, group words and group labels. */
    val words: Set<String> by lazy {
        (Ideas.words(title.replace("{", "").replace("}", "")) + keywords + groups.keys + groups.values.flatten())
            .map { it.lowercase() }.toSet()
    }

    /**
     * A new mission's draft from this idea. [title] defaults to the idea's own; rhythms longer than
     * the editor allows (6, 12 and 24 months) are capped at 3 months.
     */
    fun toDraft(today: LocalDate, title: String = displayTitle): MissionDraft {
        val gentle = goalCount != null && goalCount > count
        return MissionDrafts.new(today).copy(
            title = title.take(MissionDrafts.MAX_TITLE_LENGTH),
            pillarChoice = pillar.takeIf { it.scored },
            kind = kind,
            count = if (gentle) goalCount!! else count.coerceAtLeast(1),
            // Finish lines are weekly.
            period = if (kind == MissionKind.FINISH_LINE) Period.WEEK else period,
            gentle = gentle,
            gentleCount = if (gentle) count else null,
            intervalCount = if (kind == MissionKind.RHYTHM) intervalCount.coerceIn(MissionDrafts.intervalRange(intervalUnit)) else 2,
            intervalUnit = intervalUnit,
            weekDays = weekDays,
            startMinute = startMinute ?: (9 * 60),
            endMinute = endMinute ?: (12 * 60),
            endDate = if (kind == MissionKind.FINISH_LINE) {
                today.plus((finishWeeks ?: MissionDrafts.DEFAULT_FINISH_WEEKS) * 7, DateTimeUnit.DAY)
            } else null,
            milestones = if (kind == MissionKind.FINISH_LINE) {
                milestones.take(MissionDrafts.MAX_MILESTONES).map { MilestoneDraft(text = it) }
            } else emptyList(),
            durationMinutes = durationMinutes,
            effort = effort,
            weight = weight.coerceIn(1, 3),
        )
    }

    companion object {
        private val SLOT = Regex("\\{[^}]*\\}")
    }
}

/** The whole library: themes in display order, and the ideas. */
data class IdeaLibrary(val themes: List<IdeaTheme>, val ideas: List<Idea>) {
    /** Every word any idea is found by; a word outside it may still be half-typed. */
    val allWords: Set<String> by lazy { ideas.flatMapTo(mutableSetOf()) { it.words } }
}

/** One suggestion while typing a title (spec 8.3). [reason] explains a match through a group. */
data class IdeaMatch(val idea: Idea, val score: Int, val reason: String?, val best: Boolean = false)

object Ideas {

    private val stopWords = setOf(
        "a", "an", "the", "my", "your", "to", "for", "with", "and", "of", "in", "on", "at", "go", "do", "get",
        "have", "take", "some", "more", "i", "want", "every", "day",
    )

    /** Lowercase words, split on anything that isn't a letter, digit, apostrophe or hyphen. */
    fun words(text: String): List<String> =
        text.lowercase().split(Regex("[^\\p{L}\\p{N}'-]+")).filter { it.isNotBlank() }

    /**
     * Spec 8.3: 2 points for each typed word an idea is found by. While the last word is still being
     * typed (no space after it) and matches nothing in the library, it scores 1 where it starts a word.
     */
    fun match(input: String, library: IdeaLibrary, limit: Int = 4): List<IdeaMatch> {
        val typed = words(input).filter { it !in stopWords }
        if (typed.isEmpty()) return emptyList()
        val last = typed.last()
        val prefix = !input.last().isWhitespace() && last !in library.allWords
        val scored = library.ideas.mapNotNull { idea ->
            var score = 0
            var reason: String? = null
            typed.forEach { word ->
                if (word in idea.words) {
                    score += 2
                    if (reason == null) {
                        idea.groups.entries.firstOrNull { (label, members) -> word != label && word in members }?.let { (label, _) ->
                            reason = "${word.replaceFirstChar { it.uppercase() }} counts as ${article(label)} $label"
                        }
                    }
                }
            }
            if (prefix && idea.words.any { it.startsWith(last) }) score += 1
            if (score > 0) IdeaMatch(idea, score, reason) else null
        }.sortedByDescending { it.score }.take(limit)
        if (scored.isEmpty()) return scored
        val best = scored.size == 1 || scored[0].score - scored[1].score >= 2
        return if (best) listOf(scored[0].copy(best = true)) + scored.drop(1) else scored
    }

    private fun article(word: String) = if (word.first() in "aeiou") "an" else "a"

    /** "Every month · 20 min · small", "3× a week · 30 min · steady". */
    fun preset(idea: Idea): String {
        val pace = when (idea.kind) {
            MissionKind.BLOCK -> {
                val start = idea.startMinute?.let { MissionWords.clockLabel(it) }
                val end = idea.endMinute?.let { MissionWords.clockLabel(it) }
                MissionWords.daysLabel(idea.weekDays).replaceFirstChar { it.uppercase() } +
                    if (start != null && end != null) ", $start to $end" else ""
            }
            MissionKind.RHYTHM -> MissionWords.intervalPhrase(
                idea.intervalCount.coerceIn(MissionDrafts.intervalRange(idea.intervalUnit)), idea.intervalUnit
            ).replaceFirstChar { it.uppercase() }
            MissionKind.RECHARGE -> "About ${MissionWords.times(idea.count)} a ${idea.period.name.lowercase()}"
            else -> when (idea.period) {
                Period.DAY -> "Every day"
                Period.WEEK -> if (idea.count == 1) "Once a week" else "${idea.count}× a week"
                Period.MONTH -> if (idea.count == 1) "Once a month" else "${idea.count}× a month"
            } + if (idea.kind == MissionKind.FINISH_LINE) " for ${idea.finishWeeks ?: MissionDrafts.DEFAULT_FINISH_WEEKS} weeks" else ""
        }
        val parts = mutableListOf(pace)
        if (idea.kind != MissionKind.BLOCK) parts += MissionWords.durationWords(idea.durationMinutes)
        if (idea.kind != MissionKind.RECHARGE) parts += MissionWords.weightWord(idea.weight)
        return parts.joinToString(" · ")
    }

    /** Reads ideas.json. Kinds are renamed to the app's (FIXED_BLOCK → BLOCK, EVERY_SO_OFTEN → RHYTHM). */
    fun parse(json: String): IdeaLibrary {
        val root = Json.parseToJsonElement(json).jsonObject
        val themes = root["themes"]!!.jsonArray.map {
            val o = it.jsonObject
            IdeaTheme(o.str("id")!!, o.str("name")!!)
        }
        val ideas = root["ideas"]!!.jsonArray.mapNotNull { runCatching { idea(it.jsonObject) }.getOrNull() }
        return IdeaLibrary(themes, ideas)
    }

    private fun idea(o: JsonObject): Idea {
        val pace = o["pace"]?.jsonObject ?: JsonObject(emptyMap())
        val kind = when (val k = o.str("kind")) {
            "FIXED_BLOCK" -> MissionKind.BLOCK
            "EVERY_SO_OFTEN" -> MissionKind.RHYTHM
            else -> MissionKind.valueOf(k!!)
        }
        val blank = o["blank"]?.jsonObject?.let { IdeaBlank(it.str("label").orEmpty(), it.strings("examples")) }
        val finish = o["finish"]?.jsonObject
        return Idea(
            id = o.str("id")!!,
            title = o.str("title")!!,
            blank = blank,
            theme = o.str("theme").orEmpty(),
            pillar = PillarType.valueOf(o.str("pillar")!!),
            kind = kind,
            count = pace.int("count") ?: 1,
            goalCount = pace.int("goalCount"),
            period = pace.str("period")?.let { Period.valueOf(it) } ?: Period.WEEK,
            intervalCount = pace.int("intervalCount") ?: 0,
            intervalUnit = pace.str("intervalUnit")?.let { Period.valueOf(it) } ?: Period.WEEK,
            weekDays = pace.strings("weekDays").mapNotNull { isoDays[it.uppercase()] }.toSet(),
            startMinute = MissionWords.parseClock(pace.str("startTime")),
            endMinute = MissionWords.parseClock(pace.str("endTime")),
            durationMinutes = o.int("durationMinutes") ?: 30,
            effort = o.str("effort")?.let { Effort.valueOf(it) } ?: Effort.STEADY,
            weight = o.int("weight") ?: 1,
            finishWeeks = finish?.int("weeks"),
            milestones = finish?.strings("milestones").orEmpty(),
            keywords = o.strings("keywords"),
            groups = o["groups"]?.jsonObject?.mapValues { (_, v) -> v.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull } }.orEmpty(),
        )
    }

    private val isoDays = mapOf("MON" to 1, "TUE" to 2, "WED" to 3, "THU" to 4, "FRI" to 5, "SAT" to 6, "SUN" to 7)

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.strings(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
}
