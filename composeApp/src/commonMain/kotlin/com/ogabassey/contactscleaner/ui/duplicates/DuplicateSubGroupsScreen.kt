package com.ogabassey.contactscleaner.ui.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.ui.results.ResultsViewModel
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.util.formatWithCommas
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateSubGroupsScreen(
    viewModel: ResultsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (ContactType) -> Unit = {}
) {
    val scanResultState by viewModel.scanResult.collectAsState()
    val scanResult = scanResultState

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Duplicate Groups",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (scanResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SpaceBlack),
                contentAlignment = Alignment.Center
            ) {
                Text("No data available", color = TextMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "SELECT GROUP TYPE",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    SubGroupCard(
                        title = "Duplicate Numbers",
                        count = scanResult.numberDuplicateCount,
                        icon = Icons.Default.Phone,
                        color = SecondaryNeon,
                        description = "Contacts sharing the same phone numbers",
                        onClick = { onNavigateToDetail(ContactType.DUP_NUMBER) }
                    )
                }

                item {
                    SubGroupCard(
                        title = "Duplicate Emails",
                        count = scanResult.emailDuplicateCount,
                        icon = Icons.Default.Email,
                        color = WarningNeon,
                        description = "Contacts sharing the same email addresses",
                        onClick = { onNavigateToDetail(ContactType.DUP_EMAIL) }
                    )
                }

                item {
                    SubGroupCard(
                        title = "Exact Names",
                        count = scanResult.nameDuplicateCount,
                        icon = Icons.Default.Person,
                        color = PrimaryNeon,
                        description = "Contacts with identical display names",
                        onClick = { onNavigateToDetail(ContactType.DUP_NAME) }
                    )
                }

                item {
                    SubGroupCard(
                        title = "Similar Names",
                        count = scanResult.similarNameCount,
                        icon = Icons.Default.Face,
                        color = WarningNeon,
                        description = "Names that differ by only a few characters",
                        onClick = { onNavigateToDetail(ContactType.DUP_SIMILAR_NAME) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SubGroupCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            @Suppress("DEPRECATION")
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextMedium)
            }
            Text(
                count.formatWithCommas(),
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextLow)
        }
    }
}
