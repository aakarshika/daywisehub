package com.example.todoapp.screen.diary.diaryitem.components.diarymood
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.screen.basicutils.components.WriteText
import com.example.todoapp.screen.diary.diaryitem.components.Blue80
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.happy
import todoapp.shared.generated.resources.sad
import todoapp.shared.generated.resources.angry
import todoapp.shared.generated.resources.afraid
import todoapp.shared.generated.resources.surprised
import todoapp.shared.generated.resources.disgusted


val Red80 = Color(0xFFFFCBD2)

private fun getDrawableIdFromMoodIcon(moodIcon: String): DrawableResource {
    val drawableMap = mapOf(
        "happy" to Res.drawable.happy,
        "sad" to Res.drawable.sad,
        "angry" to Res.drawable.angry,
        "afraid" to Res.drawable.afraid,
        "surprised" to Res.drawable.surprised,
        "disgusted" to Res.drawable.disgusted
    )
    return drawableMap[moodIcon] ?: Res.drawable.happy
}

@Composable
fun MoodHeader(
    todayMoodList: List<TodayMoodWithDetails>?,
    selection: LocalDate,
    upsertMood : (TodayMood) -> Unit
) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                    .padding(start = 10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(modifier = Modifier.align(Alignment.Center)) {
                        items(todayMoodList ?: listOf()) {
                            val status = it.todayMood?.tmMoodStatus
                            Logger.e("Mood status ${status}")
                            Icon(
                                tint = if (status == "ACTIVE") Red80 else Color.LightGray,
                                painter = painterResource(
                                    getDrawableIdFromMoodIcon(it.mood.moodIcon)
                                ),
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(3.dp)
                                    .clickable {
                                        val newMood = (it.todayMood ?: TodayMood(
                                            tmId = 0L,
                                            tmDate = MyDate.fromLocalDate(selection),
                                            tmMoodId = it.mood.moodId,
                                            tmMoodStatus = "NEW"
                                        )).copy(
                                            tmMoodStatus = if (status == "ACTIVE") "INACTIVE" else "ACTIVE"
                                        )
                                        Logger.i("Mood upserting ${status} ${newMood.toString()}")
                                        upsertMood(newMood)
                                    },
                                contentDescription = "mood-icon-smile"
                            )
                        }
                    }
                }
            }
        }
    }
}
