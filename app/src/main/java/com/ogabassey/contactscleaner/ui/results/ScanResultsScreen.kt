package com.ogabassey.contactscleaner.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (ContactType) -> Unit,
    onNavigateToExport: () -> Unit,
    viewModel: ResultsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val scanResult = viewModel.scanResult
    if (scanResult == null) {
        EmptyState("No scan results found.")
        return
    }

    val uiState by viewModel.uiState.collectAsState()
    val isPremium by viewModel.billingRepository.isPremium.collectAsState()
    val remainingActions by viewModel.freeActionsRemaining.collectAsState(2)
    val currentUiState = uiState // Capture for smart cast
    
    // Handle UI State changes (Success/Error)
    LaunchedEffect(currentUiState) {
        if (currentUiState is ResultsUiState.Success) {
            if (!currentUiState.shouldRescan) {
                kotlinx.coroutines.delay(1500)
                onNavigateBack()
                viewModel.resetState()
            }
            // If shouldRescan is true, we stay here and let StatusOverlay handle it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Results", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TrialStatusChip(
                        isPremium = isPremium,
                        remainingActions = remainingActions,
                        onClick = { viewModel.performBulkExport(ContactType.ALL) } // Triggers paywall if needed
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Background Gradient
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 3)
                val radius = size.width * 1.5f
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            com.ogabassey.contactscleaner.ui.theme.PrimaryNeon.copy(alpha = 0.05f),
                            com.ogabassey.contactscleaner.ui.theme.BackgroundDark
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Main Content: Bento Grid Only
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                
                    Text("Overview", style = MaterialTheme.typography.titleSmall, color = TextMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

                    // NEW Tip Banner for Format Issues
                    if (scanResult.formatIssueCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SecondaryNeon.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryNeon.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { onNavigateToDetail(ContactType.FORMAT_ISSUE) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = SecondaryNeon)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Improve Your Results",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SecondaryNeon,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Fixing format issues helps identify WhatsApp numbers and find more duplicates.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextHigh
                                    )
                                }
                            }
                        }
                    }

                    // 1. Granular Bento Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // --- FORMAT ISSUES (PRIORITY) ---
                        if (scanResult.formatIssueCount > 0) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text("High Priority", style = MaterialTheme.typography.titleSmall, color = SecondaryNeon, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                BentoCard(
                                    label = "Est. Intl. Formats", // "Format Issues"
                                    count = scanResult.formatIssueCount,
                                    type = ContactType.FORMAT_ISSUE,
                                    selected = false, // Highlight?
                                    color = SecondaryNeon,
                                    subtitle = "Tap to fix for better detection"
                                ) { onNavigateToDetail(it) }
                            }
                        }

                        // --- STANDARD ---
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                             Text("Categories", style = MaterialTheme.typography.titleSmall, color = TextMedium, modifier = Modifier.padding(top = 8.dp))
                        }
                        item { 
                            // Show Raw Count context
                            val label = if (scanResult.rawCount > scanResult.total) "Merged from ${scanResult.rawCount}" else "System Contacts"
                            BentoCard("All Contacts", scanResult.total, ContactType.ALL, false, SecondaryNeon, subtitle = label) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Accounts", scanResult.accountCount, ContactType.ACCOUNT, false, SecondaryNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("WhatsApp", scanResult.whatsAppCount, ContactType.WHATSAPP, false, PrimaryNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Telegram", scanResult.telegramCount, ContactType.TELEGRAM, false, PrimaryNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Non-WhatsApp", scanResult.nonWhatsAppCount, ContactType.NON_WHATSAPP, false, TextMedium) { onNavigateToDetail(it) } 
                        }

                        // --- DUPLICATES SECTION ---
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text("Duplicates", style = MaterialTheme.typography.titleSmall, color = TextMedium, modifier = Modifier.padding(top = 16.dp))
                        }
                        item { 
                            BentoCard("Dup Numbers", scanResult.numberDuplicateCount, ContactType.DUP_NUMBER, false, WarningNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Dup Emails", scanResult.emailDuplicateCount, ContactType.DUP_EMAIL, false, WarningNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Exact Names", scanResult.nameDuplicateCount, ContactType.DUP_NAME, false, WarningNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Similar Names", scanResult.similarNameCount, ContactType.DUP_SIMILAR_NAME, false, WarningNeon) { onNavigateToDetail(it) } 
                        }

                        // --- BLANK CONTACTS ---
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text("Blank Contacts", style = MaterialTheme.typography.titleSmall, color = TextMedium, modifier = Modifier.padding(top = 16.dp))
                        }
                        item { 
                            BentoCard("Empty Name", scanResult.noNameCount, ContactType.JUNK_NO_NAME, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Empty Number", scanResult.noNumberCount, ContactType.JUNK_NO_NUMBER, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }


                        // --- ANOMALIES / JUNK ---
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text("Junk & Anomalies", style = MaterialTheme.typography.titleSmall, color = TextMedium, modifier = Modifier.padding(top = 16.dp))
                        }
                        item { 
                            BentoCard("Invalid Char", scanResult.invalidCharCount, ContactType.JUNK_INVALID_CHAR, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Long Numbers", scanResult.longNumberCount, ContactType.JUNK_LONG_NUMBER, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Short Numbers", scanResult.shortNumberCount, ContactType.JUNK_SHORT_NUMBER, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Repetitive #", scanResult.repetitiveNumberCount, ContactType.JUNK_REPETITIVE, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }
                        item { 
                            BentoCard("Symbol Names", scanResult.symbolNameCount, ContactType.JUNK_SYMBOL, false, ErrorNeon) { onNavigateToDetail(it) } 
                        }
                    }
                }
            }

            // Status Overlays
            StatusOverlay(uiState, viewModel)
        }
    }
}


