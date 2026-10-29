package com.example.todoapp.screen.init

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todoapp.domain.PillarType
import com.example.todoapp.screen.basicutils.InkBlack
import com.example.todoapp.screen.basicutils.InkGray
import com.example.todoapp.screen.basicutils.Light_Yellowww
import com.example.todoapp.screen.basicutils.components.MarkerHeading
import com.example.todoapp.screen.basicutils.pillarLight
import com.example.todoapp.screen.diary.plan.PlanButton
import com.example.todoapp.screen.diary.plan.StickyNote

/** Shown once, on the first start, before the Missions tab opens on its ideas library. */
@Composable
fun IntroScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Light_Yellowww).statusBarsPadding().navigationBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("Inbalance", fontSize = 34.sp, fontStyle = FontStyle.Italic, letterSpacing = 0.5.sp, color = InkGray)
            Text(
                "Big days, slow days, all in balance.",
                fontSize = 15.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A to-do list that plans around your energy, so every part of life gets its turn.",
                fontSize = 16.sp, lineHeight = 23.sp, color = InkBlack, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            PillarType.scoredPillars.forEach { pillar ->
                MarkerHeading(pillar.title, pillarLight(pillar), Modifier.padding(top = 18.dp))
                Text(
                    pillar.description,
                    fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkGray, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
                )
            }
            StickyNote(rotation = -1.2f, modifier = Modifier.padding(top = 20.dp)) {
                Text(
                    "Start with a mission: one thing you want to make room for. " +
                        "Each morning the diary picks what fits your day, and you tick off what got done.",
                    fontSize = 16.sp, fontStyle = FontStyle.Italic, lineHeight = 23.sp, color = InkBlack
                )
            }
            Spacer(Modifier.height(20.dp))
            PlanButton("Add my first mission", filled = true, onClick = onStart)
            Spacer(Modifier.height(32.dp))
        }
    }
}
