package com.ogabassey.contactscleaner.ui.limitedaccess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.ui.components.rememberContactsPermissionState
import com.ogabassey.contactscleaner.ui.util.PickedContact
import com.ogabassey.contactscleaner.ui.util.rememberContactPicker
import com.ogabassey.contactscleaner.ui.theme.*

/**
 * Limited Access Screen - Apple Guideline 5.1.1 Compliance
 *
 * Shown when user denies full contacts access. Provides alternatives:
 * 1. Pick specific contacts to clean (uses native picker, no permission needed)
 * 2. Demo mode with sample contacts
 * 3. Link to settings to grant full access
 */
@Composable
fun LimitedAccessScreen(
    onDemoMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onContactPicked: (PickedContact) -> Unit = {}
) {
    val permissionState = rememberContactsPermissionState()
    var pickedContact by remember { mutableStateOf<PickedContact?>(null) }
    var showPickedContactDialog by remember { mutableStateOf(false) }

    // Contact picker - launches system picker, no permission needed
    val contactPicker = rememberContactPicker(
        onContactPicked = { contact ->
            pickedContact = contact
            showPickedContactDialog = true
            onContactPicked(contact)
        },
        onCancelled = {
            // User cancelled picker, do nothing
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

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

            Spacer(modifier = Modifier.height(48.dp))

            // Explanation
            Text(
                text = "Choose how you'd like to proceed",
                style = MaterialTheme.typography.titleMedium,
                color = TextMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Option 1: Pick Specific Contacts
            AccessOptionCard(
                icon = Icons.Default.Person,
                iconColor = PrimaryNeon,
                title = "Clean Specific Contacts",
                description = "Select contacts to scan and clean one by one",
                onClick = {
                    contactPicker.launch()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Demo Mode
            AccessOptionCard(
                icon = Icons.Default.PlayArrow,
                iconColor = SecondaryNeon,
                title = "See How It Works",
                description = "Try with sample contacts (Demo Mode)",
                onClick = onDemoMode
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 3: Grant Full Access
            AccessOptionCard(
                icon = Icons.Default.Settings,
                iconColor = TextMedium,
                title = "Grant Full Access",
                description = "Clean all contacts at once in Settings",
                onClick = {
                    permissionState.openSettings()
                    onOpenSettings()
                },
                isSecondary = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Privacy note
            Text(
                text = "Your contacts are processed locally on your device and never uploaded to our servers.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLow,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Show dialog when contact is picked
    // 2026 Fix: Use let to avoid redundant null checks after smart cast
    if (showPickedContactDialog) {
        pickedContact?.let { contact ->
            AlertDialog(
                onDismissRequest = { showPickedContactDialog = false },
                title = {
                    Text("Contact Selected", color = Color.White)
                },
                text = {
                    Column {
                        Text(
                            text = contact.name ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryNeon
                        )
                        contact.phoneNumber?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMedium
                            )
                        }
                        contact.email?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLow
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This contact has been selected for cleaning. In a full implementation, you would be shown analysis and cleanup options for this contact.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPickedContactDialog = false }) {
                        Text("OK", color = PrimaryNeon)
                    }
                },
                containerColor = SurfaceSpaceElevated
            )
        }
    }
}

@Composable
private fun AccessOptionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    isSecondary: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSecondary)
                Color.White.copy(alpha = 0.05f)
            else
                SurfaceSpaceElevated
        ),
        border = if (!isSecondary)
            androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
        else
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }
        }
    }
}
