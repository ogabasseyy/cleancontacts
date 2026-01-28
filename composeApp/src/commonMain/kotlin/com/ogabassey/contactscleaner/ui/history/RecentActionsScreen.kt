package com.ogabassey.contactscleaner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import com.ogabassey.contactscleaner.ui.components.VerticalScrollBar
import com.ogabassey.contactscleaner.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Recent Actions (Undo History) Screen for Compose Multiplatform.
 *
 * Shows a list of recent destructive actions that can be undone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActionsScreen(
    viewModel: RecentActionsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val actions by viewModel.actions.collectAsState()
    val undoState by viewModel.undoState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle undo state changes
    LaunchedEffect(undoState) {
        when (val state = undoState) {
            is UndoState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            is UndoState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recent Actions",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (actions.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(PrimaryNeon.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = PrimaryNeon.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Recent Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Actions like delete and merge will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLow
                    )
                }
            }
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(actions, key = { it.id }) { snapshot ->
                        ActionCard(
                            snapshot = snapshot,
                            onUndo = { viewModel.undoAction(snapshot) },
                            isLoading = undoState is UndoState.Loading
                        )
                    }
                }
                
                VerticalScrollBar(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(paddingValues)
                        .padding(end = 4.dp, top = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    snapshot: Snapshot,
    onUndo: () -> Unit,
    isLoading: Boolean
) {
    val icon = when (snapshot.actionType) {
        "DELETE" -> Icons.Default.Delete
        "MERGE" -> Icons.Default.Refresh
        else -> Icons.Default.Refresh
    }

    val actionColor = when (snapshot.actionType) {
        "DELETE" -> ErrorNeon
        "MERGE" -> PrimaryNeon
        else -> SecondaryNeon
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSpaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(actionColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    snapshot.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${snapshot.contacts.size} contacts • ${formatRelativeTime(snapshot.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Undo Button
            Button(
                onClick = onUndo,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNeon,
                    disabledContainerColor = PrimaryNeon.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SpaceBlack
                    )
                } else {
                    Text(
                        "UNDO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpaceBlack
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to relative time string.
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = currentTimeMillis()
    val diffMs = now - timestamp
    val diffMinutes = diffMs / (1000 * 60)
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24

    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "$diffMinutes min ago"
        diffHours < 24 -> "$diffHours hours ago"
        diffDays == 1L -> "yesterday"
        diffDays < 7 -> "$diffDays days ago"
        else -> "${diffDays / 7} weeks ago"
    }
}

/**
 * Platform-agnostic way to get current time.
 * kotlinx-datetime would be better but this works for now.
 */
private fun currentTimeMillis(): Long {
    return kotlin.time.Clock.System.now().toEpochMilliseconds()
}
