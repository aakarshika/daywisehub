package com.example.todoapp.screen.diary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.water.WaterIntake
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.diary.diaryitem.components.NotesItem
import com.example.todoapp.screen.diary.diaryitem.components.dateheader.DateHeader
import com.example.todoapp.screen.diary.diaryitem.components.diarymood.MoodHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.WaterIntakeHeader
import com.example.todoapp.screen.diary.diaryitem.components.waterintake.water_daily_limit
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.block_b
import todoapp.shared.generated.resources.pencil

fun LazyListScope.MoodHeaderItem(todayMoodList: List<TodayMoodWithDetails>?,
                                 selection: LocalDate,
                                 upsertMood : (TodayMood) -> Unit) {
    item(key = "mood") {
        MoodHeader(
            todayMoodList,
            selection,
            upsertMood
        )
    }
}

fun LazyListScope.DateHeaderItem(cDate: LocalDate,
                                 changeDate: (LocalDate) -> Unit) {
    item(key = "date-header") {
        DateHeader(cDate, changeDate = { changeDate ( it ) })
    }
}

fun LazyListScope.WaterIntakeHeaderItem(waterIntake: WaterIntake?,
                                        selection: LocalDate,
                                        upsertWaterIntake : (WaterIntake) -> Unit,
                                        waterIntakeCompleted : (Boolean) -> Unit) {
    item(key = "water") {
        WaterIntakeHeader(waterIntake?.waterIntakeProgress?:0.0, selection, upsertWaterIntake = {progress->
            upsertWaterIntake((waterIntake ?: WaterIntake(
                waterIntakeId = 0,
                waterIntakeProgress = progress,
                waterDate = MyDate.fromLocalDate(selection)
            )).copy(waterIntakeProgress = progress))
            waterIntakeCompleted(progress.toInt() == (water_daily_limit-1))
        })
    }
}

fun LazyListScope.Notes(waterIntake: WaterIntake?,
                        selection: LocalDate,
                        upsertWaterIntake : (WaterIntake) -> Unit) {
    item(key = "notes") {
        val rowHeight = remember { mutableStateOf(52) }

        // The notes keep a sideways drag to themselves rather than turning the diary's page.
        Box(Modifier.pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume() } }) {
            NotesItem(rowHeight.value,waterIntake, selection,textArranged = { lineCount ->
                rowHeight.value = 26*(lineCount+1)
            }, upsertWaterIntake = {n:String->
                upsertWaterIntake((waterIntake ?: WaterIntake(
                    waterIntakeId = 0,
                    notes = n,
                    waterDate = MyDate.fromLocalDate(selection)
                )).copy(notes = n))
            })
        }
    }
}

fun LazyListScope.ToDoListHeader(headingtext: String, color: Color, addLabel: String, editClicked: () -> Unit) {
    item(key = "heading-$headingtext") {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { HeadingBanner(color, headingtext) }
            // A 44dp target around the 31dp pencil, lifted so the pencil sits about where it did.
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 13.5.dp).size(44.dp)
                    .clickable(role = Role.Button) { editClicked() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.shadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(color)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(Res.drawable.pencil),
                        contentDescription = addLabel,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp).padding(start = 2.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

/** A section heading. Keyed by its text, so each one on a screen needs its own. */
fun LazyListScope.HeadingText(headingtext: String, color: Color) {
    item(key = "heading-$headingtext") {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            HeadingBanner(color, headingtext)
        }
    }
}

@Composable
private fun HeadingBanner(color: Color, headingtext: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.height(30.dp).fillMaxWidth()
                .padding(horizontal = 60.dp)
        ) {
            Image(
                painterResource(Res.drawable.block_b),
                contentDescription = "Section heading background",
                colorFilter = ColorFilter.tint(color),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
            )
            Text(
                text = headingtext,
                modifier = Modifier
                    .align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