@Composable
fun BentoCard(
    label: String,
    count: Int,
    type: ContactType,
    selected: Boolean,
    color: Color,
    subtitle: String? = null,
    onClick: (ContactType) -> Unit
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.US) }
    
    // Category-specific icon
    val icon = when(type) {
        ContactType.ALL -> Icons.Default.Person
        ContactType.ACCOUNT -> Icons.Default.AccountCircle
        ContactType.WHATSAPP -> Icons.Default.Email
        ContactType.TELEGRAM -> Icons.Default.Send
        ContactType.NON_WHATSAPP -> Icons.Default.Phone
        ContactType.DUP_NUMBER, ContactType.DUP_EMAIL, ContactType.DUP_NAME, ContactType.DUP_SIMILAR_NAME -> Icons.Default.Face
        ContactType.JUNK, ContactType.JUNK_NO_NAME, ContactType.JUNK_NO_NUMBER -> Icons.Default.Delete
        ContactType.JUNK_INVALID_CHAR, ContactType.JUNK_SYMBOL -> Icons.Default.Warning
        ContactType.JUNK_LONG_NUMBER, ContactType.JUNK_SHORT_NUMBER, ContactType.JUNK_REPETITIVE -> Icons.Default.Star
        ContactType.JUNK_SUSPICIOUS -> Icons.Default.Lock
        ContactType.FORMAT_ISSUE -> Icons.Default.Edit
        else -> Icons.Default.Info
    }
    
    // Dynamic glow for high-count items (more subtle now)
    val glowAlpha = if (count > 1000) 0.1f else if (count > 100) 0.05f else 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        SurfaceSpaceElevated.copy(alpha = 0.95f),
                        DeepSpace.copy(alpha = 0.9f)
                    )
                )
            )
            .then(
                if (glowAlpha > 0) Modifier.border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(color.copy(alpha = glowAlpha * 3), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) else Modifier.border(
                    width = 0.5.dp,
                    color = GlassWhite,
                    shape = RoundedCornerShape(20.dp)
                )
            )
            .clickable { onClick(type) }
    ) {
        // Left accent bar (thicker for more impact)
        Box(
            modifier = Modifier
                .width(5.dp)
                .matchParentSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Icon in top-left with slightly better contrast
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = numberFormat.format(count), 
                style = MaterialTheme.typography.displaySmall, 
                fontWeight = FontWeight.ExtraBold,
                color = if (count > 0) color else TextMedium.copy(alpha = 0.4f),
                letterSpacing = 0.sp
            )
            Text(
                text = label.uppercase(), 
                style = MaterialTheme.typography.labelLarge, 
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = TextMedium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

fun getFilterTitle(type: ContactType): String {
    return when(type) {
        ContactType.JUNK_NO_NAME -> "Contacts with No Name"
        ContactType.JUNK_NO_NUMBER -> "Contacts with No Number"
        ContactType.JUNK_INVALID_CHAR -> "Numbers with Invalid Chars"
        ContactType.JUNK_LONG_NUMBER -> "Suspicious Long Numbers"
        ContactType.JUNK_SHORT_NUMBER -> "Suspicious Short Numbers"
        ContactType.JUNK_REPETITIVE -> "Repetitive Numbers"
        ContactType.JUNK_SYMBOL -> "Symbol-Only Names"
        
        ContactType.DUP_EMAIL -> "Duplicate Emails"
        ContactType.DUP_NUMBER -> "Duplicate Numbers"
        ContactType.DUP_NAME -> "Exact Name Duplicates"
        ContactType.DUP_SIMILAR_NAME -> "Similar Name Duplicates"
        
        ContactType.ACCOUNT -> "Synced Accounts"
        ContactType.TELEGRAM -> "Telegram Contacts"
        ContactType.ALL -> "All System Contacts"
        else -> type.name
    }
}


@Composable
fun StatusOverlay(uiState: ResultsUiState, viewModel: ResultsViewModel, onRescan: () -> Unit = {}) {
    val shouldShow = uiState is ResultsUiState.Processing || 
                     uiState is ResultsUiState.Error || 
                     (uiState is ResultsUiState.Success && uiState.shouldRescan)

    if (shouldShow) {
        val isSuccess = uiState is ResultsUiState.Success
        val isError = uiState is ResultsUiState.Error
        val isProcessing = uiState is ResultsUiState.Processing
        val shouldRescan = (uiState as? ResultsUiState.Success)?.shouldRescan == true

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack.copy(alpha = 0.95f))
                .zIndex(10f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                if (isProcessing) {
                    val state = uiState as ResultsUiState.Processing
                    CircularProgressIndicator(
                        progress = { state.progress },
                        color = PrimaryNeon,
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = state.message?.uppercase() ?: "PROCESSING...",
                        color = TextHigh,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                } else {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSuccess) SuccessNeon else ErrorNeon,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = when (uiState) {
                            is ResultsUiState.Success -> uiState.message
                            is ResultsUiState.Error -> uiState.message
                            else -> ""
                        },
                        color = TextHigh,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    if (isError) {
                        Button(
                            onClick = { viewModel.resetState() },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceSpaceElevated)
                        ) {
                            Text("TRY AGAIN", color = TextHigh)
                        }
                    } else if (shouldRescan) {
                         Button(
                            onClick = onRescan,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("RESCAN NOW", color = SpaceBlack, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                         TextButton(
                            onClick = { viewModel.resetState() }
                        ) {
                            Text("LATER", color = TextMedium)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EmptyState(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = TextLow, style = MaterialTheme.typography.bodyLarge)
    }
}
