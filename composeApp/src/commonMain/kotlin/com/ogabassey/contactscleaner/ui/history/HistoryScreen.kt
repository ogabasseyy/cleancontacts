package com.ogabassey.contactscleaner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import com.ogabassey.contactscleaner.ui.components.VerticalScrollBar
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: RecentActionsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val actions by viewModel.actions.collectAsState()
    val undoState by viewModel.undoState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle Undo results
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Process History", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (actions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Refresh, null, tint = TextLow, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No actions recorded yet", color = TextMedium)
                }
            }
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            "Recent Changes",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(actions, key = { it.id }) { snapshot ->
                        HistoryCard(
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
                        .padding(padding)
                        .padding(end = 4.dp, top = 24.dp, bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    snapshot: Snapshot,
    onUndo: () -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassy()
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        snapshot.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${snapshot.contacts.size} contacts affected",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }

                IconButton(
                    onClick = onUndo,
                    enabled = !isLoading,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = PrimaryNeon.copy(alpha = 0.1f),
                        contentColor = PrimaryNeon
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryNeon, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Restore, contentDescription = "Undo")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Action recorded at: ${formatTimestamp(snapshot.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    // Simple placeholder for KMP without datetime lib
    return "snapshot $timestamp" 
}
