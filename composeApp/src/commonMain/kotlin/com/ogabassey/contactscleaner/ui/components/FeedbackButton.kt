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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.data.api.FeedbackApi
import com.ogabassey.contactscleaner.data.api.FeedbackRequest
import com.ogabassey.contactscleaner.platform.SupportEmail
import com.ogabassey.contactscleaner.platform.SupportEmailLauncher
import com.ogabassey.contactscleaner.ui.theme.*
import com.ogabassey.contactscleaner.util.DeviceInfo
import kotlinx.coroutines.launch

private const val MAX_FEEDBACK_LENGTH = 5000
private val categories = listOf("Bug Report", "Feature Request", "General Feedback")

private sealed class SubmissionResult {
    data class Success(val message: String) : SubmissionResult()
    data class Error(val message: String) : SubmissionResult()
}

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackBottomSheet(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionResult by remember { mutableStateOf<SubmissionResult?>(null) }
    var isDismissed by remember { mutableStateOf(false) }
    val isEmailValid = email.isBlank() || email.contains("@")
    val trimmedMessage = message.trim()
    val trimmedEmail = email.trim()
    val deviceString = "${DeviceInfo.platformName} ${DeviceInfo.osVersion} | ${DeviceInfo.deviceModel}"

    ModalBottomSheet(
        onDismissRequest = {
            isDismissed = true
            onDismiss()
        },
        sheetState = sheetState,
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                isError = !isEmailValid,
                supportingText = if (!isEmailValid) {
                    { Text("Enter a valid email address") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryNeon,
                    unfocusedBorderColor = SurfaceSpaceElevated,
                    focusedLabelColor = SecondaryNeon,
                    unfocusedLabelColor = TextMedium,
                    cursorColor = SecondaryNeon,
                    focusedTextColor = TextHigh,
                    unfocusedTextColor = TextHigh,
                    errorBorderColor = ErrorNeon,
                    errorLabelColor = ErrorNeon,
                    errorSupportingTextColor = ErrorNeon
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
                visible = submissionResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                submissionResult?.let { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        val (text, color) = when (result) {
                            is SubmissionResult.Success -> result.message to SuccessNeon
                            is SubmissionResult.Error -> result.message to ErrorNeon
                        }
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )

                        if (result is SubmissionResult.Error && !isSubmitting) {
                            TextButton(
                                onClick = {
                                    val launched = SupportEmailLauncher.composeEmail(
                                        address = SupportEmail.ADDRESS,
                                        subject = SupportEmail.subjectFor(selectedCategory),
                                        body = SupportEmail.bodyFor(
                                            category = selectedCategory,
                                            message = trimmedMessage,
                                            email = trimmedEmail,
                                            deviceInfo = deviceString
                                        )
                                    )
                                    submissionResult = if (launched) {
                                        SubmissionResult.Success("Email draft opened in your mail app.")
                                    } else {
                                        SubmissionResult.Error("Failed to open an email app.")
                                    }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Email support instead", color = SecondaryNeon)
                            }
                        }
                    }
                }
            }

            // Submit button
            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        submissionResult = null
                        val result = FeedbackApi.submitFeedback(
                            FeedbackRequest(
                                category = selectedCategory,
                                message = trimmedMessage,
                                email = trimmedEmail,
                                deviceInfo = deviceString
                            )
                        )
                        if (result.success) {
                            submissionResult = SubmissionResult.Success("Feedback sent! Thank you.")
                            kotlinx.coroutines.delay(1500)
                            isSubmitting = false
                            if (!isDismissed) {
                                isDismissed = true
                                sheetState.hide()
                                onDismiss()
                            }
                        } else {
                            isSubmitting = false
                            submissionResult = SubmissionResult.Error("Failed to send. Please try again.")
                        }
                    }
                },
                enabled = trimmedMessage.isNotEmpty() && isEmailValid && !isSubmitting,
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
