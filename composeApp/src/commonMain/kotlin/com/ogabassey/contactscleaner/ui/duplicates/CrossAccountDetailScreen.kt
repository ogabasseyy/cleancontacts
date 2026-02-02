package com.ogabassey.contactscleaner.ui.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.domain.model.AccountInstance
import com.ogabassey.contactscleaner.domain.model.CrossAccountContact
import com.ogabassey.contactscleaner.ui.components.VerticalScrollBar
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.util.formatWithCommas
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossAccountDetailScreen(
    viewModel: CrossAccountViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.crossAccountContacts.collectAsState()
    val selectedKeys by viewModel.selectedMatchingKeys.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val selectedAccountToKeep by viewModel.selectedAccountToKeep.collectAsState()

    var showBulkConsolidateDialog by remember { mutableStateOf(false) }
    var bulkSelectedAccount by remember { mutableStateOf<AccountInstance?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.loadCrossAccountContacts()
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Cross-Account Duplicates",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${contacts.size} Contacts",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryNeon
                        )
                    }
                },
                actions = {
                    // Select All / Clear Selection
                    if (contacts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedKeys.size == contacts.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll()
                                }
                            }
                        ) {
                            Text(
                                if (selectedKeys.size == contacts.size) "CLEAR" else "SELECT ALL",
                                color = PrimaryNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            // Show FAB when items are selected
            if (selectedKeys.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBulkConsolidateDialog = true },
                    containerColor = PrimaryNeon,
                    contentColor = SpaceBlack
                ) {
                    Icon(Icons.Default.MergeType, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CONSOLIDATE (${selectedKeys.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Info Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = SecondaryNeon.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = SecondaryNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "These contacts exist in multiple accounts. Tap to choose which account to keep.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMedium
                        )
                    }
                }

                // List Content
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState is CrossAccountUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SecondaryNeon)
                            }
                        }
                        contacts.isEmpty() && uiState is CrossAccountUiState.Success -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessNeon,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No cross-account duplicates found",
                                        color = TextMedium
                                    )
                                }
                            }
                        }
                        else -> {
                            val listState = rememberLazyListState()
                            // 2026 Fix: Use remember to avoid re-sorting on every recomposition
                            val sortedContacts = remember(contacts) {
                                contacts.sortedBy { it.name ?: "" }
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        sortedContacts,
                                        key = { it.matchingKey }
                                    ) { contact ->
                                        CrossAccountContactItem(
                                            contact = contact,
                                            isSelected = selectedKeys.contains(contact.matchingKey),
                                            onSelectToggle = { viewModel.toggleSelection(contact.matchingKey) },
                                            onContactClick = { viewModel.setSelectedContact(contact) }
                                        )
                                    }
                                }

                                VerticalScrollBar(
                                    listState = listState,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Processing Overlay
            if (uiState is CrossAccountUiState.Processing) {
                val state = uiState as CrossAccountUiState.Processing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryNeon)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.message ?: "Processing...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${(state.progress * 100).toInt()}%",
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet for Single Contact Consolidation
    // 2026 Best Practice: Use let{} to safely capture non-null value instead of !!
    selectedContact?.let { contact ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.setSelectedContact(null) },
            sheetState = sheetState,
            containerColor = SurfaceSpaceElevated
        ) {
            SingleContactConsolidationSheet(
                contact = contact,
                selectedAccount = selectedAccountToKeep,
                onAccountSelected = { viewModel.setSelectedAccountToKeep(it) },
                onConsolidate = { viewModel.consolidateSingleContact() },
                onCancel = { viewModel.setSelectedContact(null) }
            )
        }
    }

    // Bulk Consolidation Dialog
    if (showBulkConsolidateDialog) {
        // 2026 Fix: Use remember to avoid calling getAllUniqueAccounts() on every recomposition
        val allAccounts = remember(contacts) {
            viewModel.getAllUniqueAccounts()
        }

        AlertDialog(
            onDismissRequest = { showBulkConsolidateDialog = false },
            containerColor = SurfaceSpaceElevated,
            title = {
                Text(
                    "Consolidate ${selectedKeys.size} Contacts",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Select which account to keep these contacts in:",
                        color = TextMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allAccounts) { account ->
                            AccountSelectionItem(
                                account = account,
                                isSelected = bulkSelectedAccount == account,
                                onSelect = { bulkSelectedAccount = account }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        bulkSelectedAccount?.let { account ->
                            viewModel.consolidateSelectedContacts(
                                account.accountType,
                                account.accountName
                            )
                        }
                        showBulkConsolidateDialog = false
                        bulkSelectedAccount = null
                    },
                    enabled = bulkSelectedAccount != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = SpaceBlack
                    )
                ) {
                    Text("CONSOLIDATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBulkConsolidateDialog = false
                    bulkSelectedAccount = null
                }) {
                    Text("CANCEL", color = TextMedium)
                }
            }
        )
    }
}

