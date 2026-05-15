package de.carsten.android.muzzic.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A state object for managing reordering in a [ReorderableLazyColumn].
 */
class ReorderableState(
    val listState: LazyListState
) {
    var draggingItemIndex by mutableIntStateOf(-1)
    var dragOffset by mutableFloatStateOf(0f)

    fun onDragStart(index: Int) {
        draggingItemIndex = index
    }

    fun onDrag(offsetY: Float, onMove: (Int, Int) -> Unit) {
        dragOffset += offsetY
        val layoutInfo = listState.layoutInfo
        val currentItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex } ?: return
        val currentPosition = currentItem.offset + currentItem.size / 2 + dragOffset

        val targetItem = if (dragOffset > 0) {
            layoutInfo.visibleItemsInfo.findLast { item ->
                item.index > draggingItemIndex && currentPosition > item.offset + item.size / 2
            }
        } else {
            layoutInfo.visibleItemsInfo.find { item ->
                item.index < draggingItemIndex && currentPosition < item.offset + item.size / 2
            }
        }

        if (targetItem != null) {
            onMove(draggingItemIndex, targetItem.index)
            draggingItemIndex = targetItem.index
            dragOffset = 0f
        }
    }

    fun onDragEnd() {
        draggingItemIndex = -1
        dragOffset = 0f
    }
}

/**
 * Creates and remembers a [ReorderableState].
 */
@Composable
fun rememberReorderableState(listState: LazyListState = rememberLazyListState()): ReorderableState {
    return remember(listState) { ReorderableState(listState) }
}

/**
 * A specialized [LazyColumn] that supports drag-and-drop reordering of its items.
 *
 * @param items The list of items to display.
 * @param onMove Callback invoked when an item is moved from one index to another.
 * @param modifier The modifier to be applied to the column.
 * @param state The state object for managing reordering.
 * @param itemContent The composable that defines the UI for each item.
 */
@Composable
fun <T> ReorderableLazyColumn(
    items: List<T>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((Int, T) -> Any)? = null,
    state: ReorderableState = rememberReorderableState(),
    itemContent: @Composable LazyItemScope.(index: Int, item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit
) {
    LazyColumn(
        state = state.listState,
        modifier = modifier
    ) {
        itemsIndexed(items, key = key) { index, item ->
            val currentIndex by rememberUpdatedState(index)
            val isDragging = index == state.draggingItemIndex
            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")

            val dragModifier = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { state.onDragStart(currentIndex) },
                    onDragEnd = { state.onDragEnd() },
                    onDragCancel = { state.onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.onDrag(dragAmount.y, onMove)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = if (isDragging) state.dragOffset else 0f
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .shadow(elevation)
            ) {
                itemContent(index, item, isDragging, dragModifier)
            }
        }
    }
}
