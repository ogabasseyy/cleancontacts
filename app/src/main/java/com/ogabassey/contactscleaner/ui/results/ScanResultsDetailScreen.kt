package com.ogabassey.contactscleaner.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsDetailScreen(
    contactType: ContactType,
    onNavigateBack: () -> Unit,
    viewModel: ResultsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedContacts = remember(contactType) { viewModel.getPagedContacts(contactType) }.collectAsLazyPagingItems()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val accountGroups by viewModel.accountGroups.collectAsState()
    val isPremium by viewModel.billingRepository.isPremium.collectAsState()
    val remainingActions by viewModel.freeActionsRemaining.collectAsState(2)
    
    // Unified Selection State for Bottom Sheet
    var selectedGroupKey by remember { mutableStateOf<String?>(null) }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }
    var groupContacts by remember { mutableStateOf<List<com.ogabassey.contactscleaner.domain.model.Contact>>(emptyList()) }
    var customName by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Handle Export Share Intent
    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { uri ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Contacts CSV"))
        }
    }

    // Fetch contacts when group selected
    LaunchedEffect(selectedGroupKey) {
        val key = selectedGroupKey
        if (key != null) {
            groupContacts = viewModel.getContactsInGroup(key, contactType)
            // Pre-fill custom name with the first contact's name if it's a duplicate group
            if (contactType.name.startsWith("DUP_") && groupContacts.isNotEmpty()) {
                customName = groupContacts.firstOrNull()?.name ?: ""
            }
        } else {
            groupContacts = emptyList()
            customName = ""
        }
    }
    
    val isDuplicateFilter = contactType.name.startsWith("DUP") && contactType != ContactType.DUPLICATE
    val isAccountFilter = contactType == ContactType.ACCOUNT
    
    LaunchedEffect(contactType) {
        if (isDuplicateFilter) {
            viewModel.loadDuplicateGroups(contactType)
        } else if (isAccountFilter) {
            viewModel.loadAccountGroups()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Check for Success State to show Snackbar
    LaunchedEffect(uiState) {
        if (uiState is ResultsUiState.Success) {
            val state = uiState as ResultsUiState.Success
            val result = snackbarHostState.showSnackbar(
                message = state.message,
                actionLabel = if (state.canUndo) "UNDO" else null,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                viewModel.undoLastAction()
            } else {
                 // Only reset if dismissed (not undone) to keep UI clean, 
                 // but typically we let the user see the result. 
                 // If we reset immediately it implies "done".
                 // viewModel.resetState() 
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(getFilterTitle(contactType), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        val countLabel = when {
                            isDuplicateFilter -> "${duplicateGroups.size} Groups"
                            isAccountFilter -> "${accountGroups.size} Accounts"
                            else -> "${pagedContacts.itemCount} Items"
                        }
                        Text(
                            countLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = PrimaryNeon)
                    }
                },
                actions = {
                    TrialStatusChip(
                        isPremium = isPremium,
                        remainingActions = remainingActions,
                        onClick = { viewModel.performBulkExport(contactType) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextHigh
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                
                // Free Trial Banner (if applicable)
                if (!isPremium && remainingActions > 0) {
                    FreeTrialBanner(
                        remainingActions = remainingActions,
                        onUpgradeClick = { viewModel.performBulkExport(contactType) }
                    )
                }

                // Utility Action Bar
                ResultsUtilityBar(
                    contactType = contactType,
                    onDeleteAll = { viewModel.performCleanup(contactType) },
                    onMergeAll = { viewModel.performMerge(contactType) },
                    onExportAll = { viewModel.performBulkExport(contactType) }
                )

                // List Content
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isDuplicateFilter -> DuplicateGroupList(
                            groups = duplicateGroups, 
                            accentColor = getColorForFilter(contactType),
                            onGroupClick = { 
                                selectedGroupKey = it.groupKey
                                selectedGroupName = it.groupKey
                            }
                        )
                        isAccountFilter -> AccountGroupList(
                            groups = accountGroups,
                            onGroupClick = {
                                selectedGroupKey = it.accountType
                                selectedGroupName = it.accountName ?: it.accountType
                            }
                        )
                        else -> ContactList(pagedContacts, getColorForFilter(contactType))
                    }
                }
            }
            
            StatusOverlay(uiState, viewModel)

            // Paywall Dialog
            if (uiState is ResultsUiState.ShowPaywall) {
                com.ogabassey.contactscleaner.ui.paywall.PaywallScreen(
                   onDismiss = { viewModel.retryPendingAction() }
                )
            }
        }
    }

    // Bottom Sheet for Group Details
    if (selectedGroupKey != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                selectedGroupKey = null
                selectedGroupName = null
            },
            containerColor = SurfaceSpaceElevated
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            selectedGroupName?.let { name ->
                                viewModel.performGroupExport(groupContacts, name)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT AS CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                if (contactType.name.startsWith("DUP_")) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Name for Merged Contact", color = TextMedium) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextHigh,
                            unfocusedTextColor = TextHigh,
                            cursorColor = PrimaryNeon,
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = GlassBorder
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                if (groupContacts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = PrimaryNeon)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupContacts.size) { index ->
                            com.ogabassey.contactscleaner.ui.results.ContactItem(groupContacts[index], getColorForFilter(contactType))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (contactType.name.startsWith("DUP_")) {
                        Button(
                            onClick = { 
                                val ids = groupContacts.map { it.id }
                                viewModel.performSingleMerge(ids, customName, contactType)
                                selectedGroupKey = null
                                selectedGroupName = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = SpaceBlack),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text("MERGE", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = { 
                           selectedGroupKey = null 
                           selectedGroupName = null
                        },
                        modifier = if (contactType.name.startsWith("DUP_")) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        colors = if (contactType.name.startsWith("DUP_")) 
                                    ButtonDefaults.buttonColors(containerColor = SurfaceSpace, contentColor = TextMedium) 
                                 else 
                                    ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = SpaceBlack),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text("CLOSE", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
