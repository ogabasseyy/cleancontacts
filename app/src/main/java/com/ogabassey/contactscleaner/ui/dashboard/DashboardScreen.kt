package com.ogabassey.contactscleaner.ui.dashboard

import android.Manifest
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToResults: () -> Unit,
    onNavigateToRecentActions: () -> Unit = {},
    onNavigateToSafeList: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    )

    // Robust Navigation
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when(event) {
                is DashboardViewModel.DashboardEvent.NavigateToResults -> {
                    onNavigateToResults()
                }
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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    ) { paddingValues ->
        // Content ... (omitted for brevity in replacement, but I will keep it)
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

                // App Branding (Modern Stack)
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
                    modifier = Modifier.size(320.dp) // Larger container for orbit
                ) {
                    // Orbiting Satellites
                    OrbitalFeatures(radius = 130.dp)

                    // Pulse
                    PulseAnimation(color = PrimaryNeon)
                    
                    // Main Button
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "buttonScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .size(160.dp)
                            .shadow(32.dp, CircleShape, spotColor = PrimaryNeon)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        PrimaryNeon,
                                        PrimaryNeonDim.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button
                            ) {
                                if (permissionsState.allPermissionsGranted) {
                                    viewModel.startScan()
                                } else {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                             },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState is DashboardUiState.Scanning) {
                            val scanState = uiState as DashboardUiState.Scanning
                            val pct = (scanState.progress * 100).toInt()
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { scanState.progress },
                                    color = SpaceBlack,
                                    modifier = Modifier.size(64.dp),
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
                        } else {
                            val iconDesc = if (!permissionsState.allPermissionsGranted)
                                "Grant Permissions to Scan"
                            else
                                "Start Scan"

                            Icon(
                                imageVector = if (!permissionsState.allPermissionsGranted) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = iconDesc,
                                tint = SpaceBlack,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                if (uiState is DashboardUiState.ShowingResults) {
                    val result = (uiState as DashboardUiState.ShowingResults).result
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .glassy(radius = 16.dp)
                            .padding(16.dp)
                            .clickable { viewModel.showDetails() }
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
                            ResultMiniStat(count = result.junkCount, label = "JUNK", color = ErrorNeon)
                            ResultMiniStat(count = result.duplicateCount, label = "DUPS", color = WarningNeon)
                            ResultMiniStat(count = result.formatIssueCount, label = "FORMAT", color = SecondaryNeon)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.showDetails() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("VIEW DETAILS", color = SpaceBlack, fontWeight = FontWeight.Black)
                            }
                            
                            // Recent Actions Button
                            FilledTonalButton(
                                onClick = onNavigateToRecentActions,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(44.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.1f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = "Recent Actions",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = when {
                            uiState is DashboardUiState.Scanning -> {
                                val state = uiState as DashboardUiState.Scanning
                                // Format message with comma-separated count
                                state.message?.uppercase()?.replace(Regex("\\((\\d+)\\)")) { match ->
                                    val num = match.groupValues[1].toLongOrNull() ?: 0
                                    "(${java.text.NumberFormat.getNumberInstance().format(num)})"
                                } ?: "ANALYZING"
                            }
                            uiState is DashboardUiState.Error -> {
                                "ERROR: ${(uiState as DashboardUiState.Error).message}"
                            }
                            !permissionsState.allPermissionsGranted -> "TAP TO GRANT ACCESS"
                            else -> "TAP TO DEEP SCAN"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (uiState is DashboardUiState.Error) ErrorNeon else PrimaryNeon.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
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
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
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
fun SettingsContent(
    versionName: String,
    onDismiss: () -> Unit,
    onNavigateToSafeList: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryNeon
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // App Info
        SettingsItem(
            icon = Icons.Default.Info,
            title = "Version",
            subtitle = versionName
        )
        
        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
        


        SettingsItem(
            icon = Icons.Default.Lock,
            title = "Safe List",
            subtitle = "Managed protected contacts",
            onClick = onNavigateToSafeList
        )
        
        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
        
        // Privacy Policy
        SettingsItem(
            icon = Icons.Default.Info,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = {
                val uri = android.net.Uri.parse("https://cleancontacts-ai-privacy-793339695925.us-west1.run.app")
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
        )
        
        SettingsItem(
             icon = Icons.Default.List,
             title = "Terms of Service",
             subtitle = "Legal agreements",
             onClick = {
                 // Open TOS URL
             }
         )

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "CleanContacts AI is built with privacy in mind. Your contacts are processed locally on your device and never uploaded to our servers.",
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
            .clickable(enabled = onClick != null) { onClick?.invoke() }
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
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ResultMiniStat(count: Int, label: String, color: Color) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
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
    val features = listOf(
        OrbitalItem(Icons.Default.Email, SecondaryNeon),  // WhatsApp/Email
        OrbitalItem(Icons.Default.Delete, ErrorNeon),     // Junk
        OrbitalItem(Icons.Default.Face, WarningNeon),     // Duplicates
        OrbitalItem(Icons.Default.Lock, PrimaryNeon)      // Security
    )

    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    // Slow rotation for the whole system
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    features.forEachIndexed { index, item ->
        val angleDeg = (index * 90f) + rotation // 4 items spaced 90 degrees apart + rotation
        val angleRad = Math.toRadians(angleDeg.toDouble())
        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(
                    x = (radius.value * cos(angleRad)).dp,
                    y = (radius.value * sin(angleRad)).dp
                )
        ) {
            // Satellite Bubble
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
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
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
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

data class OrbitalItem(val icon: ImageVector, val color: Color)
