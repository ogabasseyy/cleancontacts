package com.ogabassey.contactscleaner.ui.category

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.ogabassey.contactscleaner.ui.util.rememberContactLauncher
import com.ogabassey.contactscleaner.util.ExportFormat
import com.ogabassey.contactscleaner.util.formatWithCommas
import com.ogabassey.contactscleaner.util.isIOS
import com.ogabassey.contactscleaner.util.rememberShareLauncher
import org.koin.compose.viewmodel.koinViewModel

/**
 * 2026 Best Practice: Extract platform-specific contact ID resolution to reduce duplication.
 * iOS requires platform_uid for CNContactStore lookup, Android uses numeric ID.
 */
private fun Contact.getTargetId(): String = if (isIOS) platform_uid ?: id.toString() else id.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    type: ContactType,
    viewModel: CategoryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val shareLauncher = rememberShareLauncher()
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val groupContacts by viewModel.groupContacts.collectAsState()
    val deletingContactId by viewModel.deletingContactId.collectAsState()
    val exportData by viewModel.exportData.collectAsState()

    // Track export format for proper file extension
    var lastExportFormat by remember { mutableStateOf(ExportFormat.CSV) }

    // 2026 Best Practice: Refresh contacts when returning from native Contacts app
    val contactLauncher = rememberContactLauncher(
        onReturn = { viewModel.loadCategory(type) }
    )

    // Bottom sheet state for group details
    var selectedGroupKey by remember { mutableStateOf<String?>(null) }
    var customMergeName by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var isGroupExport by remember { mutableStateOf(false) }
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
                    onExportAll = {
                        isGroupExport = false
                        showExportFormatDialog = true
                    }
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
                                            onContactClick = { c -> contactLauncher.openContact(c.getTargetId()) },
                                            onDeleteContact = { contactToDelete = it },
                                            onEditContact = { c -> contactLauncher.openContact(c.getTargetId()) }
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
                        onClick = {
                            isGroupExport = true
                            showExportFormatDialog = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                onContactClick = { c -> contactLauncher.openContact(c.getTargetId()) },
                                onDeleteContact = { contactToDelete = it },
                                onEditContact = { c -> contactLauncher.openContact(c.getTargetId()) }
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
        val isDeleting = deletingContactId == contactToDelete?.id
        val hasError = uiState is CategoryUiState.Error
        val errorMessage = (uiState as? CategoryUiState.Error)?.message

        // 2026 Best Practice: Auto-dismiss dialog when deletion completes successfully
        LaunchedEffect(deletingContactId, uiState) {
            if (contactToDelete != null && deletingContactId == null && uiState is CategoryUiState.Success) {
                contactToDelete = null
            }
        }

        AlertDialog(
            onDismissRequest = {
                // Only allow dismiss if not currently deleting
                if (!isDeleting) {
                    contactToDelete = null
                    if (hasError) viewModel.resetState()
                }
            },
            containerColor = SurfaceSpaceElevated,
            title = {
                Text(
                    if (hasError) "Delete Failed" else "Delete Contact?",
                    color = if (hasError) ErrorNeon else Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                when {
                    hasError -> {
                        Column {
                            Text(
                                errorMessage ?: "Failed to delete contact",
                                color = ErrorNeon.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Please check permissions and try again.",
                                color = TextMedium,
                                fontSize = 12.sp
                            )
                        }
                    }
                    isDeleting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryNeon,
                                strokeWidth = 2.dp
                            )
                            Text("Deleting contact...", color = TextMedium)
                        }
                    }
                    else -> {
                        Text(
                            "Are you sure you want to delete ${contactToDelete?.name ?: "this contact"}?",
                            color = TextMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (hasError) {
                    Button(
                        onClick = {
                            viewModel.resetState()
                            contactToDelete?.let { viewModel.deleteSingleContact(it, type) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorNeon, contentColor = SpaceBlack)
                    ) {
                        Text("RETRY", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            contactToDelete?.let { viewModel.deleteSingleContact(it, type) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorNeon, contentColor = SpaceBlack),
                        enabled = !isDeleting
                    ) {
                        Text(if (isDeleting) "DELETING..." else "DELETE", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        contactToDelete = null
                        if (hasError) viewModel.resetState()
                    },
                    enabled = !isDeleting
                ) {
                    Text(
                        if (hasError) "CLOSE" else "CANCEL",
                        color = if (isDeleting) TextLow else TextMedium
                    )
                }
            }
        )
    }

    // Export Format Selection Dialog
    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            containerColor = SurfaceSpaceElevated,
            title = {
                Text(
                    "Export Format",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose the export format:",
                        color = TextMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // CSV Option
                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            lastExportFormat = ExportFormat.CSV
                            if (isGroupExport) {
                                viewModel.exportGroupToCsv()
                            } else {
                                viewModel.exportToCsv()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CSV (Spreadsheet)", fontWeight = FontWeight.Bold)
                    }

                    // vCard Option
                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            lastExportFormat = ExportFormat.VCARD
                            if (isGroupExport) {
                                viewModel.exportGroupToVCard()
                            } else {
                                viewModel.exportToVCard()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryNeon,
                            contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("vCard (Contact File)", fontWeight = FontWeight.Bold)
                    }

                    // Excel-Compatible Option (CSV with Excel MIME type)
                    Button(
                        onClick = {
                            showExportFormatDialog = false
                            lastExportFormat = ExportFormat.EXCEL_COMPATIBLE
                            if (isGroupExport) {
                                viewModel.exportGroupToCsv()
                            } else {
                                viewModel.exportToCsv()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessNeon,
                            contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Excel Compatible", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text("CANCEL", color = TextMedium)
                }
            }
        )
    }

    // Export Data Dialog with Share options
    if (exportData != null) {
        val fileName = "contacts_export.${lastExportFormat.extension}"

        AlertDialog(
            onDismissRequest = { viewModel.clearExportData() },
            containerColor = SurfaceSpaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Export Ready",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Your contacts have been exported. Choose how to share:",
                        color = TextMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preview box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(SurfaceSpace, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            exportData?.take(300)?.let {
                                if ((exportData?.length ?: 0) > 300) "$it..." else it
                            } ?: "",
                            color = TextMedium,
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Share buttons grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Primary Share button
                        Button(
                            onClick = {
                                exportData?.let {
                                    shareLauncher.share(it, fileName, lastExportFormat.mimeType)
                                }
                                viewModel.clearExportData()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryNeon,
                                contentColor = SpaceBlack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SHARE", fontWeight = FontWeight.Bold)
                        }

                        // Google Sheets & Excel row (only for CSV/Excel formats)
                        if (lastExportFormat != ExportFormat.VCARD) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Google Sheets button
                                OutlinedButton(
                                    onClick = {
                                        exportData?.let {
                                            shareLauncher.openInGoogleSheets(it, fileName)
                                        }
                                        viewModel.clearExportData()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SuccessNeon
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(SuccessNeon)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Sheets", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Excel button
                                OutlinedButton(
                                    onClick = {
                                        exportData?.let {
                                            shareLauncher.openInExcel(it, fileName)
                                        }
                                        viewModel.clearExportData()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SecondaryNeon
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(SecondaryNeon)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Copy to clipboard
                        OutlinedButton(
                            onClick = {
                                exportData?.let { clipboardManager.setText(AnnotatedString(it)) }
                                viewModel.clearExportData()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextMedium
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Copy to Clipboard", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearExportData() }) {
                    Text("CLOSE", color = TextMedium)
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
    // 2026 Fix: Removed redundant AnimatedVisibility(visible = true)
    // Animation is handled by LazyColumn's item appearance
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

@Composable
private fun ContactListItem(
    contact: Contact,
    accentColor: Color,
    isFormatType: Boolean = false,
    onContactClick: (Contact) -> Unit = {},
    onDeleteContact: (Contact) -> Unit = {},
    onEditContact: (Contact) -> Unit = {}
) {
    // 2026 Fix: Removed redundant AnimatedVisibility(visible = true)
    // Animation is handled by LazyColumn's item appearance
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
