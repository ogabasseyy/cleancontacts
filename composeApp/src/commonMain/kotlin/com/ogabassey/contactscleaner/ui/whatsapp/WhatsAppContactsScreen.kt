package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.data.api.BusinessDetectionProgress
import com.ogabassey.contactscleaner.data.api.WhatsAppContact
import com.ogabassey.contactscleaner.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * WhatsApp Contacts screen with Business/Personal tabs and export functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppContactsScreen(
    viewModel: WhatsAppContactsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToLink: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    var showExportMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("") }
    val exportData by viewModel.exportData.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Handle export data when available
    LaunchedEffect(exportData) {
        if (exportData != null) {
            showExportDialog = true
        }
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WhatsApp Contacts",
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
                actions = {
                    if (state is WhatsAppContactsState.Loaded) {
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Export",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export as CSV") },
                                    leadingIcon = { Icon(Icons.Default.Description, null) },
                                    onClick = {
                                        showExportMenu = false
                                        exportFormat = "CSV"
                                        viewModel.exportToCsv()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export as vCard") },
                                    leadingIcon = { Icon(Icons.Default.ContactPage, null) },
                                    onClick = {
                                        showExportMenu = false
                                        exportFormat = "vCard"
                                        viewModel.exportToVCard()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is WhatsAppContactsState.Loading,
                is WhatsAppContactsState.LoadingMore -> {
                    LoadingContent()
                }
                is WhatsAppContactsState.NotConnected -> {
                    NotConnectedContent(onLinkWhatsApp = onNavigateToLink)
                }
                is WhatsAppContactsState.Error -> {
                    ErrorContent(
                        message = currentState.message,
                        onRetry = { viewModel.loadContacts() }
                    )
                }
                is WhatsAppContactsState.Loaded -> {
                    ContactsContent(
                        contacts = viewModel.getFilteredContacts(),
                        businessCount = currentState.businessCount,
                        personalCount = currentState.personalCount,
                        totalCount = currentState.totalCount,
                        nonWhatsAppCount = currentState.nonWhatsAppCount,
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        businessDetectionProgress = currentState.businessDetectionProgress
                    )
                }
            }
        }
    }

    // Export dialog
    if (showExportDialog && exportData != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                viewModel.clearExportData()
            },
            title = { Text("Export Ready", color = Color.White) },
            text = {
                Column {
                    Text(
                        "$exportFormat data generated with ${viewModel.getFilteredContacts().size} contacts.",
                        color = TextMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Copy to clipboard to share or save.",
                        color = TextLow
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        exportData?.let { clipboardManager.setText(AnnotatedString(it)) }
                        showExportDialog = false
                        viewModel.clearExportData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy to Clipboard", color = SpaceBlack)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        viewModel.clearExportData()
                    }
                ) {
                    Text("Cancel", color = TextMedium)
                }
            },
            containerColor = DeepSpace
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryNeon)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading contacts...", color = TextMedium)
        }
    }
}

@Composable
private fun NotConnectedContent(onLinkWhatsApp: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryNeon.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = PrimaryNeon,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "WhatsApp Not Linked",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Link your WhatsApp to see your contacts with business detection",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLinkWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Link, null, tint = SpaceBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Link WhatsApp", color = SpaceBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ErrorNeon,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = TextMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
            ) {
                Text("Retry", color = SpaceBlack)
            }
        }
    }
}

