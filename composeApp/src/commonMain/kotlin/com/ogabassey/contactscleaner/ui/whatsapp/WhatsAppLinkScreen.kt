package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full-screen WhatsApp linking flow.
 * Guides the user through entering their phone number and pairing code.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppLinkScreen(
    viewModel: WhatsAppLinkViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onConnected: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val isConnected = state is WhatsAppLinkState.Connected
    
    // Navigate back when connected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            onConnected()
        }
    }

    Scaffold(
        containerColor = SpaceBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Link WhatsApp",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelLinking()
                        onNavigateBack()
                    }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                },
                label = "WhatsAppLinkStateTransition"
            ) { currentState ->
                when (currentState) {
                    is WhatsAppLinkState.Checking -> LoadingContent()
                    is WhatsAppLinkState.NotLinked -> PhoneInputContent(
                        onSubmit = { viewModel.requestPairingCode(it) }
                    )
                    is WhatsAppLinkState.RequestingCode -> LoadingContent(
                        message = "Requesting pairing code..."
                    )
                    is WhatsAppLinkState.WaitingForPairing -> PairingCodeContent(
                        code = currentState.code,
                        phoneNumber = currentState.phoneNumber,
                        onCancel = { viewModel.cancelLinking() }
                    )
                    is WhatsAppLinkState.Connected -> SuccessContent()
                    is WhatsAppLinkState.Error -> ErrorContent(
                        message = currentState.message,
                        onRetry = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String = "Checking connection...") {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        CircularProgressIndicator(color = PrimaryNeon)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = TextMedium)
    }
}

@Composable
private fun PhoneInputContent(onSubmit: (String) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isValid = remember(phoneNumber) {
        val digits = phoneNumber.filter { it.isDigit() }
        digits.length in 8..15
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83D\uDCAC",
                fontSize = 36.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Link Your WhatsApp",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Enter the phone number linked to your WhatsApp account",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Phone input
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it.filter { c -> c.isDigit() || c == '+' } },
            label = { Text("Phone Number") },
            placeholder = { Text("+1234567890") },
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = "Phone number input", tint = TextMedium)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { 
                    if (isValid) {
                        keyboardController?.hide()
                        onSubmit(phoneNumber)
                    }
                }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryNeon,
                unfocusedBorderColor = TextLow,
                focusedLabelColor = PrimaryNeon,
                unfocusedLabelColor = TextMedium,
                cursorColor = PrimaryNeon,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Include country code (e.g., +1 for US)",
            style = MaterialTheme.typography.labelSmall,
            color = TextLow
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { 
                keyboardController?.hide()
                onSubmit(phoneNumber) 
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryNeon,
                contentColor = SpaceBlack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Get Pairing Code",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Privacy info
        PrivacyInfoCard()
    }
}

@Composable
private fun PairingCodeContent(
    code: String,
    phoneNumber: String,
    onCancel: () -> Unit,
    viewModel: WhatsAppLinkViewModel = koinViewModel()
) {
    val expirationSeconds by viewModel.pairingCodeExpiration.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Enter this code in WhatsApp",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "A linking request was sent to $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Pairing code display
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DeepSpace,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Format code as "XXXX-XXXX"
                val formattedCode = if (code.length == 8) {
                    "${code.substring(0, 4)}-${code.substring(4, 8)}"
                } else {
                    code
                }

                Text(
                    formattedCode,
                    style = MaterialTheme.typography.displayMedium,
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Copy Button
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = PrimaryNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Code", style = MaterialTheme.typography.labelMedium, color = PrimaryNeon)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val remainingMinutes = expirationSeconds?.let { it / 60 } ?: 0
                    val remainingSeconds = expirationSeconds?.let { it % 60 } ?: 0
                    val timeString = expirationSeconds?.let { 
                        "${remainingMinutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}"
                    } ?: "--:--"

                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryNeon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Code expires in $timeString",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "How to link:",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                InstructionStep(1, "Open WhatsApp on your phone")
                InstructionStep(2, "Go to Settings > Linked Devices")
                InstructionStep(3, "Tap \"Link a Device\"")
                InstructionStep(4, "Enter the code shown above")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onCancel) {
            Text("Cancel", color = TextMedium)
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PrimaryNeon.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryNeon,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMedium
        )
    }
}

@Composable
private fun SuccessContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PrimaryNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Successfully connected",
                tint = PrimaryNeon,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "WhatsApp Linked!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Your contact scans will now show WhatsApp status",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(ErrorNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error occurred",
                tint = ErrorNeon,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryNeon,
                contentColor = SpaceBlack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PrivacyInfoCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PrimaryNeon.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Privacy protected",
                tint = PrimaryNeon,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Privacy First",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your phone numbers are checked but never stored. " +
                    "All data is processed in-memory and discarded immediately. " +
                    "You can disconnect anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }
        }
    }
}
