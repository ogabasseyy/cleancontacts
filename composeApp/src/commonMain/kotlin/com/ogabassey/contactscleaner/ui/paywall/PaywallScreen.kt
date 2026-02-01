package com.ogabassey.contactscleaner.ui.paywall

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.domain.model.Resource
import com.ogabassey.contactscleaner.platform.LegalUrls
import com.ogabassey.contactscleaner.platform.UrlOpener
import com.ogabassey.contactscleaner.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Paywall Screen for Compose Multiplatform.
 *
 * Displays premium subscription options with RevenueCat integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel = koinViewModel(),
    onDismiss: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val packagesResource by viewModel.packages.collectAsState()
    var selectedPackageId by remember { mutableStateOf<String?>(null) }

    // Handle success - show snackbar then dismiss the paywall
    LaunchedEffect(uiState) {
        if (uiState is PaywallUiState.Success) {
            // Give user time to see the success message
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceSpaceElevated,
                contentColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = uiState !is PaywallUiState.Loading
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMedium
                        )
                    }
                }

                // Header
                Text(
                    text = "Upgrade to",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextMedium
                )
                Text(
                    text = "CleanContacts Pro",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNeon
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Features
                FeatureRow("Delete junk contacts")
                FeatureRow("Merge duplicates")
                FeatureRow("Sync with cloud backup")
                FeatureRow("Save imported contacts")
                FeatureRow("Priority support")

                Spacer(modifier = Modifier.height(32.dp))

                // Packages
                when (val state = packagesResource) {
                    is Resource.Loading -> {
                        // Skeleton Loading UI
                        repeat(2) {
                            PricingOptionSkeleton()
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Fetching best prices...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLow,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    is Resource.Error -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ErrorNeon,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Unable to load pricing info.\n${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = ErrorNeon
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorNeon.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry", color = ErrorNeon)
                        }
                    }

                    is Resource.Success -> {
                        val packages = state.data
                        if (packages.isEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorNeon,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No packages available.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = ErrorNeon
                            )
                        } else {
                            // Auto-select monthly
                            LaunchedEffect(packages) {
                                if (selectedPackageId == null) {
                                    selectedPackageId = packages.find {
                                        it.identifier == "monthly"
                                    }?.id ?: packages.first().id
                                }
                            }

                            packages.forEach { pkg ->
                                PricingOption(
                                    title = when (pkg.identifier) {
                                        "monthly" -> "Monthly"
                                        "lifetime" -> "Lifetime"
                                        else -> pkg.title
                                    },
                                    price = pkg.price,
                                    // Apple Guideline 3.1.2: Show subscription length clearly
                                    billingPeriod = when (pkg.identifier) {
                                        "monthly" -> "per month, auto-renews"
                                        "lifetime" -> "one-time purchase"
                                        else -> null
                                    },
                                    badge = if (pkg.identifier == "lifetime") "Best Value" else null,
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
                            viewModel.purchasePackage(id)
                        }
                    },
                    enabled = selectedPackageId != null && uiState !is PaywallUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        disabledContainerColor = PrimaryNeon.copy(alpha = 0.3f)
                    )
                ) {
                    if (uiState is PaywallUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SpaceBlack,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpaceBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { viewModel.restorePurchases() },
                    enabled = uiState !is PaywallUiState.Loading
                ) {
                    Text(
                        "Restore Purchases",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Apple Guideline 3.1.2: Subscription info and legal links
                SubscriptionLegalText()
            }
        }

        // Snackbar for success and errors
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(uiState) {
            when (uiState) {
                is PaywallUiState.Success -> {
                    snackbarHostState.showSnackbar("Purchase successful! Premium features unlocked.")
                }
                is PaywallUiState.Error -> {
                    val message = (uiState as PaywallUiState.Error).message
                    snackbarHostState.showSnackbar(message)
                    viewModel.resetState()
                }
                else -> {}
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = PrimaryNeon,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

/**
 * Apple Guideline 3.1.2: Display subscription details clearly.
 * Shows title, price, billing period, and optional badge.
 */
@Composable
private fun PricingOption(
    title: String,
    price: String,
    billingPeriod: String? = null,
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
                PrimaryNeon.copy(alpha = 0.15f)
            else
                Color.White.copy(alpha = 0.05f)
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, PrimaryNeon)
        else
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryNeon.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryNeon,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // Apple Guideline 3.1.2: Show billing period clearly
                if (billingPeriod != null) {
                    Text(
                        text = billingPeriod,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLow
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PrimaryNeon else TextMedium
                )
            }
        }
    }
}

/**
 * Apple Guideline 3.1.2: Required subscription legal text with functional links.
 * - Subscription auto-renews unless cancelled
 * - Lifetime is a one-time purchase (not a subscription)
 * - Links to Terms of Use and Privacy Policy
 */
@Composable
private fun SubscriptionLegalText() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Monthly subscriptions auto-renew unless cancelled at least 24 hours before the end of the current period. Your account will be charged for renewal within 24 hours prior to the end of the current period. You can manage and cancel your subscriptions in your App Store account settings. Lifetime is a one-time purchase with no recurring charges.",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Clickable legal links - Apple Guideline 3.1.2 requirement
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terms of Use",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryNeon,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable { UrlOpener.openUrl(LegalUrls.TERMS_OF_USE) }
            )

            Text(
                text = "  •  ",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow
            )

            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryNeon,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable { UrlOpener.openUrl(LegalUrls.PRIVACY_POLICY) }
            )
        }
    }
}

@Composable
private fun PricingOptionSkeleton() {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.1f),
        Color.White.copy(alpha = 0.2f),
        Color.White.copy(alpha = 0.1f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200)
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(brush)
    )
}
