package hk.uwu.reareye.ui.components.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * State for drag-to-reorder in a LazyColumn-like list.
 * Attach [draggableItem] to each item and provide callbacks for reorder events.
 */
@Composable
fun rememberDragReorderState(
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): DragReorderState {
    val scope = rememberCoroutineScope()
    return remember(onMove) {
        DragReorderState(scope = scope, onMove = onMove)
    }
}

class DragReorderState internal constructor(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingIndex by mutableIntStateOf(-1)
        internal set
    var dragOffset by mutableFloatStateOf(0f)
        internal set
    var itemHeight by mutableFloatStateOf(0f)
        internal set

    internal val itemPositions = mutableMapOf<Int, Float>()

    fun itemModifier(index: Int): Modifier {
        return Modifier
            .onGloballyPositioned { coords ->
                itemPositions[index] = coords.positionInParent().y
                if (coords.size.height > 0) {
                    itemHeight = coords.size.height.toFloat()
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        draggingIndex = index
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y

                        // Calculate target position based on drag offset
                        val currentY = itemPositions[index] ?: return@detectDragGesturesAfterLongPress
                        val targetY = currentY + dragOffset

                        // Find which item index the target position corresponds to
                        val targetIndex = itemPositions.entries
                            .filter { it.key != index }
                            .minByOrNull { (_, y) -> kotlin.math.abs(y - targetY) }
                            ?.key ?: return@detectDragGesturesAfterLongPress

                        if (targetIndex != index) {
                            onMove(index, targetIndex)
                        }
                    },
                    onDragEnd = {
                        draggingIndex = -1
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        draggingIndex = -1
                        dragOffset = 0f
                    },
                )
            }
            .graphicsLayer {
                if (draggingIndex == index) {
                    scaleX = 1.03f
                    scaleY = 1.03f
                    shadowElevation = 12f
                    alpha = 0.95f
                }
            }
            .offset {
                if (draggingIndex == index) {
                    IntOffset(0, dragOffset.roundToInt())
                } else {
                    IntOffset.Zero
                }
            }
    }

    fun isDragging(index: Int): Boolean = draggingIndex == index
    fun isAnyDragging(): Boolean = draggingIndex >= 0
}
