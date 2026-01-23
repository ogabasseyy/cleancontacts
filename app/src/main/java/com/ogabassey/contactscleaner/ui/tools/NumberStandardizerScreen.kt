package com.ogabassey.contactscleaner.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.hilt.navigation.compose.hiltViewModel
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel
import com.ogabassey.contactscleaner.ui.results.ResultsUiState
import com.ogabassey.contactscleaner.ui.results.FormatGroup
import com.ogabassey.contactscleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberStandardizerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExport: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val formatGroups by viewModel.formatGroups.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // State to track expanded countries
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        viewModel.loadFormatGroups()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Standardize Numbers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            
            // Header Info
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSpaceElevated),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Optimization Found", color = SecondaryNeon, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${viewModel.scanResult?.formatIssueCount ?: 0} contacts have local formatting (e.g. 080...) but should be international (+234...).",
                        color = TextMedium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Action Button
            Button(
                onClick = { 
                    viewModel.performStandardizationAll()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = SpaceBlack)
            ) {
                Text("FIX ALL FORMATS", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Expandable List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                formatGroups.forEach { group ->
                    item(key = group.region) {
                        val isExpanded = expandedGroups[group.region] ?: false
                        
                        CountryHeader(
                            title = group.region,
                            count = group.contacts.size,
                            isExpanded = isExpanded,
                            onToggle = { expandedGroups[group.region] = !isExpanded },
                            onShare = onNavigateToExport
                        )
                    }

                    if (expandedGroups[group.region] == true) {
                        items(group.contacts, key = { "contact_${it.id}" }) { contact ->
                            StandardContactItem(contact)
                        }
                    }
                }
            }
        }

        // Processing Overlay
        if (uiState is ResultsUiState.Processing) {
            val progress = (uiState as ResultsUiState.Processing).progress
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(progress = { progress }, color = PrimaryNeon)
            }
        }
    }
}

@Composable
fun CountryHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        onClick = onToggle,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown 
                             else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = SecondaryNeon,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                color = SecondaryNeon,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            Surface(
                color = SecondaryNeon.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$count",
                    color = SecondaryNeon,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Share, 
                    contentDescription = "Share", 
                    tint = TextLow,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StandardContactItem(contact: com.ogabassey.contactscleaner.domain.model.Contact) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceSpaceElevated.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name ?: "Unknown", color = TextHigh, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(contact.numbers.firstOrNull() ?: "", color = ErrorNeon.copy(alpha = 0.8f), fontSize = 11.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, 
                         tint = TextLow, modifier = Modifier.size(12.dp).padding(horizontal = 2.dp))
                    Text(contact.normalizedNumber ?: "", color = SuccessNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Default.Check, null, tint = SecondaryNeon.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}
