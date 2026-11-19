package com.example.hello.ui.avatar

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.domain.model.NotionistBackgroundSwatch
import com.example.hello.domain.model.NotionistCatalog
import com.example.hello.domain.model.NotionistOption
import com.example.hello.domain.model.NotionistThumbnailUrlBuilder
import com.example.hello.domain.model.NotionistTraitCategory
import com.example.hello.platform.avatar.NotionistThumbnailMemoryCache
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.TonalFilterPill
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvatarPlaygroundScreen(
    uiState: AvatarPlaygroundUiState,
    onEvent: (AvatarPlaygroundUiEvent) -> Unit,
    loadTraitThumbnail: suspend (NotionistTraitCategory, String) -> ByteArray? = { _, _ -> null },
    onNavigateBack: () -> Unit,
    onApplyAvatar: (pngBytes: ByteArray, configJson: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHelloHaptics.current

    BackHandler(enabled = uiState.hasUnsavedChanges) {
        onEvent(AvatarPlaygroundUiEvent.OnBackClick)
    }

    val decodedPreview = remember(uiState.preview?.requestUrl, uiState.preview?.bytes?.size) {
        val bytes = uiState.preview?.bytes ?: return@remember null
        runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Avatar Playground",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.contactName.isNotBlank()) {
                            Text(
                                text = uiState.contactName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) {
                            onEvent(AvatarPlaygroundUiEvent.OnBackClick)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val bytes = uiState.preview?.bytes ?: return@Button
                            haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                            onApplyAvatar(bytes, uiState.config.toJson())
                        },
                        enabled = uiState.canApply,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isOffline && uiState.preview == null) {
            OfflinePlaygroundGate(
                onRetry = {
                    haptics.perform(SemanticHapticType.MODE_TOGGLE)
                    onEvent(AvatarPlaygroundUiEvent.OnRetry)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            )
        } else {
            val bottomNavPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Top Live Preview Stage
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(164.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (decodedPreview != null) {
                                Image(
                                    bitmap = decodedPreview,
                                    contentDescription = "Notionist avatar preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (!uiState.isLoadingPreview) {
                                CustomizableIcon(
                                    iconKey = HelloIconKey.USER,
                                    size = 72.dp,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.isLoadingPreview) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(
                                                alpha = if (decodedPreview != null) 0.45f else 0.9f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(AvatarPlaygroundUiEvent.OnRandomize)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Randomize",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Non-destructive offline or fetch error notice while preserving user selections
                if (uiState.isOffline || uiState.errorMessage != null) {
                    val noticeMessage = if (uiState.isOffline) {
                        "You're offline. Reconnect to preview changes — your selections are kept."
                    } else {
                        uiState.errorMessage.orEmpty()
                    }
                    InlinePlaygroundNotice(
                        isOffline = uiState.isOffline,
                        message = noticeMessage,
                        onRetry = {
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onEvent(AvatarPlaygroundUiEvent.OnRetry)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 12.dp)
                    )
                }

                // Category Tab Strip (pill-to-squircle morph matching People page filter tags)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(NotionistTraitCategory.entries, key = { it.name }) { category ->
                        val isSelected = category == uiState.activeCategory
                        TonalFilterPill(
                            label = category.displayLabel,
                            isSelected = isSelected,
                            unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(AvatarPlaygroundUiEvent.OnSelectCategory(category))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Category Controls + Stepper
                val activeCategory = uiState.activeCategory
                val selectedLabel = uiState.config.selectedOptionLabel(activeCategory)
                val selectedId = uiState.config.selectedOptionId(activeCategory)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeCategory.displayLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(AvatarPlaygroundUiEvent.OnStepOption(-1))
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Previous option",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onEvent(AvatarPlaygroundUiEvent.OnStepOption(1))
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Next option",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeCategory == NotionistTraitCategory.BACKGROUND) {
                    BackgroundSwatchGrid(
                        swatches = NotionistCatalog.backgroundSwatches,
                        selectedHex = uiState.config.backgroundColor,
                        onSelectSwatch = { hex ->
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onEvent(AvatarPlaygroundUiEvent.OnSelectOption(hex))
                        },
                        contentBottomPadding = bottomNavPadding + 24.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    TraitThumbnailGrid(
                        category = activeCategory,
                        options = NotionistCatalog.optionsFor(activeCategory),
                        selectedOptionId = selectedId,
                        retryGeneration = uiState.thumbnailRetryGeneration,
                        loadTraitThumbnail = loadTraitThumbnail,
                        onSelectOption = { id ->
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onEvent(AvatarPlaygroundUiEvent.OnSelectOption(id))
                        },
                        contentBottomPadding = bottomNavPadding + 24.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(AvatarPlaygroundUiEvent.OnDismissDiscardDialog) },
            shape = RoundedCornerShape(8.dp),
            title = { Text("Discard avatar changes?") },
            text = { Text("Your customizations in Avatar Playground will not be applied.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(AvatarPlaygroundUiEvent.OnConfirmDiscard)
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(AvatarPlaygroundUiEvent.OnDismissDiscardDialog) }) {
                    Text("Keep editing")
                }
            }
        )
    }
}

@Composable
private fun OfflinePlaygroundGate(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "You're offline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Avatar Playground requires an internet connection to customize a Notionist avatar. Connect to the internet and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun InlinePlaygroundNotice(
    isOffline: Boolean,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Rounded.WifiOff else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun TraitThumbnailGrid(
    category: NotionistTraitCategory,
    options: List<NotionistOption>,
    selectedOptionId: String?,
    retryGeneration: Int,
    loadTraitThumbnail: suspend (NotionistTraitCategory, String) -> ByteArray?,
    onSelectOption: (String?) -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 2.dp,
            bottom = contentBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        items(
            items = options,
            key = { "${category.name}_${it.id ?: "none"}" }
        ) { option ->
            val isSelected = option.id == selectedOptionId
            TraitThumbnailTile(
                category = category,
                option = option,
                isSelected = isSelected,
                retryGeneration = retryGeneration,
                loadTraitThumbnail = loadTraitThumbnail,
                onClick = { onSelectOption(option.id) }
            )
        }
    }
}

@Composable
private fun TraitThumbnailTile(
    category: NotionistTraitCategory,
    option: NotionistOption,
    isSelected: Boolean,
    retryGeneration: Int,
    loadTraitThumbnail: suspend (NotionistTraitCategory, String) -> ByteArray?,
    onClick: () -> Unit
) {
    val optionId = option.id
    val cacheKey = remember(category, optionId) {
        optionId?.let { NotionistThumbnailUrlBuilder.cacheKey(category, it) }
    }

    var thumbnailBitmap by remember(category, optionId) {
        val cachedBytes = cacheKey?.let { NotionistThumbnailMemoryCache.get(it) }
        val decoded = cachedBytes?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
        }
        mutableStateOf<ImageBitmap?>(decoded)
    }

    LaunchedEffect(category, optionId, retryGeneration) {
        if (optionId != null && thumbnailBitmap == null) {
            val bytes = loadTraitThumbnail(category, optionId)
            if (bytes != null) {
                val decoded = withContext(Dispatchers.Default) {
                    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
                }
                if (decoded != null) {
                    thumbnailBitmap = decoded
                }
            }
        }
    }

    val tileBackground = when {
        optionId == null -> MaterialTheme.colorScheme.surfaceVariant
        thumbnailBitmap != null -> Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = tileBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics {
                contentDescription = "${category.displayLabel} ${option.label}"
                role = Role.RadioButton
                selected = isSelected
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                optionId == null -> {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                thumbnailBitmap != null -> {
                    Image(
                        bitmap = thumbnailBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // Calm neutral placeholder while queued or uncached-offline (remains tappable)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundSwatchGrid(
    swatches: List<NotionistBackgroundSwatch>,
    selectedHex: String,
    onSelectSwatch: (String) -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 2.dp,
            bottom = contentBottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        items(
            items = swatches,
            key = { it.hex }
        ) { swatch ->
            val isSelected = swatch.hex.equals(selectedHex, ignoreCase = true)
            Surface(
                onClick = { onSelectSwatch(swatch.hex) },
                shape = RoundedCornerShape(8.dp),
                color = Color(swatch.argb),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .semantics {
                        contentDescription = "Background ${swatch.label}"
                        role = Role.RadioButton
                        selected = isSelected
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