@Composable
private fun ContactsContent(
    contacts: List<WhatsAppContact>,
    businessCount: Int,
    personalCount: Int,
    totalCount: Int,
    nonWhatsAppCount: Int,
    selectedTab: ContactsTab,
    onTabSelected: (ContactsTab) -> Unit,
    businessDetectionProgress: BusinessDetectionProgress? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 2026 Best Practice: Animated category cards with populating effect
        AnimatedCategoryCards(
            totalCount = totalCount,
            businessCount = businessCount,
            personalCount = personalCount,
            nonWhatsAppCount = nonWhatsAppCount,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            businessDetectionProgress = businessDetectionProgress
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = PrimaryNeon
        ) {
            Tab(
                selected = selectedTab == ContactsTab.ALL,
                onClick = { onTabSelected(ContactsTab.ALL) },
                text = { Text("All ($totalCount)") },
                selectedContentColor = PrimaryNeon,
                unselectedContentColor = TextMedium
            )
            Tab(
                selected = selectedTab == ContactsTab.BUSINESS,
                onClick = { onTabSelected(ContactsTab.BUSINESS) },
                text = { Text("Business ($businessCount)") },
                selectedContentColor = SecondaryNeon,
                unselectedContentColor = TextMedium
            )
            Tab(
                selected = selectedTab == ContactsTab.PERSONAL,
                onClick = { onTabSelected(ContactsTab.PERSONAL) },
                text = { Text("Personal ($personalCount)") },
                selectedContentColor = PrimaryNeon,
                unselectedContentColor = TextMedium
            )
        }

        // Contacts list
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = TextLow,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No contacts in this category",
                        color = TextMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Contacts will appear after WhatsApp syncs",
                        color = TextLow,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts, key = { it.jid }) { contact ->
                    ContactCard(contact = contact)
                }
            }
        }
    }
}

/**
 * 2026 Best Practice: Animated category cards with populating count-up effect.
 * Users see numbers visibly increasing as business detection progresses.
 */
@Composable
private fun AnimatedCategoryCards(
    totalCount: Int,
    businessCount: Int,
    personalCount: Int,
    nonWhatsAppCount: Int,
    selectedTab: ContactsTab,
    onTabSelected: (ContactsTab) -> Unit,
    businessDetectionProgress: BusinessDetectionProgress?
) {
    val isDetecting = businessDetectionProgress?.inProgress == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row with WhatsApp Personal, Business, and Non-WhatsApp cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // WhatsApp Personal Card
            AnimatedCountingCard(
                modifier = Modifier.weight(1f),
                count = personalCount,
                label = "WhatsApp",
                subtitle = "Personal",
                icon = Icons.Default.Person,
                color = PrimaryNeon,
                isSelected = selectedTab == ContactsTab.PERSONAL,
                isAnimating = isDetecting,
                onClick = { onTabSelected(ContactsTab.PERSONAL) }
            )

            // WhatsApp Business Card
            AnimatedCountingCard(
                modifier = Modifier.weight(1f),
                count = businessCount,
                label = "WhatsApp",
                subtitle = "Business",
                icon = Icons.Default.Business,
                color = SecondaryNeon,
                isSelected = selectedTab == ContactsTab.BUSINESS,
                isAnimating = isDetecting,
                onClick = { onTabSelected(ContactsTab.BUSINESS) }
            )

            // Non-WhatsApp Card (Phone contacts without WhatsApp)
            AnimatedCountingCard(
                modifier = Modifier.weight(1f),
                count = nonWhatsAppCount,
                label = "Phone",
                subtitle = "Non-WhatsApp",
                icon = Icons.Default.PhoneDisabled,
                color = TextMedium,
                isSelected = false,
                isAnimating = false,
                onClick = { /* Non-clickable - informational only */ }
            )
        }

        // Detection progress indicator (compact)
        if (isDetecting && businessDetectionProgress != null) {
            val progressPercent = if (businessDetectionProgress.total > 0) {
                (businessDetectionProgress.checked.toFloat() / businessDetectionProgress.total * 100).toInt()
            } else 0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DeepSpace
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pulsing indicator
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryNeon.copy(alpha = alpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scanning contacts...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMedium
                            )
                        }
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (businessDetectionProgress.total > 0) businessDetectionProgress.checked.toFloat() / businessDetectionProgress.total else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SecondaryNeon,
                        trackColor = SpaceBlack
                    )
                }
            }
        }

        // All contacts card (full width)
        AnimatedCountingCard(
            modifier = Modifier.fillMaxWidth(),
            count = totalCount,
            label = "All WhatsApp",
            subtitle = "Contacts",
            icon = Icons.Default.Contacts,
            color = Color.White,
            isSelected = selectedTab == ContactsTab.ALL,
            isAnimating = false,
            onClick = { onTabSelected(ContactsTab.ALL) },
            compact = true
        )
    }
}

