package com.ogabassey.contactscleaner.ui.tools

import android.content.Intent
import com.ogabassey.contactscleaner.ui.theme.PrimaryNeon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.ui.theme.BackgroundDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is ExportUiState.Success) {
            val uri = (uiState as ExportUiState.Success).fileUri
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Contacts CSV"))
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Contacts", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Select Groups to Export",
                style = MaterialTheme.typography.titleMedium,
                color = com.ogabassey.contactscleaner.ui.theme.TextMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val options = listOf(
                ContactType.ALL to "All Contacts",
                ContactType.WHATSAPP to "WhatsApp Contacts",
                ContactType.TELEGRAM to "Telegram Contacts",
                ContactType.NON_WHATSAPP to "Non-WhatsApp Contacts",
                ContactType.JUNK to "Junk Contacts"
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(options.size) { index ->
                    val (type, label) = options[index]
                    val isSelected = selectedTypes.contains(type)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(if (isSelected) PrimaryNeon.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { viewModel.toggleType(type) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleType(type) },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryNeon)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.export() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                enabled = uiState !is ExportUiState.Loading && selectedTypes.isNotEmpty()
            ) {
                if (uiState is ExportUiState.Loading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to CSV", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
