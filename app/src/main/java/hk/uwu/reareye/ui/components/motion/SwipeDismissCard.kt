package hk.uwu.reareye.ui.components.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * A card wrapper that supports swipe-to-dismiss gesture.
 *
 * Swiping beyond [dismissThreshold] fraction of the width triggers [onDismiss].
 * The card slides back if the swipe is released before the threshold.
 */
@Composable
fun SwipeDismissCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dismissThreshold: Float = 0.4f,
    onDismiss: () -> Unit,
    dismissDirection: SwipeDirection = SwipeDirection.EndToStart,
    backgroundContent: @Composable (progress: Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetX = remember { Animatable(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var isSwiping by remember { mutableStateOf(false) }

    val dismissThresholdPx = containerWidthPx * dismissThreshold
    val progress = if (containerWidthPx > 0f) {
        (offsetX.value.absoluteValue / containerWidthPx).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                containerWidthPx = size.width.toFloat()

                detectHorizontalDragGestures(
                    onDragStart = { isSwiping = true },
                    onDragEnd = {
                        scope.launch {
                            isSwiping = false
                            if (offsetX.value.absoluteValue >= dismissThresholdPx) {
                                val target = if (dismissDirection == SwipeDirection.EndToStart) {
                                    -containerWidthPx
                                } else {
                                    containerWidthPx
                                }
                                offsetX.animateTo(target, tween(180))
                                onDismiss()
                            } else {
                                offsetX.animateTo(0f, tween(250))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            isSwiping = false
                            offsetX.animateTo(0f, tween(250))
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        val newOffset = (offsetX.value + dragAmount).let {
                            when (dismissDirection) {
                                SwipeDirection.EndToStart -> it.coerceAtMost(0f)
                                SwipeDirection.StartToEnd -> it.coerceAtLeast(0f)
                                SwipeDirection.Both -> it
                            }
                        }
                        offsetX.snapTo(newOffset)
                    }
                }
            }
    ) {
        // Background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = progress },
            contentAlignment = when (dismissDirection) {
                SwipeDirection.EndToStart -> Alignment.CenterEnd
                SwipeDirection.StartToEnd -> Alignment.CenterStart
                SwipeDirection.Both -> {
                    if (offsetX.value < 0) Alignment.CenterEnd else Alignment.CenterStart
                }
            },
        ) {
            backgroundContent(progress)
        }

        // Foreground content
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    val scale = 1f - progress * 0.02f
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content()
        }
    }
}

enum class SwipeDirection {
    EndToStart,
    StartToEnd,
    Both,
}
