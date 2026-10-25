package com.example.todoapp.domain

/** A piece of the mission sentence. An answer carries its [step]; [placeholder] is the blank being asked now. */
data class SentencePart(val text: String, val step: EditorStep? = null, val placeholder: Boolean = false)

/** Spec 4.1: "I want to ⟨learn guitar⟩ for my ⟨Life⟩. It's ⟨something I keep doing⟩, ⟨about 3 times a week⟩, …" */
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
        EditorStep.PILLAR -> " for my "
        EditorStep.KIND -> ". It's "
        EditorStep.PACE -> if (draft.kind == MissionKind.BLOCK) ", on " else ", "
        EditorStep.DURATION -> ", "
        EditorStep.EFFORT -> ", and it feels "
        EditorStep.WEIGHT -> ". It's a "
        EditorStep.IF_MISSED -> ". If a day goes sideways, "
        EditorStep.GENTLE -> ""
    }

    private fun answer(draft: MissionDraft, step: EditorStep): List<SentencePart> = when (step) {
        EditorStep.TITLE -> listOf(SentencePart(draft.title.trim(), step))
        EditorStep.PILLAR -> listOf(SentencePart(draft.pillar.title, step))
        EditorStep.KIND -> listOf(SentencePart(draft.kind?.let { MissionWords.kindPhrase(it) }.orEmpty(), step))
        EditorStep.PACE -> pace(draft)
        EditorStep.DURATION -> listOf(SentencePart(MissionWords.durationWords(draft.durationMinutes), step), SentencePart(" each"))
        EditorStep.EFFORT -> listOf(SentencePart(MissionWords.effortWord(draft.effort), step))
        EditorStep.WEIGHT -> listOf(
            SentencePart(MissionWords.weightWord(draft.weight), step),
            SentencePart(" part of my ${draft.pillar.title} week"),
        )
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
                    add(SentencePart(", usually on "))
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
