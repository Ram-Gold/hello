package com.example.hello.ui.contacts

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.TagColorPalette
import com.example.hello.platform.portability.VCardShareHelper
import com.example.hello.ui.contacts.components.ArchivedContactsSheet
import com.example.hello.ui.contacts.components.PeopleFavoritesSection
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.ContactCard
import com.example.hello.ui.components.TonalFilterPill
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.ui.contacts.components.ContactSelectionActionTray
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.settings.shapeFor
import com.example.hello.ui.theme.HelloTheme

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
    val context = LocalContext.current

    BackHandler(enabled = uiState.isSelectionMode) {
        onEvent(ContactsUiEvent.OnClearSelection)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() / 2
                )
        ) {
            // 1. Search Bar & Header / Contextual Selection Header
            AnimatedContent(
                targetState = uiState.isSelectionMode,
                label = "ContactsHeaderTransition"
            ) { isSelectionMode ->
                if (isSelectionMode) {
                    ContactsSelectionHeader(
                        selectedCount = uiState.selectedContactIds.size,
                        onClearSelection = { onEvent(ContactsUiEvent.OnClearSelection) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    ContactsSearchHeader(
                        query = uiState.searchQuery,
                        onQueryChange = { onEvent(ContactsUiEvent.OnSearchQueryChange(it)) },
                        onClear = { onEvent(ContactsUiEvent.OnClearSearch) },
                        totalCount = uiState.totalCount,
                        terminology = terminology,
                        onNavigateToHome = onNavigateToHome,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // 2. Smart #tag Filter Pills & Sort Toggle
            val activeTagFilters = uiState.selectedTags
            val activeTagFilter = activeTagFilters.joinToString(", ").ifBlank { null }
            AnimatedVisibility(
                visible = !uiState.isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ContactsFilterBar(
                    selectedTags = activeTagFilters,
                    smartFilterTags = uiState.smartFilterTags,
                    availableTags = uiState.availableTags,
                    onSelectTag = { onEvent(ContactsUiEvent.OnSelectTag(it)) },
                    sortMode = uiState.sortMode,
                    onToggleSort = { onEvent(ContactsUiEvent.OnToggleSortMode(it)) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 3. Contacts List View or Empty State
            if (uiState.contacts.isEmpty()) {
                val isFilterActive = uiState.searchQuery.isNotBlank() || activeTagFilters.isNotEmpty()
                ContactsEmptyState(
                    isFilterActive = isFilterActive,
                    query = uiState.searchQuery,
                    selectedTag = activeTagFilter,
                    terminology = terminology,
                    onClearFilters = { onEvent(ContactsUiEvent.OnClearFilters) },
                    onAddContactClick = onAddContactClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!uiState.isSelectionMode && uiState.searchQuery.isBlank() && activeTagFilters.isEmpty() && uiState.favoriteContacts.isNotEmpty()) {
                        item(
                            key = "people_favorites_strip",
                            contentType = "favorites_strip"
                        ) {
                            PeopleFavoritesSection(
                                favorites = uiState.favoriteContacts,
                                onContactClick = onContactClick,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }

                    uiState.groupedContacts.forEach { (header, contactsInGroup) ->
                        stickyHeader(
                            key = "header-$header",
                            contentType = "group_header"
                        ) {
                            ContactGroupHeader(
                                title = header,
                                count = contactsInGroup.size
                            )
                        }

                        itemsIndexed(
                            items = contactsInGroup,
                            key = { _, contact -> contact.id },
                            contentType = { _, _ -> "contact_card" }
                        ) { index, contact ->
                            val isSelected = contact.id in uiState.selectedContactIds
                            ContactCard(
                                contact = contact,
                                isSelected = isSelected,
                                shape = if (isSelected) RoundedCornerShape(24.dp) else shapeFor(index, contactsInGroup.size),
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        onEvent(ContactsUiEvent.OnToggleSelectContact(contact.id))
                                    } else {
                                        onContactClick(contact.id)
                                    }
                                },
                                onLongClick = {
                                    onEvent(ContactsUiEvent.OnContactLongClick(contact.id))
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Pull-up footer indicator at the end of the contacts list (ADR-0028)
                    item(key = "archived-pull-up-footer") {
                        ArchivedPullUpFooter(
                            archivedCount = uiState.archivedContacts.size,
                            onClick = { onEvent(ContactsUiEvent.OnOpenArchivedSheet) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    }
                }
            }
        }

        // Floating Action Tray on the right edge
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            ContactSelectionActionTray(
                onShareClick = {
                    val selectedContacts = uiState.contacts.filter { it.id in uiState.selectedContactIds }
                    VCardShareHelper.shareVCard(context, selectedContacts)
                },
                onDeleteClick = {
                    onEvent(ContactsUiEvent.OnRequestDeleteSelected)
                },
                onSelectAllClick = {
                    onEvent(ContactsUiEvent.OnSelectAllVisibleContacts)
                }
            )
        }

        // Batch Deletion Confirmation Dialog
        if (uiState.isDeleteConfirmationVisible) {
            val count = uiState.selectedContactIds.size
            AlertDialog(
                onDismissRequest = { onEvent(ContactsUiEvent.OnDismissDeleteDialog) },
                title = {
                    Text(
                        text = "Delete $count contact${if (count > 1) "s" else ""}?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "All notes, encounters, and reminders associated with these contacts will be permanently removed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onEvent(ContactsUiEvent.OnConfirmDeleteSelected) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(ContactsUiEvent.OnDismissDeleteDialog) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        // Archived Contacts Pull-Up Sheet (ADR-0028)
        ArchivedContactsSheet(
            isOpen = uiState.isArchivedSheetOpen,
            archivedContacts = uiState.archivedContacts,
            onDismiss = { onEvent(ContactsUiEvent.OnDismissArchivedSheet) },
            onRestoreContact = { onEvent(ContactsUiEvent.OnRestoreContact(it)) }
        )
    }
}

@Composable
private fun ArchivedPullUpFooter(
    archivedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDragEnd = {
                        if (accumulatedDrag < -40f) {
                            onClick()
                        }
                        accumulatedDrag = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        if (accumulatedDrag < -80f) {
                            onClick()
                        }
                    }
                )
            }
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Pull up for archived",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Pull up for archived ($archivedCount)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactsSelectionHeader(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        FilledIconButton(
            onClick = onClearSelection,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancel selection",
                modifier = Modifier.size(20.dp)
            )
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Material 3 Expressive Top Bar Row matching Settings PixelStyleTopBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = terminology.pluralTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "$totalCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Material 3 Expressive Pill Search Bar (Zero Border, surfaceContainer)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = terminology.searchPlaceholder(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
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
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContactsFilterBar(
    selectedTags: Set<String>,
    smartFilterTags: List<String>,
    availableTags: List<Tag> = emptyList(),
    onSelectTag: (String?) -> Unit,
    sortMode: ContactSortMode,
    onToggleSort: (ContactSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Full-width horizontal scrolling Smart #tag Filter Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "filter-all") {
                val isAllSelected = selectedTags.isEmpty()
                TonalFilterPill(
                    label = "All",
                    isSelected = isAllSelected,
                    unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { onSelectTag(null) }
                )
            }

            items(
                items = smartFilterTags,
                key = { it }
            ) { tagName ->
                val cleanTagName = tagName.removePrefix("#")
                val isSelected = selectedTags.any { it.equals(cleanTagName, ignoreCase = true) }
                val customTag = availableTags.firstOrNull { it.name.equals(cleanTagName, ignoreCase = true) }
                val tagColorHex = customTag?.colorHex ?: TagColorPalette.getDeterministicColorHex(cleanTagName)
                val tag = customTag?.copy(name = cleanTagName) ?: Tag(
                    id = "filter-$cleanTagName",
                    name = cleanTagName,
                    colorHex = tagColorHex
                )
                TonalTagPill(
                    tag = tag,
                    isSelected = isSelected,
                    onClick = {
                        onSelectTag(cleanTagName)
                    }
                )
            }
        }

        // Trailing Sort Button with Smooth Left-Edge Gradient Fade
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            // Horizontal gradient fade scrim from transparent to background color
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(36.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor
                            )
                        )
                    )
            )

            // Solid container behind Sort Toggle (guarantees no scrolling chips bleed through)
            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .padding(end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val next = if (sortMode == ContactSortMode.ALPHABETICAL) {
                                ContactSortMode.RECENTLY_ENCOUNTERED
                            } else {
                                ContactSortMode.ALPHABETICAL
                            }
                            onToggleSort(next)
                        }
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
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
    }
}

@Composable
private fun ContactGroupHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun MinimalistContactRow(
    contact: ContactSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) {
    ContactCard(
        contact = contact,
        onClick = onClick,
        shape = shape,
        modifier = modifier
    )
}

@Composable
fun ContactsEmptyState(
    isFilterActive: Boolean,
    query: String,
    terminology: RelationshipTerminology,
    onClearFilters: () -> Unit,
    onAddContactClick: () -> Unit,
    selectedTag: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
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
                CustomizableIcon(
                    iconKey = if (isFilterActive) HelloIconKey.SEARCH else HelloIconKey.USER_PLUS,
                    size = 26.dp,
                    tint = if (isFilterActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
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
                    when {
                        query.isNotBlank() ->
                            "No ${terminology.pluralTitle.lowercase()} found matching \"$query\". Try a different keyword or reset filters."
                        selectedTag != null ->
                            terminology.emptyTagFilterMessage(selectedTag)
                        else ->
                            "No ${terminology.pluralTitle.lowercase()} match your active filter."
                    }
                } else {
                    "Your directory is ready! Start adding ${terminology.pluralTitle.lowercase()}, tagging life spheres (friends, work), and logging encounters."
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

@Preview(name = "Contacts Selection Mode Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun ContactsSelectionModeLightPreview() {
    val sampleContacts = ContactsViewModel.createSampleContacts()
    val sampleState = ContactsViewModel.computeState(
        searchQuery = "",
        sortMode = ContactSortMode.ALPHABETICAL,
        contacts = sampleContacts,
        selectedContactIds = setOf(sampleContacts[0].id, sampleContacts[1].id)
    )
    HelloTheme(darkTheme = false) {
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
