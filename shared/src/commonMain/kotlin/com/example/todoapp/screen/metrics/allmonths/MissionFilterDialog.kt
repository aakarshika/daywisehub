package com.example.todoapp.screen.metrics.allmonths

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.pillarInk
import com.example.todoapp.screen.basicutils.pillarStrong
import com.example.todoapp.screen.diary.plan.SmallChip
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.check_square
import todoapp.shared.generated.resources.square_b

/**
 * Spec 9.4: every mission, archived ones too, under its pillar's heading in [choices]' order, each a 44dp
 * checkbox row. [picked] is ticked to begin with. "Clear" unticks every row, "Done" hands the ticked ones
 * to [onDone], and dismissing it changes nothing.
 */
@Composable
fun MissionFilterDialog(choices: List<MissionChoice>, picked: Set<Long>, onDone: (Set<Long>) -> Unit, onDismiss: () -> Unit) {
    var ticked by remember { mutableStateOf(picked) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { SmallChip("Done", onClick = { onDone(ticked) }) },
        dismissButton = { SmallChip("Clear", onClick = { ticked = emptySet() }) },
        title = { Text("Missions", modifier = Modifier.semantics { heading() }) },
        text = {
            if (choices.isEmpty()) {
                Text("Missions you add can be picked here.", fontSize = 15.sp, fontStyle = FontStyle.Italic, color = InkGray)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    // groupBy keeps the choices' order: pillars in order, and each one's missions as they came.
                    choices.groupBy { it.pillar }.forEach { (pillar, group) ->
                        item(key = "pillar-${pillar.name}") { PillarHeading(pillar) }
                        items(group, key = { it.missionId }) { choice ->
                            ChoiceRow(choice, checked = choice.missionId in ticked) { on ->
                                ticked = if (on) ticked + choice.missionId else ticked - choice.missionId
                            }
                        }
                    }
                }
            }
        },
        containerColor = Light_Yellowww
    )
}

/** "HEALTH", in its pillar's ink. */
@Composable
private fun PillarHeading(pillar: PillarType) {
    Text(
        pillar.title.uppercase(), fontSize = 11.sp, letterSpacing = 1.2.sp, color = pillarInk(pillar),
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp).semantics { heading() }
    )
}

/** The diary's check box in the pillar's strong color, the title, and "archived" after an archived one. */
@Composable
private fun ChoiceRow(choice: MissionChoice, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(if (checked) Res.drawable.check_square else Res.drawable.square_b),
            contentDescription = null,
            colorFilter = ColorFilter.tint(pillarStrong(choice.pillar)),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(choice.title, fontSize = 16.sp, color = InkBlack, modifier = Modifier.weight(1f, fill = false))
        if (choice.archived) {
            Text("archived", fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
