package com.example.todoapp.screen.basicutils.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A screen with a back arrow: a drag from the left edge slides it right, as on a phone's own screens.
 * Let go past a third of the width, or with a flick, and it slides off and calls [onBack].
 */
fun Modifier.edgeSwipeBack(onBack: () -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onBack)
    val scope = rememberCoroutineScope()
    var offset by remember { mutableFloatStateOf(0f) }
    this
        .pointerInput(Unit) {
            val edge = 28.dp.toPx()
            val slop = viewConfiguration.touchSlop
            awaitEachGesture {
                // The Initial pass sees the drag before a list or a button under the finger does.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x > edge) return@awaitEachGesture
                var dragging = false
                var dx = 0f
                var dy = 0f
                var lastDx = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.positionChange()
                    dx += delta.x
                    dy += delta.y
                    if (!dragging) {
                        if (abs(dy) > slop && abs(dy) > abs(dx)) return@awaitEachGesture
                        if (dx > slop) dragging = true
                    }
                    if (dragging) {
                        change.consume()
                        lastDx = delta.x
                        offset = (offset + delta.x).coerceAtLeast(0f)
                    }
                    if (!change.pressed) break
                }
                if (!dragging) return@awaitEachGesture
                val width = size.width.toFloat()
                scope.launch {
                    if (offset > width / 3f || lastDx > 12.dp.toPx()) {
                        animate(offset, width) { v, _ -> offset = v }
                        latest()
                        offset = 0f
                    } else {
                        animate(offset, 0f) { v, _ -> offset = v }
                    }
                }
            }
        }
        .graphicsLayer { translationX = offset }
}

/**
 * A screen or card with a Close: a drag down slides it down, as a sheet does. On a list it starts once the
 * list is at its top. Let go past a quarter of the height, or with a flick down, and it slides off and calls [onClose].
 */
fun Modifier.swipeDownToClose(onClose: () -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onClose)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var offset by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }

    suspend fun release(velocity: Float) {
        val far = offset > minOf(height / 4f, with(density) { 160.dp.toPx() })
        val flick = velocity > with(density) { 900.dp.toPx() } && offset > 0f
        if (far || flick) {
            animate(offset, height.toFloat()) { v, _ -> offset = v }
            latest()
            offset = 0f
        } else if (offset > 0f) {
            animate(offset, 0f) { v, _ -> offset = v }
        }
    }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Pulled down, an upward drag takes the screen back up before the list scrolls.
                if (source == NestedScrollSource.UserInput && offset > 0f && available.y < 0f) {
                    val used = maxOf(available.y, -offset)
                    offset += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // What the list can't scroll past its top pulls the screen down.
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    offset += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset <= 0f) return Velocity.Zero
                release(available.y)
                return available
            }
        }
    }
    this
        .onSizeChanged { height = it.height }
        .nestedScroll(connection)
        .draggable(
            state = rememberDraggableState { delta -> offset = (offset + delta).coerceAtLeast(0f) },
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> scope.launch { release(velocity) } },
        )
        .graphicsLayer { translationY = offset }
}
