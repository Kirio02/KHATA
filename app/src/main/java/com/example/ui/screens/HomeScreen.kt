package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.ui.components.PartyCard
import com.example.ui.theme.AccentCrimson
import com.example.ui.theme.CreditRed
import com.example.ui.theme.DebitGreen
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    profile: BusinessProfile,
    allParties: List<Party>,
    filteredParties: List<Party>,
    searchQuery: String,
    selectedPartyType: PartyType,
    strings: AppStrings,
    onSearchQueryChange: (String) -> Unit,
    onPartyTypeSelected: (PartyType) -> Unit,
    onPartyClick: (Party) -> Unit,
    onRemindParty: (Party) -> Unit,
    onAddPartyClick: (PartyType) -> Unit,
    onOpenCashbook: () -> Unit,
    onPdfReportClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onSetupStoreDetailsClick: () -> Unit = {}
) {
    val totalGet = allParties.filter { it.balance > 0 }.sumOf { it.balance }
    val totalGive = allParties.filter { it.balance < 0 }.sumOf { abs(it.balance) }
    val currency = profile.currencySymbol.ifBlank { "₹" }
    val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    Scaffold(
        floatingActionButton = {
            // Big crimson button (Exact match to Photo 1 "+ ADD CUSTOMER")
            ExtendedFloatingActionButton(
                onClick = { onAddPartyClick(selectedPartyType) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                },
                text = {
                    Text(
                        text = if (selectedPartyType == PartyType.CUSTOMER) "+ ADD CUSTOMER" else "+ ADD SUPPLIER",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                containerColor = AccentCrimson,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("add_party_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Top Header in Solid Navy Blue (Exact match to Photo 1)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryNavy)
                            .padding(top = 12.dp, bottom = 0.dp)
                    ) {
                        Column {
                            // Row 1: Book icon + Business Name + Edit pencil icon + Language selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onSetupStoreDetailsClick)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = "Store",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = profile.businessName.ifBlank { "My Business" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Business Name",
                                        tint = Color(0xFFBBDEFB),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onLanguageClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("language_selector_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Change Language",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Row 2: CUSTOMERS & SUPPLIERS Tabs (Requirement #5: keep customer and supplier for users on get n give in 1 page)
                            TabRow(
                                selectedTabIndex = if (selectedPartyType == PartyType.CUSTOMER) 0 else 1,
                                containerColor = PrimaryNavy,
                                contentColor = Color.White,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(
                                            if (selectedPartyType == PartyType.CUSTOMER) tabPositions[0] else tabPositions[1]
                                        ),
                                        color = Color(0xFFFFB300),
                                        height = 3.dp
                                    )
                                },
                                divider = {}
                            ) {
                                Tab(
                                    selected = selectedPartyType == PartyType.CUSTOMER,
                                    onClick = { onPartyTypeSelected(PartyType.CUSTOMER) },
                                    text = {
                                        Text(
                                            text = strings.customers,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selectedPartyType == PartyType.CUSTOMER) Color.White else Color(0xFFBBDEFB)
                                        )
                                    }
                                )
                                Tab(
                                    selected = selectedPartyType == PartyType.SUPPLIER,
                                    onClick = { onPartyTypeSelected(PartyType.SUPPLIER) },
                                    text = {
                                        Text(
                                            text = strings.suppliers,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selectedPartyType == PartyType.SUPPLIER) Color.White else Color(0xFFBBDEFB)
                                        )
                                    }
                                )
                            }

                            // Row 3: Light Lilac Card with "You will give ₹0 | You will get ₹852" & "PDF View Reports"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF6)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = strings.youWillGive,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "₹ ${fmt.format(abs(totalGive))}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = TextPrimary
                                                )
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = strings.youWillGet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "₹ ${fmt.format(abs(totalGet))}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (totalGet > 0) CreditRed else TextPrimary
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFD6DBE5))

                                        // "PDF View Reports" Action Strip
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                                                .clickable(onClick = onPdfReportClick)
                                                .padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = "View Reports",
                                                tint = PrimaryNavy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = strings.viewReports,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryNavy
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Row 4: Search Bar with "Search Customer" + Filters + Cashbook shortcut (Exact match to Photo 1)
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        text = if (selectedPartyType == PartyType.CUSTOMER) "Search Customer" else "Search Supplier",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = PrimaryNavy,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = PrimaryNavy,
                                    unfocusedBorderColor = Color(0xFFD6DBE5)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("party_search_input")
                            )

                            // Cashbook Shortcut Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6DBE5)),
                                color = Color.White,
                                modifier = Modifier
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenCashbook)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "₹",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cashbook",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Party List (Exact match to Photo 1 with initials, name, time, amount, and REMIND >)
                if (filteredParties.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (selectedPartyType == PartyType.CUSTOMER) "No customers added yet" else "No suppliers added yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ ADD CUSTOMER' below to start recording khata entries.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyCard(
                            party = party,
                            currencySymbol = currency,
                            onClick = { onPartyClick(party) },
                            onRemindClick = { onRemindParty(party) }
                        )
                    }
                }
            }
        }
    }
}
