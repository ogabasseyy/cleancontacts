package com.ogabassey.contactscleaner.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.ui.components.VerticalScrollBar
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.ui.util.rememberContactLauncher
import com.ogabassey.contactscleaner.util.formatWithCommas
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    type: ContactType,
    viewModel: CategoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val groupContacts by viewModel.groupContacts.collectAsState()
    
    val contactLauncher = rememberContactLauncher()

    // Bottom sheet state for group details
    var selectedGroupKey by remember { mutableStateOf<String?>(null) }
    var customMergeName by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(type) {
        viewModel.loadCategory(type)
    }

    // Load group contacts when a group is selected
    LaunchedEffect(selectedGroupKey) {
        selectedGroupKey?.let { key ->
            viewModel.loadGroupContacts(key, type)
            // Pre-fill custom name with first contact's name
        }
    }

    // Pre-fill merge name when group contacts are loaded
    LaunchedEffect(groupContacts) {
        if (groupContacts.isNotEmpty() && selectedGroupKey != null) {
            customMergeName = groupContacts.firstOrNull()?.name ?: ""
        }
    }

    val isDuplicateType = type.name.startsWith("DUP_") || type == ContactType.DUPLICATE

    val title = when (type) {
        ContactType.JUNK -> "Junk Contacts"
        ContactType.DUPLICATE -> "Duplicate Contacts"
        ContactType.DUP_NUMBER -> "Duplicate Numbers"
        ContactType.DUP_EMAIL -> "Duplicate Emails"
        ContactType.DUP_NAME -> "Exact Names"
        ContactType.DUP_SIMILAR_NAME -> "Similar Names"
        ContactType.FORMAT_ISSUE -> "Format Issues"
        ContactType.WHATSAPP -> "WhatsApp Contacts"
        ContactType.TELEGRAM -> "Telegram Contacts"
        ContactType.SENSITIVE -> "Sensitive Data"
        ContactType.JUNK_NO_NAME -> "Missing Names"
        ContactType.JUNK_NO_NUMBER -> "Missing Numbers"
        ContactType.JUNK_INVALID_CHAR -> "Invalid Characters"
        ContactType.JUNK_LONG_NUMBER -> "Long Numbers"
        ContactType.JUNK_SHORT_NUMBER -> "Short Numbers"
        ContactType.JUNK_REPETITIVE -> "Repetitive Digits"
        ContactType.JUNK_SYMBOL -> "Symbolic Names"
        ContactType.JUNK_NUMERICAL_NAME -> "Numerical Names"
        ContactType.JUNK_EMOJI_NAME -> "Emoji Names"
        ContactType.JUNK_FANCY_FONT -> "Fancy Fonts"
        else -> "Contacts"
    }

    val accentColor = getColorForType(type)

    val countLabel = if (isDuplicateType) {
        "${duplicateGroups.size} Groups"
    } else {
        "${contacts.size} Items"
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            countLabel,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // ACTIONS Utility Bar at TOP (not in a separate tab)
                ResultsUtilityBar(
                    contactType = type,
                    onDeleteAll = { showConfirmationDialog = true },
                    onMergeAll = { showConfirmationDialog = true },
                    onExportAll = { /* TODO: Export */ }
                )

                // List Content
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState is CategoryUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = accentColor)
                            }
                        }
                        isDuplicateType -> {
                            // Show duplicate groups (not flat list)
                            val duplicateListState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                DuplicateGroupList(
                                    groups = duplicateGroups,
                                    accentColor = accentColor,
                                    listState = duplicateListState,
                                    onGroupClick = { group ->
                                        selectedGroupKey = group.groupKey
                                    }
                                )
                                
                                VerticalScrollBar(
                                    listState = duplicateListState,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 4.dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                        }
                        contacts.isEmpty() && uiState is CategoryUiState.Success -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No contacts found", color = TextMedium)
                            }
                        }
                        else -> {
                            // Show flat contact list for non-duplicate types
                            val listState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 20.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(contacts.sortedBy { it.name ?: "" }, key = { it.id }) { contact ->
                                        ContactListItem(
                                            contact = contact,
                                            accentColor = accentColor,
                                            isFormatType = type == ContactType.FORMAT_ISSUE,
                                            onContactClick = { contactLauncher.openContact(it.id.toString()) },
                                            onDeleteContact = { contactToDelete = it },
                                            onEditContact = { contactLauncher.openContact(it.id.toString()) }
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
            if (uiState is CategoryUiState.Processing) {
                val state = uiState as CategoryUiState.Processing
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

    // Bottom Sheet for Group Details (with merge name selection)
    if (selectedGroupKey != null) {
        ModalBottomSheet(
            onDismissRequest = {
                selectedGroupKey = null
                customMergeName = ""
                viewModel.clearGroupContacts()
            },
            sheetState = sheetState,
            containerColor = SurfaceSpaceElevated
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Group Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextHigh,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { /* TODO: Export group */ },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Group key display
                Text(
                    "Matching: $selectedGroupKey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Name TextField for Merged Contact
                if (isDuplicateType) {
                    OutlinedTextField(
                        value = customMergeName,
                        onValueChange = { customMergeName = it },
                        label = { Text("Name for Merged Contact", color = TextMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextHigh,
                            unfocusedTextColor = TextHigh,
                            cursorColor = PrimaryNeon,
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = PrimaryNeon,
                            focusedLabelColor = PrimaryNeon,
                            unfocusedLabelColor = TextMedium
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Contacts in Group
                if (groupContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryNeon)
                    }
                } else {
                    Text(
                        "${groupContacts.size.formatWithCommas()} contacts in this group",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupContacts.sortedBy { it.name ?: "" }, key = { it.id }) { contact ->
                            ContactListItem(
                                contact = contact,
                                accentColor = accentColor,
                                isFormatType = false,
                                onContactClick = { contactLauncher.openContact(it.id.toString()) },
                                onDeleteContact = { contactToDelete = it },
                                onEditContact = { contactLauncher.openContact(it.id.toString()) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isDuplicateType) {
                        Button(
                            onClick = {
                                val ids = groupContacts.map { it.id }
                                viewModel.performSingleMerge(ids, customMergeName, type)
                                selectedGroupKey = null
                                customMergeName = ""
                                viewModel.clearGroupContacts()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryNeon,
                                contentColor = SpaceBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = groupContacts.isNotEmpty()
                        ) {
                            Text("MERGE", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            selectedGroupKey = null
                            customMergeName = ""
                            viewModel.clearGroupContacts()
                        },
                        modifier = if (isDuplicateType) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceSpace,
                            contentColor = TextMedium
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CLOSE", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmationDialog) {
        val actionText = when {
            type.name.startsWith("DUP") || type == ContactType.DUPLICATE -> "merge"
            type == ContactType.FORMAT_ISSUE -> "fix"
            else -> "delete"
        }
        
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            containerColor = SurfaceSpaceElevated,
            title = { 
                Text(
                    "Confirm Action", 
                    color = Color.White,
                    fontWeight = FontWeight.Bold 
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to $actionText all contacts in this category? \n\nYou can undo this action later from the History tab.",
                    color = TextMedium
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        viewModel.performAction(type)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionText == "delete") ErrorNeon else PrimaryNeon,
                        contentColor = SpaceBlack
                    )
                ) {
                    Text(actionText.uppercase(), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("CANCEL", color = TextMedium)
                }
            }
        )
    }

    // Single Contact Deletion Confirmation
    if (contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            containerColor = SurfaceSpaceElevated,
            title = { Text("Delete Contact?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${contactToDelete?.name ?: "this contact"}?", color = TextMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        contactToDelete?.let { viewModel.deleteSingleContact(it, type) }
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorNeon, contentColor = SpaceBlack)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("CANCEL", color = TextMedium)
                }
            }
        )
    }
}

@Composable
private fun ResultsUtilityBar(
    contactType: ContactType,
    onDeleteAll: () -> Unit = {},
    onMergeAll: () -> Unit = {},
    onExportAll: () -> Unit = {}
) {
    val isDuplicateFilter = contactType.name.startsWith("DUP") && contactType != ContactType.DUPLICATE
    val canDelete = contactType.name.startsWith("JUNK") || contactType == ContactType.NON_WHATSAPP
    val isFormat = contactType == ContactType.FORMAT_ISSUE
    val accentColor = getColorForType(contactType)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .glassy(radius = 12.dp)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ACTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = TextMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Export Action
            TextButton(
                onClick = onExportAll,
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EXPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Primary Action (Merge or Delete or Fix)
            when {
                isDuplicateFilter || contactType == ContactType.DUPLICATE -> {
                    Button(
                        onClick = onMergeAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = SpaceBlack
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("MERGE ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                canDelete -> {
                    Button(
                        onClick = onDeleteAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorNeon,
                            contentColor = SpaceBlack
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DELETE ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                isFormat -> {
                    Button(
                        onClick = onDeleteAll, // Uses performAction which handles FORMAT_ISSUE
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryNeon,
                            contentColor = SpaceBlack
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("FIX ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupList(
    groups: List<DuplicateGroupSummary>,
    accentColor: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onGroupClick: (DuplicateGroupSummary) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No duplicate groups found.", color = TextLow)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(groups.sortedBy { it.groupKey }, key = { it.groupKey }) { group ->
                DuplicateGroupItem(group, accentColor, onGroupClick)
            }
        }
    }
}

@Composable
private fun DuplicateGroupItem(
    group: DuplicateGroupSummary,
    accentColor: Color,
    onGroupClick: (DuplicateGroupSummary) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassy(radius = 16.dp)
                .clickable { onGroupClick(group) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    group.count.toString(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.groupKey,
                    color = TextHigh,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (group.previewNames.isNotEmpty()) {
                    Text(
                        group.previewNames,
                        color = TextMedium,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "View", tint = TextMedium)
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    accentColor: Color,
    isFormatType: Boolean = false,
    onContactClick: (Contact) -> Unit = {},
    onDeleteContact: (Contact) -> Unit = {},
    onEditContact: (Contact) -> Unit = {}
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassy(radius = 16.dp)
                .clickable { onContactClick(contact) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name?.take(1)?.uppercase() ?: "?",
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                val normalized = contact.normalizedNumber
                if (isFormatType && normalized != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            contact.numbers.firstOrNull() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMedium
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(horizontal = 4.dp),
                            tint = SecondaryNeon
                        )
                        Text(
                            normalized,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        contact.normalizedNumber ?: contact.numbers.firstOrNull() ?: "No Number",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }
            }

            // Action Icons (Hold & Pencil)
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit (Pencil) Icon
                IconButton(
                    onClick = { onEditContact(contact) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextMedium.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete (Trash) Icon
                IconButton(
                    onClick = { onDeleteContact(contact) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorNeon.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getColorForType(type: ContactType): Color {
    return when {
        type.name.startsWith("JUNK") || type == ContactType.JUNK_FANCY_FONT -> ErrorNeon
        type.name.startsWith("DUP") -> WarningNeon
        type == ContactType.WHATSAPP -> SuccessNeon
        type == ContactType.TELEGRAM -> SecondaryNeon
        type == ContactType.SENSITIVE -> WarningNeon
        type == ContactType.FORMAT_ISSUE -> SecondaryNeon
        else -> PrimaryNeon
    }
}
