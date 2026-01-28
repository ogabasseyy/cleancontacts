package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.ui.theme.*

/**
 * Card displayed on Results screen for iOS users to link WhatsApp.
 * Shows a CTA to unlock WhatsApp detection feature.
 */
@Composable
fun WhatsAppLinkCard(
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onLinkClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WhatsApp-style icon background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryNeon.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uD83D\uDCAC", // Speech bubble emoji as WhatsApp placeholder
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Unlock WhatsApp Detection",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Link your WhatsApp to see which contacts use it",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate to link WhatsApp",
                    tint = PrimaryNeon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryNeon.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Privacy protected",
                    tint = PrimaryNeon,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Privacy First - No data stored",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryNeon
                )
            }
        }
    }
}

/**
 * Compact version of the card for use in settings or other contexts.
 */
@Composable
fun WhatsAppLinkCardCompact(
    isConnected: Boolean,
    onLinkClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = if (isConnected) onDisconnectClick else onLinkClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) PrimaryNeon.copy(alpha = 0.15f)
                        else TextMedium.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\uD83D\uDCAC",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "WhatsApp Detection",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (isConnected) "Connected" else "Not linked",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConnected) PrimaryNeon else TextMedium
                )
            }

            Text(
                if (isConnected) "Disconnect" else "Link",
                style = MaterialTheme.typography.labelMedium,
                color = if (isConnected) ErrorNeon else PrimaryNeon,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