@Composable
private fun CrossAccountContactItem(
    contact: CrossAccountContact,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onContactClick: () -> Unit
) {
    // 2026 Fix: Removed redundant AnimatedVisibility(visible = true)
    // Animation is handled by LazyColumn's item appearance
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassy(radius = 16.dp)
            .clickable { onContactClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onSelectToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryNeon,
                uncheckedColor = TextMedium,
                checkmarkColor = SpaceBlack
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SecondaryNeon.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.name?.take(1)?.uppercase() ?: "?",
                color = SecondaryNeon,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.name ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                contact.primaryNumber ?: contact.primaryEmail ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TextMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Account Badges
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                contact.accounts.forEach { account ->
                    AccountBadge(account)
                }
            }
        }

        // Arrow
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "View details",
            tint = TextMedium
        )
    }
}

/**
 * 2026 Best Practice: Extract shared account type styling to reduce duplication.
 * Used by both AccountBadge and AccountSelectionItem.
 *
 * Prefers stable accountType matching (e.g., "com.google", "com.apple") over
 * displayLabel string matching to avoid misclassification with localized labels.
 */
private fun getAccountStyle(
    accountType: String?,
    displayLabel: String
): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> {
    // First try matching by accountType (stable, not affected by localization)
    // Note: Only Gmail and iOS local accounts appear in cross-account duplicates
    accountType?.let { type ->
        when {
            type.contains("google", ignoreCase = true) ->
                return Pair(Color(0xFF4285F4), Icons.Default.Email)
            type.contains("apple", ignoreCase = true) || type.contains("icloud", ignoreCase = true) ->
                return Pair(Color(0xFF007AFF), Icons.Default.Cloud)
            type.contains("exchange", ignoreCase = true) || type.contains("microsoft", ignoreCase = true) ->
                return Pair(Color(0xFF0078D4), Icons.Default.Business)
        }
    }
    // null/empty accountType = iOS local contacts
    if (accountType.isNullOrEmpty()) {
        return Pair(Color(0xFF007AFF), Icons.Default.Phone)
    }

    // Fallback to displayLabel matching for unknown accountTypes
    return when {
        // Match Gmail emails (e.g., "user@gmail.com") or explicit Gmail/Google labels
        displayLabel.contains("@gmail.com", ignoreCase = true) ||
        displayLabel.contains("Gmail", ignoreCase = true) ||
        displayLabel.contains("Google", ignoreCase = true) ->
            Pair(Color(0xFF4285F4), Icons.Default.Email)
        displayLabel.contains("iCloud", ignoreCase = true) ->
            Pair(Color(0xFF007AFF), Icons.Default.Cloud)
        displayLabel.contains("iOS", ignoreCase = true) || displayLabel.contains("Local", ignoreCase = true) ->
            Pair(Color(0xFF007AFF), Icons.Default.Phone)
        displayLabel.contains("Exchange", ignoreCase = true) ->
            Pair(Color(0xFF0078D4), Icons.Default.Business)
        else -> Pair(TextMedium, Icons.Default.AccountCircle)
    }
}

@Composable
private fun AccountBadge(account: AccountInstance) {
    val (color, icon) = getAccountStyle(account.accountType, account.displayLabel)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                account.displayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SingleContactConsolidationSheet(
    contact: CrossAccountContact,
    selectedAccount: AccountInstance?,
    onAccountSelected: (AccountInstance) -> Unit,
    onConsolidate: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Text(
            contact.name ?: "Unknown Contact",
            style = MaterialTheme.typography.titleLarge,
            color = TextHigh,
            fontWeight = FontWeight.Bold
        )

        val primaryNumber = contact.primaryNumber
        if (primaryNumber != null) {
            Text(
                primaryNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMedium
            )
        }

        val primaryEmail = contact.primaryEmail
        if (primaryEmail != null) {
            Text(
                primaryEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section Header
        Text(
            "Keep contact in:",
            style = MaterialTheme.typography.titleMedium,
            color = TextHigh,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Account Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contact.accounts.forEach { account ->
                AccountSelectionItem(
                    account = account,
                    isSelected = selectedAccount == account,
                    onSelect = { onAccountSelected(account) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Text
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = WarningNeon.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningNeon,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "The contact will be removed from all other accounts and kept only in the selected account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onConsolidate,
                modifier = Modifier.weight(1f),
                enabled = selectedAccount != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryNeon,
                    contentColor = SpaceBlack
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CONSOLIDATE", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceSpace,
                    contentColor = TextMedium
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CANCEL", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AccountSelectionItem(
    account: AccountInstance,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (color, icon) = getAccountStyle(account.accountType, account.displayLabel)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else SurfaceSpace,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 2026 Fix: onClick = null to avoid duplicate invocation
            // (Surface.selectable already handles selection semantics)
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = color,
                    unselectedColor = TextMedium
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextHigh,
                    fontWeight = FontWeight.Medium
                )
                val accountName = account.accountName
                if (accountName != null) {
                    Text(
                        accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }
            }
        }
    }
}
