package com.example.hello.ui.contacts

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.ui.home.components.CircleBadge
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onEvent: (ContactsUiEvent) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onContactClick: (String) -> Unit = { onEvent(ContactsUiEvent.OnContactClick(it)) },
    onAddContactClick: () -> Unit = {},
    terminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() / 2
            )
    ) {
        // 1. Search Bar & Header
        ContactsSearchHeader(
            query = uiState.searchQuery,
            onQueryChange = { onEvent(ContactsUiEvent.OnSearchQueryChange(it)) },
            onClear = { onEvent(ContactsUiEvent.OnClearSearch) },
            totalCount = uiState.totalCount,
            terminology = terminology,
            onNavigateToHome = onNavigateToHome,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // 2. Circle Filter Chips & Sort Toggle
        ContactsFilterBar(
            selectedCircle = uiState.selectedCircle,
            onSelectCircle = { onEvent(ContactsUiEvent.OnSelectCircle(it)) },
            sortMode = uiState.sortMode,
            onToggleSort = { onEvent(ContactsUiEvent.OnToggleSortMode(it)) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 3. Contacts List View or Empty State
        if (uiState.contacts.isEmpty()) {
            val isFilterActive = uiState.searchQuery.isNotBlank() || uiState.selectedCircle != null
            ContactsEmptyState(
                isFilterActive = isFilterActive,
                query = uiState.searchQuery,
                terminology = terminology,
                onClearFilters = { onEvent(ContactsUiEvent.OnClearFilters) },
                onAddContactClick = onAddContactClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 32.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.groupedContacts.forEach { (header, contactsInGroup) ->
                    stickyHeader(key = "header-$header") {
                        ContactGroupHeader(
                            title = header,
                            count = contactsInGroup.size
                        )
                    }

                    items(
                        items = contactsInGroup,
                        key = { it.id }
                    ) { contact ->
                        MinimalistContactRow(
                            contact = contact,
                            onClick = { onContactClick(contact.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    totalCount: Int,
    terminology: RelationshipTerminology,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = terminology.pluralTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = terminology.networkCounterText(totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick shortcut back to Home (animates dock pill to Home)
            val homeBadgeInteractionSource = remember { MutableInteractionSource() }
            val isHomeBadgePressed by homeBadgeInteractionSource.collectIsPressedAsState()
            val homeBadgeScale by animateFloatAsState(
                targetValue = if (isHomeBadgePressed) MotionTokens.BADGE_PRESSED_SCALE else 1.0f,
                animationSpec = MotionTokens.badgePressSpring(),
                label = "homeBadgeScale"
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = homeBadgeScale
                        scaleY = homeBadgeScale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = homeBadgeInteractionSource,
                        indication = null,
                        onClick = onNavigateToHome
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Home",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Rounded Organic Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search contacts, notes, roles...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContactsFilterBar(
    selectedCircle: Circle?,
    onSelectCircle: (Circle?) -> Unit,
    sortMode: ContactSortMode,
    onToggleSort: (ContactSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle Filter Chips (All, Work, Friends, Family, Uni)
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCircle == null,
                    onClick = { onSelectCircle(null) },
                    label = { Text("All") },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            items(Circle.entries.toTypedArray()) { circle ->
                val isSelected = selectedCircle == circle
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectCircle(if (isSelected) null else circle)
                    },
                    label = { Text(circle.displayName) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        // Sort Mode Toggle Pill
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .clickable {
                    val next = if (sortMode == ContactSortMode.ALPHABETICAL) {
                        ContactSortMode.RECENTLY_ENCOUNTERED
                    } else {
                        ContactSortMode.ALPHABETICAL
                    }
                    onToggleSort(next)
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = sortMode.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ContactGroupHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinimalistContactRow(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tactile Avatar
            DoodleAvatar(
                initials = contact.avatarInitials,
                bgIndex = contact.avatarBgIndex,
                size = 48.dp,
                shape = RoundedCornerShape(16.dp)
            )

            // Info & Tags
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    contact.lastEncounterText?.let { encounter ->
                        Text(
                            text = "• $encounter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = contact.contextSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Circle Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    contact.circles.forEach { circle ->
                        CircleBadge(circle = circle)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsEmptyState(
    isFilterActive: Boolean,
    query: String,
    terminology: RelationshipTerminology,
    onClearFilters: () -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isFilterActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFilterActive) "🔍" else "👋",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Text(
                text = if (isFilterActive) {
                    "No matching ${terminology.pluralTitle.lowercase()}"
                } else {
                    terminology.emptyDirectoryTitle()
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isFilterActive) {
                    if (query.isNotBlank()) {
                        "No ${terminology.pluralTitle.lowercase()} found matching \"$query\". Try a different keyword or reset filters."
                    } else {
                        "No ${terminology.pluralTitle.lowercase()} found in this circle sphere."
                    }
                } else {
                    "Your circle is waiting! Start adding ${terminology.pluralTitle.lowercase()} and logging encounters to keep your relationships alive."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFilterActive) {
                Button(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Clear filters", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Button(
                    onClick = onAddContactClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = terminology.emptyDirectoryPrompt(),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Previews
// -------------------------------------------------------------

@Preview(name = "Contacts Screen Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ContactsScreenLightPreview() {
    val sampleState = ContactsViewModel.computeState(
        searchQuery = "",
        selectedCircle = null,
        sortMode = ContactSortMode.ALPHABETICAL,
        contacts = ContactsViewModel.createSampleContacts()
    )
    HelloTheme(darkTheme = false) {
        ContactsScreen(
            uiState = sampleState,
            onEvent = {}
        )
    }
}

@Preview(name = "Contacts Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 844)
@Composable
fun ContactsScreenDarkPreview() {
    val sampleState = ContactsViewModel.computeState(
        searchQuery = "",
        selectedCircle = null,
        sortMode = ContactSortMode.ALPHABETICAL,
        contacts = ContactsViewModel.createSampleContacts()
    )
    HelloTheme(darkTheme = true) {
        ContactsScreen(
            uiState = sampleState,
            onEvent = {}
        )
    }
}

@Preview(name = "Contacts Empty State", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ContactsScreenEmptyPreview() {
    val emptyState = ContactsUiState(
        searchQuery = "Unknown Person",
        contacts = emptyList(),
        groupedContacts = emptyMap()
    )
    HelloTheme(darkTheme = false) {
        ContactsScreen(
            uiState = emptyState,
            onEvent = {},
            terminology = RelationshipTerminology.FRIENDS
        )
    }
}
