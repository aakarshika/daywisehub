package com.example.todoapp.screen.metrics.components
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.data.todotask.TodayTask
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.repo.TodayMoodRepository
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.happiness
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
    selection: LocalDate,
    moodViewModel: MoodViewModel
) {
    val todayMoodList:List<TodayMoodWithDetails>? by moodViewModel.todayMoodStatuses.collectAsState(emptyList())
    LaunchedEffect(selection){
        moodViewModel.loadDailyMoodStatus()
    }


    Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)) {
        Row {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .weight(1f)
                        .padding(2.dp)
                        .background(Red80)
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .weight(1.5f)
                        .padding(0.dp)
                        .background(Red80)
                ){
                    Text(
                        text = "MOOD",
                        modifier = Modifier
                            .align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                        .weight(1f)
                        .padding(2.dp)
                        .background(Red80)
                )
            }
            Column(
                modifier = Modifier.weight(2f)
                    .padding(start = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight().align(Alignment.Start)
                ){
                    todayMoodList?.forEach {
                        Icon(
                            tint = if (it.todayMood?.tmMoodStatus != "ACTIVE") Color.Gray else Color.Red,
                            painter = painterResource(
                                getDrawableIdFromMoodIcon(it.mood.moodIcon)
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterVertically)
                                .padding(3.dp)
                                .clickable {
                                    moodViewModel.upsertTodayMood(
                                        TodayMood(
                                            tmId = it.todayMood?.tmId?:0L,
                                            tmDate = MyDate.fromLocalDate(selection),
                                            tmMoodId = it.mood.moodId,
                                            tmMoodStatus = if (it.todayMood?.tmMoodStatus != "ACTIVE") "ACTIVE" else "INACTIVE"
                                        )
                                    )

                                },
                            contentDescription = "mood-icon-smile"
                        )
                    }
                }
            }
        }
    }
}
