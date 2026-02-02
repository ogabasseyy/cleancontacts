package com.ogabassey.contactscleaner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.ui.theme.PrimaryNeon
import kotlinx.coroutines.launch

/**
 * A shared Vertical Scroll Bar that adapts to the height of its container.
 * 2026 KMP Best Practice: Custom scroll indicators for better cross-platform visibility.
 */
@Composable
fun VerticalScrollBar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    var trackHeight by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    // Smooth alpha transition: stays partially visible when idle, brightens on scroll/hold
    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (listState.isScrollInProgress || isDragging) 1.0f else 0.4f,
        animationSpec = tween(durationMillis = 300)
    )
    
    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp) // Wide invisible touch target for easier grabbing
            .onGloballyPositioned { trackHeight = it.size.height.toFloat() }
            .pointerInput(trackHeight) {
                val totalItems = listState.layoutInfo.totalItemsCount
                if (totalItems <= 0) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        isDragging = true
                        
                        // Start tracking drag
                        var dragPosition = down.position.y
                        
                        verticalDrag(
                            down.id,
                            onDrag = { change ->
                                dragPosition = change.position.y.coerceIn(0f, trackHeight)
                                val scrollRatio = dragPosition / trackHeight
                                val targetIndex = (totalItems * scrollRatio).toInt().coerceIn(0, totalItems - 1)
                                
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                                change.consume()
                            },
                        )
                        
                        // Drag ended or cancelled
                        isDragging = false
                    }
                }
            }
    ) {
        val layoutInfo = listState.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val visibleItems = layoutInfo.visibleItemsInfo
        
        if (totalItemsCount > 0 && visibleItems.isNotEmpty() && trackHeight > 0) {
            val visibleItemsCount = visibleItems.size
            val firstVisibleIndex = listState.firstVisibleItemIndex
            
            // Calculate scrollbar size and position based on actual track height
            val sizeRatio = visibleItemsCount.toFloat() / totalItemsCount
            val scrollRatio = firstVisibleIndex.toFloat() / totalItemsCount
            
            val scrollbarHeight = (trackHeight * sizeRatio).coerceAtLeast(60f) // Minimum size for visibility
            val scrollbarOffset = trackHeight * scrollRatio
            
            // Adjust offset to stay within track bounds
            val adjustedOffset = scrollbarOffset.coerceIn(0f, (trackHeight - scrollbarHeight).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(thumbWidth)
                    .height(with(LocalDensity.current) { scrollbarHeight.toDp() })
                    .graphicsLayer(translationY = adjustedOffset)
                    .background(PrimaryNeon.copy(alpha = scrollbarAlpha.coerceAtLeast(0.4f)), CircleShape)
            )
        }
    }
}
