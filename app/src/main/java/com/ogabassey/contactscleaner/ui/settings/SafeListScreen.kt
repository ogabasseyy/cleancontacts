package com.ogabassey.contactscleaner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel
import com.ogabassey.contactscleaner.ui.components.glassy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeListScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResultsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val ignoredContacts by viewModel.ignoredContacts.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safe List", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Header Info
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = WarningNeon.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningNeon.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = WarningNeon)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Contacts here are protected from cleanup scans and duplicate detection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHigh
                    )
                }
            }

            if (ignoredContacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, null, tint = TextLow, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Your Safe List is empty", color = TextLow)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(ignoredContacts) { contact ->
                        SafeContactItem(
                            contact = contact,
                            onRemove = { viewModel.unignoreContact(contact.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafeContactItem(
    contact: com.ogabassey.contactscleaner.data.db.entity.IgnoredContact,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassy(radius = 16.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.displayName, color = TextHigh, fontWeight = FontWeight.Bold)
            Text(contact.reason, color = WarningNeon, fontSize = 12.sp)
        }
        
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, "Remove", tint = ErrorNeon.copy(alpha = 0.7f))
        }
    }
}
