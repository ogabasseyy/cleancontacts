package com.ogabassey.contactscleaner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.data.api.FeedbackApi
import com.ogabassey.contactscleaner.data.api.FeedbackRequest
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.util.DeviceInfo
import kotlinx.coroutines.launch

private const val MAX_FEEDBACK_LENGTH = 5000
private val categories = listOf("Bug Report", "Feature Request", "General Feedback")

@Composable
fun FloatingFeedbackButton(
    modifier: Modifier = Modifier,
    onOpenSheet: () -> Unit
) {
    SmallFloatingActionButton(
        onClick = onOpenSheet,
        modifier = modifier
            .size(48.dp)
            .glassy(radius = 24.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Send feedback"
            },
        shape = CircleShape,
        containerColor = Color.Transparent,
        contentColor = SecondaryNeon
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isDismissed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            isDismissed = true
            onDismiss()
        },
        containerColor = DeepSpace,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Send Feedback",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextHigh
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category chips
            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                color = TextMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryNeon.copy(alpha = 0.2f),
                            selectedLabelColor = SecondaryNeon,
                            containerColor = SurfaceSpaceElevated,
                            labelColor = TextMedium
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = SecondaryNeon.copy(alpha = 0.5f),
                            enabled = true,
                            selected = selectedCategory == category
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Message field
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= MAX_FEEDBACK_LENGTH) message = it },
                label = { Text("Message") },
                placeholder = { Text("Describe your feedback...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                minLines = 4,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryNeon,
                    unfocusedBorderColor = SurfaceSpaceElevated,
                    focusedLabelColor = SecondaryNeon,
                    unfocusedLabelColor = TextMedium,
                    cursorColor = SecondaryNeon,
                    focusedTextColor = TextHigh,
                    unfocusedTextColor = TextHigh
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email field (optional)
            OutlinedTextField(
                value = email,
                onValueChange = { if (it.length <= 254) email = it },
                label = { Text("Email (optional)") },
                placeholder = { Text("your@email.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryNeon,
                    unfocusedBorderColor = SurfaceSpaceElevated,
                    focusedLabelColor = SecondaryNeon,
                    unfocusedLabelColor = TextMedium,
                    cursorColor = SecondaryNeon,
                    focusedTextColor = TextHigh,
                    unfocusedTextColor = TextHigh
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Your message, category, and device info are shared. Email is optional.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Result message
            AnimatedVisibility(
                visible = resultMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                resultMessage?.let { msg ->
                    val isError = msg.startsWith("Failed")
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) ErrorNeon else SuccessNeon,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Submit button
            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        resultMessage = null
                        val deviceString = "${DeviceInfo.platformName} ${DeviceInfo.osVersion} | ${DeviceInfo.deviceModel}"
                        val result = FeedbackApi.submitFeedback(
                            FeedbackRequest(
                                category = selectedCategory,
                                message = message.trim(),
                                email = email.trim(),
                                deviceInfo = deviceString
                            )
                        )
                        isSubmitting = false
                        if (result.success) {
                            resultMessage = "Feedback sent! Thank you."
                            kotlinx.coroutines.delay(1500)
                            if (!isDismissed) {
                                isDismissed = true
                                onDismiss()
                            }
                        } else {
                            resultMessage = "Failed to send. Please try again."
                        }
                    }
                },
                enabled = message.isNotBlank() && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryNeon,
                    contentColor = SpaceBlack,
                    disabledContainerColor = SurfaceSpaceElevated,
                    disabledContentColor = TextLow
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SpaceBlack,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Feedback", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
