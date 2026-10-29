package com.example.todoapp.domain

/** A piece of the mission sentence. An answer carries its [step]; [placeholder] is the blank being asked now. */
data class SentencePart(val text: String, val step: EditorStep? = null, val placeholder: Boolean = false)

/**
 * "I want to ⟨go for a bike ride⟩. It's ⟨something I do regularly⟩, ⟨about twice a week⟩, ⟨45 mins⟩ each, and it
 * feels ⟨steady⟩. I will get ⟨+3⟩ points toward my ⟨Health⟩. If it goes sideways, ⟨move it⟩."
 */
object MissionSentence {

    /**
     * The answered part of the sentence before [current], ending in a blank for [current].
     * With [current] null (the before-saving screen) it's the whole sentence.
     * Gentle start isn't part of the sentence; it has its own sticky note.
     */
    fun parts(draft: MissionDraft, answered: Set<EditorStep>, current: EditorStep?): List<SentencePart> {
        val out = mutableListOf<SentencePart>()
        for (step in EditorStep.entries) {
            if (step == EditorStep.GENTLE || !draft.applies(step)) continue
            if (step == current) {
                out += SentencePart(lead(draft, step))
                out += SentencePart("", step, placeholder = true)
                return out
            }
            if (step !in answered) return out
            out += SentencePart(lead(draft, step))
            out += answer(draft, step)
        }
        out += SentencePart(".")
        return out
    }

    private fun lead(draft: MissionDraft, step: EditorStep): String = when (step) {
        EditorStep.TITLE -> "I want to "
        EditorStep.KIND -> ". It's "
        EditorStep.PACE -> if (draft.kind == MissionKind.BLOCK && !MissionWords.isEveryDay(draft.weekDays)) ", on " else ", "
        EditorStep.DURATION -> ", "
        EditorStep.EFFORT -> ", and it feels "
        EditorStep.WEIGHT -> ". I will get "
        EditorStep.PILLAR -> " toward my "
        EditorStep.IF_MISSED -> ". If it goes sideways, "
        EditorStep.GENTLE -> ""
    }

    private fun answer(draft: MissionDraft, step: EditorStep): List<SentencePart> = when (step) {
        EditorStep.TITLE -> listOf(SentencePart(draft.title.trim(), step))
        EditorStep.KIND -> listOf(SentencePart(draft.kind?.let { MissionWords.kindPhrase(it) }.orEmpty(), step))
        EditorStep.PACE -> pace(draft)
        EditorStep.DURATION -> listOf(SentencePart(MissionWords.durationWords(draft.durationMinutes), step), SentencePart(" each"))
        EditorStep.EFFORT -> listOf(SentencePart(MissionWords.effortWord(draft.effort), step))
        EditorStep.WEIGHT -> listOf(
            SentencePart("+${draft.weight}", step),
            SentencePart(if (draft.weight == 1) " point" else " points"),
        )
        EditorStep.PILLAR -> listOf(SentencePart(draft.pillar.title, step))
        EditorStep.IF_MISSED -> listOf(SentencePart(MissionWords.ifMissedWords(draft.ifMissed), step))
        EditorStep.GENTLE -> emptyList()
    }

    private fun pace(draft: MissionDraft): List<SentencePart> {
        val step = EditorStep.PACE
        return when (draft.kind) {
            MissionKind.BLOCK -> listOf(
                SentencePart(MissionWords.daysLabel(draft.weekDays), step),
                SentencePart(", "),
                SentencePart("${MissionWords.clockLabel(draft.startMinute)} to ${MissionWords.clockLabel(draft.endMinute)}", step),
            )
            MissionKind.RHYTHM -> listOf(
                SentencePart("about ${MissionWords.intervalPhrase(draft.intervalCount, draft.intervalUnit)}", step)
            )
            MissionKind.RECHARGE -> listOf(SentencePart(MissionWords.countPhrase(draft.count, Period.WEEK), step))
            null -> emptyList()
            else -> buildList {
                add(SentencePart(MissionWords.countPhrase(draft.count, draft.period), step))
                if (draft.usesDays && draft.weekDays.isNotEmpty()) {
                    add(SentencePart(if (MissionWords.isEveryDay(draft.weekDays)) ", " else ", usually on "))
                    add(SentencePart(MissionWords.daysLabel(draft.weekDays), step))
                }
                if (draft.kind == MissionKind.FINISH_LINE) {
                    draft.endDate?.let { end ->
                        add(SentencePart(", until "))
                        add(SentencePart(end.shortLabel(), step))
                    }
                    val milestones = draft.milestones.count { it.text.isNotBlank() }
                    if (milestones > 0) {
                        add(SentencePart(", with "))
                        add(SentencePart(if (milestones == 1) "1 milestone" else "$milestones milestones", step))
                    }
                }
            }
        }
    }
}
