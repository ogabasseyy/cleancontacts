package com.ogabassey.contactscleaner.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import com.ogabassey.contactscleaner.ui.components.ContactsPermissionState
import com.ogabassey.contactscleaner.ui.components.ContactsAuthorizationStatus
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.components.rememberContactsPermissionState
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.util.formatWithCommas
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dashboard Screen for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Cross-platform composable with Koin ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToResults: () -> Unit = {},
    onNavigateToRecentActions: () -> Unit = {},
    onNavigateToSafeList: () -> Unit = {},
    onNavigateToReviewSensitive: () -> Unit = {},
    onNavigateToLimitedAccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionsState = rememberContactsPermissionState()
    val haptic = LocalHapticFeedback.current
    var scanRequested by remember { mutableStateOf(false) }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardViewModel.DashboardEvent.NavigateToResults -> {
                    onNavigateToResults()
                }
            }
        }
    }

    // 2026 Fix: Consolidated LaunchedEffect for permission handling
    // When scanRequested and status is determined, take action immediately
    LaunchedEffect(permissionsState.authorizationStatus, scanRequested) {
        if (scanRequested && permissionsState.authorizationStatus != ContactsAuthorizationStatus.NOT_DETERMINED) {
            scanRequested = false
            when (permissionsState.authorizationStatus) {
                ContactsAuthorizationStatus.AUTHORIZED,
                ContactsAuthorizationStatus.LIMITED -> {
                    // Can scan with full or limited access
                    viewModel.startScan()
                }
                ContactsAuthorizationStatus.DENIED -> {
                    // Apple Guideline 5.1.1: Navigate to limited access screen
                    onNavigateToLimitedAccess()
                }
                // NOT_DETERMINED is excluded by outer condition - no else branch needed
                // 2026 Best Practice: Exhaustive when without else ensures compile-time safety
                ContactsAuthorizationStatus.NOT_DETERMINED -> Unit
            }
        }
    }

    var showSettingsSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp), // Increased vertical padding to bring it down
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Info, // Changed from Settings to Info
                        contentDescription = "About",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Glow Decor
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryNeon.copy(alpha = 0.15f), Color.Transparent),
                        center = center.copy(y = 0f)
                    ),
                    radius = size.width
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // App Branding
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CONTACTS",
                        style = MaterialTheme.typography.displayLarge,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "CLEANER",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // The Solar System Centerpiece
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(320.dp)
                ) {
                    OrbitalFeatures(radius = 130.dp)
                    PulseAnimation(color = PrimaryNeon)

                    // Main Button
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        label = "buttonScale",
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )

                    val buttonContentDescription = when (val state = uiState) {
                        is DashboardUiState.Scanning -> "Scan in Progress: ${(state.progress * 100).toInt()} percent"
                        else -> if (!permissionsState.allPermissionsGranted) "Grant Permissions to Scan" else "Start Deep Scan"
                    }

                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(32.dp, CircleShape, spotColor = PrimaryNeon)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(PrimaryNeon, PrimaryNeonDim.copy(alpha = 0.8f))
                                )
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = buttonContentDescription
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val status = permissionsState.authorizationStatus
                                when (status) {
                                    ContactsAuthorizationStatus.AUTHORIZED,
                                    ContactsAuthorizationStatus.LIMITED -> {
                                        // Can scan with full or limited access
                                        viewModel.startScan()
                                    }
                                    ContactsAuthorizationStatus.DENIED -> {
                                        // Already denied - go to limited access screen
                                        onNavigateToLimitedAccess()
                                    }
                                    ContactsAuthorizationStatus.NOT_DETERMINED -> {
                                        // Request permission
                                        scanRequested = true
                                        permissionsState.launchRequest()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = uiState) {
                            is DashboardUiState.Scanning -> {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = state.progress,
                                    label = "progressAnimation",
                                    animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                                )
                                // Continuous rotation for flowing effect
                                val infiniteTransition = rememberInfiniteTransition(label = "scanFlow")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "flowRotation"
                                )
                                val pct = (animatedProgress * 100).toInt()
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { animatedProgress },
                                        color = SpaceBlack,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .graphicsLayer { rotationZ = rotation },
                                        strokeWidth = 6.dp,
                                        trackColor = SpaceBlack.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SpaceBlack,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            else -> {
                                Icon(
                                    imageVector = if (!permissionsState.allPermissionsGranted)
                                        Icons.Default.Lock else Icons.Default.Search,
                                    contentDescription = "Scan",
                                    tint = SpaceBlack,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Fixed-height container for Results Summary / Status to prevent layout shift
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .heightIn(min = 140.dp), // Height sufficient for ResultsSummaryCard
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (val state = uiState) {
                        is DashboardUiState.ShowingResults -> {
                            ResultsSummaryCard(
                                result = state.result,
                                onViewDetails = { viewModel.showDetails() },
                                onRecentActions = onNavigateToRecentActions,
                                onReviewSensitive = onNavigateToReviewSensitive
                            )
                        }
                        is DashboardUiState.Scanning -> {
                            Text(
                                text = state.message?.uppercase() ?: "ANALYZING",
                                style = MaterialTheme.typography.labelLarge,
                                color = PrimaryNeon.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        is DashboardUiState.Error -> {
                            Text(
                                text = "ERROR: ${state.message}",
                                style = MaterialTheme.typography.labelLarge,
                                color = ErrorNeon,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        DashboardUiState.Idle -> {
                            Text(
                                text = if (!permissionsState.allPermissionsGranted)
                                    "TAP TO GRANT ACCESS" else "TAP TO DEEP SCAN",
                                style = MaterialTheme.typography.labelLarge,
                                color = PrimaryNeon.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (uiState is DashboardUiState.ShowingResults) {
                    Text(
                        text = "TAP CENTER TO RESCAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = DeepSpace,
            contentColor = Color.White
        ) {
            SettingsContent(
                versionName = "1.0.0",
                onDismiss = { showSettingsSheet = false },
                onNavigateToSafeList = {
                    showSettingsSheet = false
                    onNavigateToSafeList()
                }
            )
        }
    }
}

@Composable
private fun ResultsSummaryCard(
    result: com.ogabassey.contactscleaner.domain.model.ScanResult,
    onViewDetails: () -> Unit,
    onRecentActions: () -> Unit,
    onReviewSensitive: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .glassy(radius = 16.dp)
            .padding(16.dp)
            .clickable { onViewDetails() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CheckCircle, "Clean", tint = PrimaryNeon, modifier = Modifier.size(20.dp))
            Text(
                text = "LAST SCAN SUMMARY",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResultMiniStat(count = result.accountCount, label = "ACCOUNTS", color = PrimaryNeon)
            
            // Sum of all issues for the summary view
            val totalIssues = result.junkCount + result.duplicateCount + result.formatIssueCount + result.sensitiveCount
            ResultMiniStat(count = totalIssues, label = "TOTAL ISSUES", color = ErrorNeon)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onViewDetails,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("VIEW DETAILS", color = SpaceBlack, fontWeight = FontWeight.Black)
            }

            FilledTonalButton(
                onClick = onRecentActions,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Recent Actions", tint = Color.White)
            }
        }
    }
}

@Composable
private fun SensitiveSuggestionBanner(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = WarningNeon.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningNeon.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(WarningNeon.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = WarningNeon, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Suggestions for Safe List",
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    "${count.formatWithCommas()} candidates found",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = WarningNeon.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SettingsContent(
    versionName: String,
    onDismiss: () -> Unit,
    onNavigateToSafeList: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryNeon
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Settings",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsItem(icon = Icons.Default.Info, title = "Version", subtitle = versionName)

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

        SettingsItem(
            icon = Icons.Default.Lock,
            title = "Safe List",
            subtitle = "Managed protected contacts",
            onClick = onNavigateToSafeList
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

        SettingsItem(
            icon = Icons.Default.Info,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = { uriHandler.openUri("https://contactscleaner.tech/privacy") }
        )

        SettingsItem(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Terms of Service",
            subtitle = "Legal agreements",
            onClick = { uriHandler.openUri("https://contactscleaner.tech/terms") }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Contacts Cleaner is built with privacy in mind. Your contacts are processed locally on your device and never uploaded to our servers.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 2026 Accessibility: Add semantic role for screen readers
            .clickable(
                enabled = onClick != null,
                role = if (onClick != null) Role.Button else null
            ) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryNeon,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        // 2026 UX: weight(1f) ensures text column doesn't push chevron off-screen
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
        // 2026 UX: Visual affordance - chevron indicates item is clickable
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ResultMiniStat(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.formatWithCommas(),
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun OrbitalFeatures(radius: Dp) {
    // ⚡ Bolt Optimization: Memoize list to prevent reallocation
    val features = remember {
        listOf(
            OrbitalItem(Icons.Default.Email, SecondaryNeon),
            OrbitalItem(Icons.Default.Delete, ErrorNeon),
            OrbitalItem(Icons.Default.Face, WarningNeon),
            OrbitalItem(Icons.Default.Lock, PrimaryNeon)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    // ⚡ 2026 Optimization: Defer state read to graphicsLayer to prevent recomposition every frame
    val rotationState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // ⚡ Bolt Optimization: Use graphicsLayer for animations to avoid layout thrashing
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }

    features.forEachIndexed { index, item ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                // ⚡ Read state inside graphicsLayer - updates layer without recomposition
                val rotation = rotationState.value
                val angleDeg = (index * 90f) + rotation
                val angleRad = angleDeg.toDouble() * PI / 180.0
                translationX = (radiusPx * cos(angleRad)).toFloat()
                translationY = (radiusPx * sin(angleRad)).toFloat()
            }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .glassy(radius = 24.dp)
                    .border(1.dp, item.color.copy(alpha = 0.5f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PulseAnimation(color: Color = PrimaryNeon) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    // ⚡ 2026 Optimization: Defer state read to graphicsLayer to prevent recomposition every frame
    val scaleState = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alphaState = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer {
                // ⚡ Read state inside graphicsLayer - updates layer without recomposition
                scaleX = scaleState.value
                scaleY = scaleState.value
                alpha = alphaState.value
            }
            .background(color, CircleShape)
    )
}

data class OrbitalItem(val icon: ImageVector, val color: Color)
