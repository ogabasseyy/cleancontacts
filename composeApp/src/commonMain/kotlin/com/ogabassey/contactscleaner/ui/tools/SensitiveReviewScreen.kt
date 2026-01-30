package com.ogabassey.contactscleaner.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for reviewing detected Sensitive Data (PII).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensitiveReviewScreen(
    viewModel: ReviewViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToSafeList: () -> Unit = {}
) {
    val contacts by viewModel.contacts.collectAsState()
    val addedContactName by viewModel.addedToSafeListContact.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadContacts(ContactType.SENSITIVE)
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Review Sensitive Data",
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
                    IconButton(onClick = onNavigateToSafeList) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Safe List",
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
        // Confirmation Dialog
        addedContactName?.let { contactName ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissConfirmation() },
                containerColor = SurfaceSpaceElevated,
                title = {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryNeon.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = PrimaryNeon,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "has been added to the safe list.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissConfirmation()
                            onNavigateToSafeList()
                        }
                    ) {
                        Text("View Safe List", color = PrimaryNeon)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissConfirmation() }) {
                        Text("Continue", color = TextMedium)
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Warning Banner
            Surface(
                color = WarningNeon.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningNeon)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "We found contacts that look like ID numbers (NIN, BVN, etc). Check them before deleting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningNeon
                    )
                }
            }

            if (contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sensitive data found", color = TextMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        SensitiveContactCard(
                            contact = contact,
                            onIgnore = { viewModel.ignoreContact(contact) },
                            onDismiss = { viewModel.removeFromList(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensitiveContactCard(
    contact: Contact,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSpaceElevated
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WarningNeon.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        contact.name?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarningNeon,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        contact.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        contact.sensitiveDescription ?: "Potential Sensitive Data",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningNeon
                    )
                    Text(
                        contact.normalizedNumber ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = TextMedium
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Skip")
                }
                Button(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = SpaceBlack
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keep Safe")
                }
            }
        }
    }
}
