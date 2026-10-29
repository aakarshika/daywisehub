package com.example.todoapp.screen.diary.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.NoteInk
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.diary.PillarProgress

/**
 * The page's week under the date: each pillar's friend fills up as its points come in, with its name and
 * points against the target beside it. [label] names the week ("THIS WEEK", "LAST WEEK", …); [freshStart]
 * adds "A FRESH START", on today's page only. The label's row ends with a tiny see-saw and which pillar
 * needs you, or "In balance" while every pillar is about as far along as the others.
 */
@Composable
fun PillarWeekStrip(pillars: List<PillarProgress>, label: String, freshStart: Boolean = false) {
    // An empty install has no pillars yet: no label over nothing.
    if (pillars.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (freshStart) "$label · A FRESH START" else label,
                fontSize = 11.sp, letterSpacing = 1.2.sp, color = InkGray, modifier = Modifier.weight(1f)
            )
            Verdict(pillars)
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pillars.forEach { progress ->
                Row(
                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A pillar with nothing due this week stays small.
                    PillarFriend(
                        progress.pillar, progress.share, extra = progress.extra > 0,
                        grown = if (progress.target == 0) 0f else progress.share
                    )
                    Spacer(Modifier.width(3.dp))
                    Column {
                        Text(progress.pillar.title, fontSize = 12.sp, color = Color(0xFF4A4A4A), maxLines = 1, softWrap = false)
                        Text("${progress.earned}/${progress.target}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NoteInk, maxLines = 1)
                        if (progress.extra > 0) {
                            Text("+${progress.extra} extra", fontSize = 11.sp, color = pillarInk(progress.pillar), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The see-saw dips toward the side with more still to do: Health and Wealth on the left, Love and Life
 * on the right. The words name the pillar furthest behind, unless all are within 15% of each other; the
 * see-saw sits level then, so it never tips while the words say "In balance".
 */
@Composable
private fun Verdict(pillars: List<PillarProgress>) {
    val balance = balanceOf(pillars) ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniSeeSaw(if (balance.balanced) 0f else balance.tilt)
        Spacer(Modifier.width(4.dp))
        Text(
            balance.words,
            fontSize = 12.sp, color = if (balance.balanced) InkGray else pillarInk(balance.lowest)
        )
    }
}

/** The verdict: the see-saw's [tilt], the pillar furthest behind, and whether all are about level. */
data class Balance(val tilt: Float, val lowest: PillarType, val balanced: Boolean) {
    val words: String get() = if (balanced) "In balance" else "${lowest.title} needs you"
}

/** [Verdict]'s rule, for the strip and the widgets. Null without pillars. */
fun balanceOf(pillars: List<PillarProgress>): Balance? {
    if (pillars.isEmpty()) return null
    fun left(p: PillarProgress) = p.pillar == PillarType.HEALTH || p.pillar == PillarType.WEALTH
    val toDo = { side: Boolean -> pillars.filter { left(it) == side }.sumOf { (it.target - it.earned).coerceAtLeast(0) } }
    val tilt = ((toDo(false) - toDo(true)) * 3f).coerceIn(-8f, 8f)
    val lowest = pillars.minBy { it.share }
    return Balance(tilt, lowest.pillar, balanced = pillars.maxOf { it.share } - lowest.share < 0.15f)
}
