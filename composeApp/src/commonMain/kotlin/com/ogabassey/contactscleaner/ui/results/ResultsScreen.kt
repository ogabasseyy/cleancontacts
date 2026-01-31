package com.ogabassey.contactscleaner.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.List

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.ui.components.VerticalScrollBar
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppContactsCard
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppLinkCard
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppLinkState
import com.ogabassey.contactscleaner.ui.whatsapp.WhatsAppLinkViewModel
import com.ogabassey.contactscleaner.ui.whatsapp.SyncState
import com.ogabassey.contactscleaner.util.formatWithCommas
import com.ogabassey.contactscleaner.util.isAndroid
import com.ogabassey.contactscleaner.util.isIOS
import org.koin.compose.viewmodel.koinViewModel

/**
 * Results Screen for Compose Multiplatform.
 *
 * Shows scan results and allows cleanup actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (ContactType) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToWhatsAppLink: () -> Unit = {},
    onNavigateToWhatsAppContacts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val allIssuesCount by viewModel.allIssuesCount.collectAsState(initial = 0)
    val freeActions by viewModel.freeActionsRemaining.collectAsState(initial = 2)

    // 2026 Best Practice: Only instantiate VPS-based ViewModel on iOS to save resources on Android.
    val whatsAppViewModel: WhatsAppLinkViewModel? = if (isIOS) koinViewModel() else null
    val whatsAppState = whatsAppViewModel?.state?.collectAsState()?.value ?: WhatsAppLinkState.NotLinked
    val syncState = whatsAppViewModel?.syncState?.collectAsState()?.value ?: SyncState.Idle

    // 2026 Best Practice: Track retry state for immediate UI feedback
    var isRetryingSync by remember { mutableStateOf(false) }

    // Recalculate WhatsApp counts when sync completes, and clear retry state on any state change
    LaunchedEffect(syncState) {
        // Clear retry flag when state changes (sync started, completed, or new error)
        isRetryingSync = false
        if (syncState is SyncState.Complete) {
            viewModel.recalculateWhatsAppCounts()
        }
    }

    var showAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Scan Results",
                            modifier = Modifier.align(Alignment.CenterStart),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onNavigateToHistory,
                            modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Restore, 
                                contentDescription = "History",
                                tint = PrimaryNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
        }
    ) { paddingValues ->
        val currentResult = scanResult
        if (currentResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No scan results available", color = TextMedium)
            }
        } else {
            val scanResult = currentResult // Local shadow for smart cast
            val listState = rememberLazyListState()
            val accountsCount by viewModel.accountsCount.collectAsState(initial = 0)
            val accountGroups by viewModel.accountGroups.collectAsState()

            if (showAccountDialog) {
                AccountSelectionDialog(
                    groups = accountGroups,
                    onDismiss = { showAccountDialog = false },
                    onAccountClick = { group ->
                        showAccountDialog = false
                        // Handle account filtering or detail view
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryCard(
                            result = scanResult,
                            accountsCount = accountsCount,
                            totalScanned = scanResult.rawCount,
                            totalIssues = allIssuesCount,
                            onAccountsClick = { 
                                viewModel.loadAccountGroups()
                                showAccountDialog = true 
                            }
                        )
                            
                            // Trial Actions Pill
                            if (freeActions > 0) {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryNeon.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            "$freeActions Free Actions Remaining",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PrimaryNeon,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // High Priority Section
                        if (scanResult.formatIssueCount > 0 || scanResult.sensitiveCount > 0) {
                            item {
                                Text(
                                    "HIGH PRIORITY",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SecondaryNeon,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            
                            if (scanResult.formatIssueCount > 0) {
                                item {
                                    IssueCard(
                                        title = "Format Issues",
                                        count = scanResult.formatIssueCount,
                                        icon = Icons.Default.Dialpad,
                                        color = SecondaryNeon,
                                        description = "Fixing Format issues helps identify WhatsApp numbers and reduces error matches",
                                        onClick = { onNavigateToDetail(ContactType.FORMAT_ISSUE) }
                                    )
                                }
                            }

                            if (scanResult.sensitiveCount > 0) {
                                 item {
                                    IssueCard(
                                        title = "Sensitive Data",
                                        count = scanResult.sensitiveCount,
                                        icon = Icons.Default.Lock,
                                        color = WarningNeon,
                                        description = "Safety Check: Potential sensitive IDs detected",
                                        onClick = { onNavigateToDetail(ContactType.SENSITIVE) }
                                    )
                                }
                            }
                        }



                        item {
                            Text(
                                "ISSUES FOUND",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }


                        // Dynamic Sorted List of Issues
                        val issues = listOfNotNull(
                            if (scanResult.junkCount > 0) ResultItem(
                                "Junk Contacts", scanResult.junkCount, Icons.Default.Error, ErrorNeon, "Contacts with missing or invalid data", ContactType.JUNK_NO_NAME
                            ) else null,
                            if (scanResult.noNumberCount > 0) ResultItem(
                                "Missing Numbers", scanResult.noNumberCount, Icons.Default.Phone, ErrorNeon, "Contacts without any phone numbers", ContactType.JUNK_NO_NUMBER
                            ) else null,
                            if (scanResult.duplicateCount > 0) ResultItem(
                                "Duplicates", scanResult.duplicateCount, Icons.Default.Person, WarningNeon, "Contacts that appear multiple times", ContactType.DUPLICATE
                            ) else null,
                            if (scanResult.invalidCharCount > 0) ResultItem(
                                "Invalid Characters", scanResult.invalidCharCount, Icons.Default.Warning, ErrorNeon, "Contacts with unusual characters", ContactType.JUNK_INVALID_CHAR
                            ) else null,
                            if (scanResult.repetitiveNumberCount > 0) ResultItem(
                                "Repetitive Numbers", scanResult.repetitiveNumberCount, Icons.Default.Refresh, ErrorNeon, "Numbers with repeating sequences", ContactType.JUNK_REPETITIVE
                            ) else null,
                            if (scanResult.symbolNameCount > 0) ResultItem(
                                "Symbol-only Names", scanResult.symbolNameCount, Icons.Default.Edit, ErrorNeon, "Names consisting only of symbols", ContactType.JUNK_SYMBOL
                            ) else null,
                            if (scanResult.numericalNameCount > 0) ResultItem(
                                "Numerical Names", scanResult.numericalNameCount, Icons.AutoMirrored.Filled.List, ErrorNeon, "Names consisting only of digits", ContactType.JUNK_NUMERICAL_NAME
                            ) else null,
                            if (scanResult.emojiNameCount > 0) ResultItem(
                                "Emoji-only Names", scanResult.emojiNameCount, Icons.Default.Face, ErrorNeon, "Names consisting only of emojis", ContactType.JUNK_EMOJI_NAME
                            ) else null,
                            if (scanResult.fancyFontCount > 0) ResultItem(
                                "Fancy Fonts", scanResult.fancyFontCount, Icons.Default.FontDownload, ErrorNeon, "Stylized or mathematical scripts", ContactType.JUNK_FANCY_FONT
                            ) else null,
                            if (scanResult.longNumberCount > 0) ResultItem(
                                "Long Numbers", scanResult.longNumberCount, Icons.Default.Info, WarningNeon, "Numbers that are unusually long", ContactType.JUNK_LONG_NUMBER
                            ) else null,
                            if (scanResult.shortNumberCount > 0) ResultItem(
                                "Short Numbers", scanResult.shortNumberCount, Icons.Default.Info, WarningNeon, "Numbers that are unusually short", ContactType.JUNK_SHORT_NUMBER
                            ) else null
                        )

                        val sortedIssues = issues.sortedBy { it.title }

                        items(sortedIssues) { issue ->
                            IssueCard(
                                title = issue.title,
                                count = issue.count,
                                icon = issue.icon,
                                color = issue.color,
                                description = issue.description,
                                onClick = { onNavigateToDetail(issue.type) }
                            )
                        }

                        item {
                            Text(
                                "CONTACT BREAKDOWN",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        // Platform-specific WhatsApp display
                        if (isAndroid) {
                            // Android: Show native WhatsApp counts (detected via account_type)
                            if (scanResult.whatsAppCount > 0) {
                                item {
                                    StatCard(
                                        title = "WhatsApp",
                                        count = scanResult.whatsAppCount,
                                        icon = Icons.Default.Email,
                                        color = PrimaryNeon,
                                        onClick = { onNavigateToDetail(ContactType.WHATSAPP) }
                                    )
                                }
                            }

                            // Non-WhatsApp Contacts (Android only - native detection)
                            if (scanResult.nonWhatsAppCount > 0) {
                                item {
                                    StatCard(
                                        title = "Non-WhatsApp Contacts",
                                        count = scanResult.nonWhatsAppCount,
                                        icon = Icons.Default.Phone,
                                        color = TextMedium,
                                        onClick = { onNavigateToDetail(ContactType.NON_WHATSAPP) }
                                    )
                                }
                            }
                        } else {
                            // iOS: Show link CTA or counts based on WhatsApp connection status
                            when (whatsAppState) {
                                is WhatsAppLinkState.Connected -> {
                                    // Connected: Show WhatsApp contacts card with business detection
                                    item {
                                        WhatsAppContactsCard(
                                            onViewContacts = onNavigateToWhatsAppContacts
                                        )
                                    }

                                    // Show sync progress or WhatsApp breakdown based on sync state
                                    when (val currentSyncState = syncState) {
                                        is SyncState.Syncing -> {
                                            // Syncing: Show progress card
                                            item {
                                                WhatsAppSyncProgressCard(
                                                    synced = currentSyncState.synced,
                                                    total = currentSyncState.total,
                                                    percent = currentSyncState.percent
                                                )
                                            }
                                        }
                                        is SyncState.Complete -> {
                                            // Sync complete: Show Personal/Business/Non-WhatsApp breakdown
                                            item {
                                                StatCard(
                                                    title = "Personal WhatsApp",
                                                    count = currentSyncState.personalCount,
                                                    icon = Icons.Default.Person,
                                                    color = PrimaryNeon,
                                                    onClick = { onNavigateToDetail(ContactType.WHATSAPP) }
                                                )
                                            }
                                            item {
                                                StatCard(
                                                    title = "Business WhatsApp",
                                                    count = currentSyncState.businessCount,
                                                    icon = Icons.Default.Business,
                                                    color = SecondaryNeon,
                                                    onClick = { onNavigateToWhatsAppContacts() }
                                                )
                                            }
                                            // Non-WhatsApp: Only show after sync completes
                                            if (scanResult.nonWhatsAppCount > 0) {
                                                item {
                                                    StatCard(
                                                        title = "Non-WhatsApp Contacts",
                                                        count = scanResult.nonWhatsAppCount,
                                                        icon = Icons.Default.PhoneDisabled,
                                                        color = TextMedium,
                                                        onClick = { onNavigateToDetail(ContactType.NON_WHATSAPP) }
                                                    )
                                                }
                                            }
                                        }
                                        is SyncState.Error -> {
                                            // Sync error: Show retry option
                                            item {
                                                WhatsAppSyncErrorCard(
                                                    message = currentSyncState.message,
                                                    isRetrying = isRetryingSync,
                                                    onRetry = {
                                                        isRetryingSync = true
                                                        whatsAppViewModel?.startWhatsAppSync()
                                                    }
                                                )
                                            }
                                        }
                                        is SyncState.Idle -> {
                                            // Idle (not synced yet): Show basic WhatsApp count
                                            if (scanResult.whatsAppCount > 0) {
                                                item {
                                                    StatCard(
                                                        title = "WhatsApp",
                                                        count = scanResult.whatsAppCount,
                                                        icon = Icons.Default.Email,
                                                        color = PrimaryNeon,
                                                        onClick = { onNavigateToDetail(ContactType.WHATSAPP) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Not connected: Show CTA to link WhatsApp
                                    item {
                                        WhatsAppLinkCard(
                                            onLinkClick = onNavigateToWhatsAppLink
                                        )
                                    }
                                }
                            }
                        }

                        // Telegram Contacts (shown on both platforms when detected)
                        if (scanResult.telegramCount > 0) {
                            item {
                                StatCard(
                                    title = "Telegram",
                                    count = scanResult.telegramCount,
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    color = SecondaryNeon,
                                    onClick = { onNavigateToDetail(ContactType.TELEGRAM) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // Simple Visual Scroll Indicator
                    VerticalScrollBar(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp, top = 24.dp, bottom = 24.dp)
                    )
                }

        }

        // Show processing overlay
        when (val state = uiState) {
            is ResultsUiState.Processing -> {
                ProcessingOverlay(progress = state.progress, message = state.message)
            }
            is ResultsUiState.Success -> {
                LaunchedEffect(state) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.resetState()
                }
            }
            is ResultsUiState.ShowPaywall -> {
                LaunchedEffect(Unit) {
                    onNavigateToPaywall()
                    viewModel.resetState()
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun SummaryCard(
    result: ScanResult,
    accountsCount: Int,
    totalScanned: Int,
    totalIssues: Int,
    onAccountsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassy(radius = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Upper Section: Large Main Metric
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 8.dp) // Subtle lead-in padding
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SecondaryNeon.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = null,
                        tint = SecondaryNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = totalScanned.formatWithCommas(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "contacts found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.White.copy(alpha = 0.1f)
            )

            // Lower Section: Secondary Metrics
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Accounts
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAccountsClick() }
                ) {
                    Text(
                        text = accountsCount.formatWithCommas(),
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Accounts",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                        .align(Alignment.CenterVertically)
                )

                // Total Issues
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = totalIssues.formatWithCommas(),
                        style = MaterialTheme.typography.titleLarge,
                        color = ErrorNeon,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Total Issues",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMedium)
            }
            Text(
                count.formatWithCommas(),
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = color.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
            Text(count.formatWithCommas(), style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLow)
        }
    }
}

@Composable
fun AccountSelectionDialog(
    groups: List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>,
    onDismiss: () -> Unit,
    onAccountClick: (com.ogabassey.contactscleaner.domain.model.AccountGroupSummary) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = PrimaryNeon)
            }
        },
        containerColor = DeepSpace,
        title = {
            Text("Contact Accounts", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                val dialogListState = rememberLazyListState()
                LazyColumn(
                    state = dialogListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (groups.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PrimaryNeon)
                            }
                        }
                    } else {
                        items(groups) { group ->
                            AccountDialogItem(group = group, onClick = { onAccountClick(group) })
                        }
                    }
                }
                
                // Add visible scrollbar to dialog too
                if (groups.size > 5) {
                    VerticalScrollBar(
                        listState = dialogListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun AccountDialogItem(
    group: com.ogabassey.contactscleaner.domain.model.AccountGroupSummary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when {
                group.accountName?.contains("google", ignoreCase = true) == true -> Icons.Default.Email
                group.accountName?.contains("whatsapp", ignoreCase = true) == true -> Icons.Default.Email 
                else -> Icons.Default.Phone
            }
            
            Icon(icon, null, tint = SecondaryNeon, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.accountName ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold)
                Text(group.accountType ?: "Unknown", color = TextMedium, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                group.count.formatWithCommas(),
                color = PrimaryNeon,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ProcessingOverlay(progress: Float, message: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                progress = { progress },
                color = PrimaryNeon,
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryNeon,
                fontWeight = FontWeight.Bold
            )
            message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextMedium)
            }
        }
    }
}

// Local VerticalScrollBar removed, now using shared component from ui.components

private data class ResultItem(
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val color: Color,
    val description: String,
    val type: ContactType
)

/**
 * Card showing WhatsApp sync progress.
 */
@Composable
private fun WhatsAppSyncProgressCard(
    synced: Int,
    total: Int,
    percent: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PrimaryNeon.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    color = PrimaryNeon,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Syncing WhatsApp Contacts...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${synced.formatWithCommas()} / ${total.formatWithCommas()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { percent / 100f },
                color = PrimaryNeon,
                trackColor = PrimaryNeon.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Card showing WhatsApp sync error with retry option.
 * 2026 Best Practice: Shows loading state during retry for immediate feedback.
 */
@Composable
private fun WhatsAppSyncErrorCard(
    message: String,
    isRetrying: Boolean = false,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ErrorNeon.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorNeon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sync Failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }
            // 2026 Best Practice: Show spinner during retry for immediate feedback
            if (isRetrying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PrimaryNeon,
                    strokeWidth = 2.dp
                )
            } else {
                TextButton(onClick = onRetry, enabled = !isRetrying) {
                    Text("RETRY", color = PrimaryNeon, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


