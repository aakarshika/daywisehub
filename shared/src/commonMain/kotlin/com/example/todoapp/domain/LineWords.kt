package com.example.todoapp.domain

import com.example.todoapp.db.data.mission.Mission

// Pieces the app's lines are built from (spec 7.1): plain words, and titles on chips in their pillar's color.

/** A line of plain words. */
internal fun words(text: String): List<LinePart> = listOf(LinePart(text))

/** A mission's title on a chip. */
internal fun chip(mission: Mission): LinePart = LinePart(mission.title, mission.pillarType)

/** A pillar's name on a chip. */
internal fun chip(pillar: PillarType): LinePart = LinePart(pillar.title, pillar)

/** [parts] as a list reads: "⟨A⟩", "⟨A⟩ and ⟨B⟩", "⟨A⟩, ⟨B⟩ and ⟨C⟩". */
internal fun joined(parts: List<LinePart>): List<LinePart> {
    val out = mutableListOf<LinePart>()
    parts.forEachIndexed { i, part ->
        if (i > 0) out += LinePart(if (i == parts.lastIndex) " and " else ", ")
        out += part
    }
    return out
}

/** Plain names the same way: "Health", "Health and Love", "Health, Wealth, Love and Life". */
internal fun joinedNames(names: List<String>): String =
    if (names.size <= 1) names.joinToString() else names.dropLast(1).joinToString(", ") + " and " + names.last()
