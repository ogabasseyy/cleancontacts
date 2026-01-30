package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.selectTab(it) }
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
    selectedTab: ContactsTab,
    onTabSelected: (ContactsTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = DeepSpace
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = totalCount, label = "Total")
                StatItem(value = businessCount, label = "Business", color = SecondaryNeon)
                StatItem(value = personalCount, label = "Personal", color = PrimaryNeon)
            }
        }

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
