package com.example.todoapp.screen.diary.diaryitem.components.diarymood
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.todoapp.db.data.mood.TodayMood
import com.example.todoapp.db.data.mood.TodayMoodWithDetails
import com.example.todoapp.db.models.MyDate
import com.example.todoapp.domain.DayBadges
import com.example.todoapp.screen.metrics.badges.BadgeSticker
import com.example.todoapp.screen.metrics.badges.badgeLines
import com.example.todoapp.screen.basicutils.Red180
import com.example.todoapp.screen.basicutils.SketchInk
import com.example.todoapp.screen.basicutils.Red80
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import todoapp.shared.generated.resources.Res
import todoapp.shared.generated.resources.happy
import todoapp.shared.generated.resources.sad
import todoapp.shared.generated.resources.angryemoji
import todoapp.shared.generated.resources.boredemoji
import todoapp.shared.generated.resources.dreamyemoji
import todoapp.shared.generated.resources.happyemoji
import todoapp.shared.generated.resources.sademoji


private fun getDrawableIdFromMoodIcon(moodIcon: String): DrawableResource {
    val drawableMap = mapOf(
        "happy" to Res.drawable.happyemoji,
        "sad" to Res.drawable.sademoji,
        "angry" to Res.drawable.angryemoji,
        "dreamy" to Res.drawable.dreamyemoji,
        "bored" to Res.drawable.boredemoji
    )
    return drawableMap[moodIcon] ?: Res.drawable.sad
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
                        items(todayMoodList ?: listOf(), key = { it.mood.moodId }) {
                            val status = it.todayMood?.tmMoodStatus
                            val active = status == "ACTIVE"
                            val sticker = DayBadges.moodBadge(it.mood.moodIcon)
                            // A picked mood shows as its sticker, popping in when tapped rather than when the page opens.
                            var tapped by remember { mutableStateOf(false) }
                            val pop = remember { Animatable(1f) }
                            LaunchedEffect(active) {
                                if (active && tapped) {
                                    pop.snapTo(0.3f)
                                    pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
                                }
                            }
                            val modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    tapped = true
                                    val newMood = (it.todayMood ?: TodayMood(
                                        tmId = 0L,
                                        tmDate = MyDate.fromLocalDate(selection),
                                        tmMoodId = it.mood.moodId,
                                        tmMoodStatus = "NEW"
                                    )).copy(
                                        tmMoodStatus = if (active) "INACTIVE" else "ACTIVE"
                                    )
                                    upsertMood(newMood)
                                }
                            if (active && sticker != null) {
                                BadgeSticker(
                                    sticker, selection, 44.dp,
                                    modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
                                    byRarity = false
                                )
                            } else {
                                if (sticker != null) {
                                    // Not picked: the sticker's own face as a grey line drawing.
                                    Icon(badgeLines(sticker), contentDescription = "mood-icon-smile", modifier = modifier.padding(2.dp), tint = SketchInk)
                                } else {
                                    Icon(
                                        tint = if (active) Red180 else Color.LightGray,
                                        painter = painterResource(getDrawableIdFromMoodIcon(it.mood.moodIcon)),
                                        modifier = modifier.padding(5.dp),
                                        contentDescription = "mood-icon-smile"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
