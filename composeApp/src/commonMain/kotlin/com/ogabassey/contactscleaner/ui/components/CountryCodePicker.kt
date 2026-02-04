package com.ogabassey.contactscleaner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ogabassey.contactscleaner.ui.theme.PrimaryNeon
import com.ogabassey.contactscleaner.ui.theme.SpaceBlack
import com.ogabassey.contactscleaner.ui.theme.TextMedium
import com.ogabassey.contactscleaner.ui.theme.TextLow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodePicker(
    selectedCountry: CountryCode,
    onCountrySelected: (CountryCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Trigger
    Row(
        modifier = modifier
            .clickable { showSheet = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedCountry.flag,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = selectedCountry.code,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SpaceBlack,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = TextLow)
            }
        ) {
            CountrySelectionContent(
                onSelected = {
                    onCountrySelected(it)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun CountrySelectionContent(
    onSelected: (CountryCode) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CountryResources.countries
        } else {
            CountryResources.countries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            "Select Country",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search country...", color = TextMedium) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMedium) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryNeon,
                unfocusedBorderColor = TextLow,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = PrimaryNeon
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCountries) { country ->
                CountryItem(
                    country = country,
                    onClick = { onSelected(country) }
                )
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: CountryCode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(country.flag, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                country.name,
                modifier = Modifier.weight(1f),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                country.code,
                color = PrimaryNeon,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
