package com.ogabassey.contactscleaner.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.ui.components.glassy
import com.ogabassey.contactscleaner.ui.theme.*

@Composable
fun ContactList(contacts: LazyPagingItems<Contact>, accentColor: Color) {
    val refreshState = contacts.loadState.refresh

    if (refreshState is LoadState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentColor)
        }
    } else if (contacts.itemCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No contacts in this category.", color = TextLow)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(
                count = contacts.itemCount,
                key = contacts.itemKey { it.id },
                contentType = contacts.itemContentType { "contact" }
            ) { index ->
                val contact = contacts[index]
                if (contact != null) {
                    ContactItem(contact, accentColor)
                }
            }
            if (contacts.loadState.append is LoadState.Loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, accentColor: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassy(radius = 16.dp)
                .clickable {
                    // Open System Contact Details
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contact.id.toString())
                        intent.data = uri
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Could not open contact", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (contact.name ?: "?").take(1).uppercase(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name ?: "Unnamed Contact", color = TextHigh, fontWeight = FontWeight.Bold)
                
                // Show Email if duplicate email, else Number
                val details = if (contact.duplicateType == com.ogabassey.contactscleaner.domain.model.DuplicateType.EMAIL_MATCH && contact.emails.isNotEmpty()) {
                    contact.emails.first()
                } else {
                    contact.numbers.firstOrNull() ?: "No Number"
                }
                
                Text(
                     details,
                     color = TextMedium, 
                     fontSize = 12.sp,
                     maxLines = 1
                )
            }
            
            // Action Icon (Merge/Delete)
            if (accentColor == ErrorNeon) {
                Icon(Icons.Default.Delete, "Delete", tint = ErrorNeon.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            } else if (accentColor == WarningNeon) {
                 Icon(Icons.Default.Build, "Merge", tint = WarningNeon.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun DuplicateGroupList(
    groups: List<DuplicateGroupSummary>, 
    accentColor: Color,
    onGroupClick: (DuplicateGroupSummary) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("No duplicate groups found.", color = TextLow)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(groups) { group ->
                DuplicateGroupItem(group, accentColor, onGroupClick)
            }
        }
    }
}

@Composable
fun DuplicateGroupItem(
    group: DuplicateGroupSummary, 
    accentColor: Color,
    onGroupClick: (DuplicateGroupSummary) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassy(radius = 16.dp)
                .clickable { onGroupClick(group) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    group.count.toString(),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Determine Title (Name/Email/Number)
                val title = group.groupKey ?: "Unknown Group"
                Text(title, color = TextHigh, fontWeight = FontWeight.Bold, maxLines = 1)
                
                // Subtitle (Preview)
                if (!group.previewNames.isNullOrEmpty()) {
                     Text(
                        group.previewNames,
                        color = TextMedium, 
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Icon(Icons.Default.KeyboardArrowRight, "View", tint = TextMedium)
        }
    }
}

@Composable
fun AccountGroupList(
    groups: List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>,
    onGroupClick: (com.ogabassey.contactscleaner.domain.model.AccountGroupSummary) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("No accounts found.", color = TextLow)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(groups) { group ->
                AccountGroupItem(group, onGroupClick)
            }
        }
    }
}

@Composable
fun AccountGroupItem(
    group: com.ogabassey.contactscleaner.domain.model.AccountGroupSummary,
    onGroupClick: (com.ogabassey.contactscleaner.domain.model.AccountGroupSummary) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassy(radius = 16.dp)
                .clickable { onGroupClick(group) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on Account Type
            val iconChar = (group.accountName ?: group.accountType ?: "D").take(1).uppercase()
             
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SecondaryNeon.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
               val image = when {
                   group.accountType?.contains("google", true) == true -> Icons.Default.Email
                   group.accountType?.contains("whatsapp", true) == true -> Icons.Default.Call
                   else -> Icons.Default.Person
               }
                Icon(image, null, tint = SecondaryNeon, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(group.accountName ?: "Device", color = TextHigh, fontWeight = FontWeight.Bold)
                Text(
                     group.accountType ?: "Local Account",
                     color = TextMedium, 
                     fontSize = 12.sp,
                     maxLines = 1
                )
            }
            
            Text(
                group.count.toString(),
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ResultsUtilityBar(
    contactType: com.ogabassey.contactscleaner.domain.model.ContactType,
    onDeleteAll: () -> Unit = {},
    onMergeAll: () -> Unit = {},
    onExportAll: () -> Unit = {}
) {
    val isDuplicateFilter = contactType.name.startsWith("DUP") && contactType != com.ogabassey.contactscleaner.domain.model.ContactType.DUPLICATE
    val canDelete = contactType.name.startsWith("JUNK") || contactType == com.ogabassey.contactscleaner.domain.model.ContactType.NON_WHATSAPP
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .glassy(radius = 12.dp)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(getColorForFilter(contactType), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ACTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = TextMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Export Action
            if (contactType != com.ogabassey.contactscleaner.domain.model.ContactType.ACCOUNT) {
                TextButton(
                    onClick = onExportAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryNeon),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Primary Action (Merge or Delete)
            if (isDuplicateFilter) {
                Button(
                    onClick = onMergeAll,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = SpaceBlack),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text("MERGE ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else if (canDelete) {
                Button(
                    onClick = onDeleteAll,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorNeon, contentColor = SpaceBlack),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text("DELETE ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrialStatusChip(
    isPremium: Boolean,
    remainingActions: Int,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isPremium) SuccessNeon.copy(alpha = 0.1f) else WarningNeon.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isPremium) SuccessNeon.copy(alpha = 0.3f) else WarningNeon.copy(alpha = 0.3f)
        ),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (isPremium) SuccessNeon else WarningNeon, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPremium) "PREMIUM" else "$remainingActions TRIAL ACTIONS LEFT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPremium) SuccessNeon else WarningNeon,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun FreeTrialBanner(remainingActions: Int, onUpgradeClick: () -> Unit) {
    if (remainingActions <= 0) return
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = WarningNeon.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningNeon.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Build, null, tint = WarningNeon, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Free Actions Remaining: $remainingActions",
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningNeon,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Try out the cleanup features before upgrading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMedium
                )
            }
            TextButton(onClick = onUpgradeClick) {
                Text("UPGRADE", color = WarningNeon, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getColorForFilter(type: com.ogabassey.contactscleaner.domain.model.ContactType): Color {
    return when {
        type.name.startsWith("JUNK") -> ErrorNeon
        type.name.startsWith("DUP") -> WarningNeon
        type == com.ogabassey.contactscleaner.domain.model.ContactType.WHATSAPP -> SuccessNeon
        type == com.ogabassey.contactscleaner.domain.model.ContactType.TELEGRAM -> SecondaryNeon
        else -> PrimaryNeon
    }
}

