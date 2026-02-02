package com.ogabassey.contactscleaner.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.util.BackgroundOperation
import com.ogabassey.contactscleaner.util.BackgroundOperationManager
import com.ogabassey.contactscleaner.util.OperationStatus
import kotlin.math.roundToInt

/**
 * 2026 Best Practice: Background operation overlay that can be minimized to a floating bubble.
 * Place this at the root of your app (in the main navigation container).
 */
@Composable
fun BackgroundOperationOverlay() {
    val operation by BackgroundOperationManager.currentOperation.collectAsState()
    val isMinimized by BackgroundOperationManager.isMinimized.collectAsState()

    if (operation == null) return

    AnimatedContent(
        targetState = isMinimized,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith
                    fadeOut(animationSpec = tween(200))
        },
        label = "operation_ui_transition"
    ) { minimized ->
        if (minimized) {
            FloatingOperationBubble(
                operation = operation!!,
                onClick = { BackgroundOperationManager.maximize() }
            )
        } else {
            OperationProgressModal(
                operation = operation!!,
                onMinimize = { BackgroundOperationManager.minimize() },
                onCancel = { BackgroundOperationManager.cancel() },
                onDismiss = { BackgroundOperationManager.dismiss() }
            )
        }
    }
}

/**
 * Full-screen modal showing operation progress.
 * 2026 Fix: Simplified - removed activity log since batch operations don't process 1-by-1.
 */
@Composable
private fun OperationProgressModal(
    operation: BackgroundOperation,
    onMinimize: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val isComplete = operation.status != OperationStatus.Running

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceSpaceElevated,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row with Minimize and Cancel/Close buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimize button (only show when running)
                    if (!isComplete) {
                        TextButton(
                            onClick = onMinimize,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextMedium)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Minimize",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Minimize", fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Cancel or Close button
                    TextButton(
                        onClick = if (isComplete) onDismiss else onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isComplete) PrimaryNeon else ErrorNeon
                        )
                    ) {
                        Icon(
                            if (isComplete) Icons.Default.Close else Icons.Default.Cancel,
                            contentDescription = if (isComplete) "Close" else "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isComplete) "Close" else "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status icon with animation
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (operation.status) {
                            OperationStatus.Running -> {
                                // 2026 Fix: Removed rotation - causes glitching with determinate progress
                                CircularProgressIndicator(
                                    progress = { operation.progress },
                                    modifier = Modifier.size(64.dp),
                                    color = PrimaryNeon,
                                    trackColor = PrimaryNeon.copy(alpha = 0.2f),
                                    strokeWidth = 4.dp
                                )
                            }
                            OperationStatus.Completed -> {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Complete",
                                    tint = SuccessNeon,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            OperationStatus.Failed -> {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = "Failed",
                                    tint = ErrorNeon,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            OperationStatus.Cancelled -> {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "Cancelled",
                                    tint = WarningNeon,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        operation.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Percentage
                    Text(
                        "${(operation.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = when (operation.status) {
                            OperationStatus.Running -> PrimaryNeon
                            OperationStatus.Completed -> SuccessNeon
                            OperationStatus.Failed -> ErrorNeon
                            OperationStatus.Cancelled -> WarningNeon
                        },
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Linear progress bar
                    LinearProgressIndicator(
                        progress = { operation.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when (operation.status) {
                            OperationStatus.Running -> PrimaryNeon
                            OperationStatus.Completed -> SuccessNeon
                            OperationStatus.Failed -> ErrorNeon
                            OperationStatus.Cancelled -> WarningNeon
                        },
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Current item or completion message
                    val displayText = when {
                        operation.status == OperationStatus.Completed ->
                            operation.completionMessage ?: "Operation complete!"
                        operation.status == OperationStatus.Failed ->
                            operation.completionMessage ?: "Operation failed"
                        operation.status == OperationStatus.Cancelled ->
                            "Operation cancelled"
                        operation.currentItem != null ->
                            "Processing: ${operation.currentItem}"
                        else ->
                            "Processing..."
                    }

                    Text(
                        displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Count display
                    if (operation.status == OperationStatus.Running) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${operation.processedItems} of ${operation.totalItems} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLow
                        )
                    }
                }

                // 2026 Fix: Removed activity log - batch operations don't process items 1-by-1
                // so individual log entries were inaccurate. The progress bar is sufficient.
            }
        }
    }
}

/**
 * Floating bubble that shows when operation is minimized.
 * Can be dragged and tapped to expand.
 * 2026 Fix: Added boundary clamping to keep bubble within screen bounds.
 */
@Composable
private fun FloatingOperationBubble(
    operation: BackgroundOperation,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Bubble dimensions for boundary calculation
    val bubbleWidth = 100.dp
    val bubbleHeight = 56.dp
    val padding = 16.dp

    // Pulse animation when nearing completion
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = if (operation.progress > 0.9f) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotation animation for spinner effect
    val rotation by rememberInfiniteTransition(label = "bubble_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_rotation"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Calculate boundaries based on container size
        // Since bubble is anchored at BottomEnd, offset is relative to that position
        val bubbleWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { bubbleWidth.toPx() }
        val bubbleHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { bubbleHeight.toPx() }
        val paddingPx = with(androidx.compose.ui.platform.LocalDensity.current) { padding.toPx() }

        // Min/max offsets (negative X moves left, negative Y moves up from bottom-end anchor)
        val minX = -(constraints.maxWidth - bubbleWidthPx - paddingPx * 2)
        val maxX = 0f
        val minY = -(constraints.maxHeight - bubbleHeightPx - paddingPx * 2)
        val maxY = 0f

        Surface(
            modifier = Modifier
                .padding(padding)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // 2026 Fix: Clamp offset to keep bubble within screen bounds
                        offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                    }
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceSpaceElevated,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circular progress or status icon
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (operation.status) {
                        OperationStatus.Running -> {
                            CircularProgressIndicator(
                                progress = { operation.progress },
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer { rotationZ = rotation * 0.05f },
                                color = PrimaryNeon,
                                trackColor = PrimaryNeon.copy(alpha = 0.2f),
                                strokeWidth = 3.dp
                            )
                        }
                        OperationStatus.Completed -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessNeon,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        OperationStatus.Failed -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorNeon,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        OperationStatus.Cancelled -> {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = WarningNeon,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Percentage text
                Text(
                    "${(operation.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = when (operation.status) {
                        OperationStatus.Running -> PrimaryNeon
                        OperationStatus.Completed -> SuccessNeon
                        OperationStatus.Failed -> ErrorNeon
                        OperationStatus.Cancelled -> WarningNeon
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