/**
 * 2026 Best Practice: Card with animated count-up effect.
 * Number animates smoothly when value changes.
 */
@Composable
private fun AnimatedCountingCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    isAnimating: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    // 2026 Best Practice: Continuous interpolation animation
    // Smoothly counts up digit by digit instead of jumping
    var displayedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(count) {
        if (displayedCount == 0 && count > 0) {
            // Initial load - animate from 0 to count
            val steps = minOf(count, 60) // Max 60 steps for ~1 second animation
            val increment = maxOf(1, count / steps)
            while (displayedCount < count) {
                displayedCount = minOf(displayedCount + increment, count)
                kotlinx.coroutines.delay(16) // ~60fps
            }
        } else if (count > displayedCount) {
            // Incremental update - smoothly count up
            val difference = count - displayedCount
            val steps = minOf(difference, 30) // Max 30 steps for updates
            val increment = maxOf(1, difference / steps)
            while (displayedCount < count) {
                displayedCount = minOf(displayedCount + increment, count)
                kotlinx.coroutines.delay(16) // ~60fps
            }
        } else {
            displayedCount = count
        }
    }

    // Pulsing glow effect when animating
    val infiniteTransition = rememberInfiniteTransition(label = "cardPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else DeepSpace,
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Box {
            // Animated glow overlay when detecting
            if (isAnimating) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color.copy(alpha = glowAlpha),
                            RoundedCornerShape(16.dp)
                        )
                )
            }

            if (compact) {
                // Compact horizontal layout for "All" card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMedium
                            )
                        }
                    }
                    Text(
                        text = formatCount(displayedCount),
                        style = MaterialTheme.typography.headlineMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Full card layout
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Animated count with large font
                    Text(
                        text = formatCount(displayedCount),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp
                        ),
                        color = color,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Labels
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Format count with K/M suffixes for large numbers.
 * 2026 Best Practice: Use Kotlin multiplatform-compatible formatting (no String.format)
 */
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            val value = count / 1_000_000.0
            "${((value * 10).toInt() / 10.0)}M"
        }
        count >= 1_000 -> {
            val value = count / 1_000.0
            "${((value * 10).toInt() / 10.0)}K"
        }
        else -> count.toString()
    }
}

@Composable
private fun StatItem(value: Int, label: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMedium
        )
    }
}

/**
 * 2026 Best Practice: Progress banner for background business detection.
 * Shows user the detection progress with percentage and counts.
 */
@Composable
private fun BusinessDetectionProgressBanner(progress: BusinessDetectionProgress) {
    val progressPercent = if (progress.total > 0) {
        (progress.checked.toFloat() / progress.total * 100).toInt()
    } else 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = SecondaryNeon.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SecondaryNeon,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detecting businesses...",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryNeon,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${progress.checked} / ${progress.total} ($progressPercent%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (progress.total > 0) progress.checked.toFloat() / progress.total else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = SecondaryNeon,
                trackColor = DeepSpace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${progress.businessCount} businesses found so far",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow
            )
        }
    }
}

@Composable
private fun ContactCard(contact: WhatsAppContact) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DeepSpace
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (contact.isBusiness) SecondaryNeon.copy(alpha = 0.2f)
                        else PrimaryNeon.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (contact.isBusiness) Icons.Default.Business else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (contact.isBusiness) SecondaryNeon else PrimaryNeon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name ?: contact.pushName ?: contact.phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )

                val category = contact.businessProfile?.category
                if (contact.isBusiness && category != null) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryNeon
                    )
                }
            }

            // Business badge
            if (contact.isBusiness) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SecondaryNeon.copy(alpha = 0.2f)
                ) {
                    Text(
                        "Business",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryNeon
                    )
                }
            }
        }
    }
}
