@file:OptIn(ExperimentalMaterial3Api::class)

package com.ogabassey.contactscleaner.ui.paywall
import com.ogabassey.contactscleaner.domain.model.Resource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    viewModel: PaywallViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPackageId by remember { mutableStateOf<String?>(null) }
    
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is PaywallUiState.Success) {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, enabled = uiState !is PaywallUiState.Loading) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // Header
                Text(
                    text = "Upgrade to",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "CleanContacts Pro",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Features
                FeatureRow("Delete junk contacts")
                FeatureRow("Merge duplicates")
                FeatureRow("Sync with Google Contacts")
                FeatureRow("Save imported contacts")
                FeatureRow("Priority support")

                Spacer(modifier = Modifier.height(32.dp))

                val resource by viewModel.packages.collectAsState()
                
                when (val state = resource) {
                    is Resource.Loading -> {
                        // Skeleton Loading UI (Show 3 placeholders)
                        repeat(3) {
                            PricingOptionSkeleton()
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        // Small text indicating what's happening
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Fetching best prices...", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    is Resource.Error -> {
                         // Empty state (failed to load or no offerings)
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Unable to load pricing info.\n${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Retry",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    is Resource.Success -> {
                        val packages = state.data
                         if (packages.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No packages available.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            // Valid packages
                            // Auto-select monthly logic can be moved to ViewModel or kept here as side-effect
                             LaunchedEffect(packages) {
                                if (selectedPackageId == null) {
                                    selectedPackageId = packages.find { it.identifier == "monthly" }?.id ?: packages.first().id
                                }
                            }
                            
                            packages.forEach { pkg ->
                                PricingOption(
                                    title = when(pkg.identifier) {
                                        "monthly" -> "Monthly"
                                        "annual" -> "Annual"
                                        "lifetime" -> "One-Time Purchase"
                                        else -> pkg.title
                                    },
                                    price = pkg.price,
                                    badge = if(pkg.identifier == "annual") "Save 44%" else null,
                                    isSelected = selectedPackageId == pkg.id,
                                    onClick = { selectedPackageId = pkg.id }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Purchase Button
                Button(
                    onClick = { 
                        selectedPackageId?.let { id ->
                            activity?.let { viewModel.purchasePackage(it, id) }
                        }
                    },
                    enabled = selectedPackageId != null && uiState !is PaywallUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState is PaywallUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { viewModel.restorePurchases() },
                    enabled = uiState !is PaywallUiState.Loading
                ) {
                    Text("Restore Purchases", style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    text = "Cancel anytime. Terms apply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Snackbar for errors (e.g. Restore failed)
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(uiState) {
            if (uiState is PaywallUiState.Error) {
                val message = (uiState as PaywallUiState.Error).message
                snackbarHostState.showSnackbar(message)
                viewModel.resetState()
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PricingOption(
    title: String,
    price: String,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge != null) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}