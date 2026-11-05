package com.example.hello.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.CircleGroupConfig
import com.example.hello.domain.model.GroupIconRegistry
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.FeatherIconSize

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupDrawerContent(
    selectedCircle: Circle?,
    circleConfigs: Map<Circle, CircleGroupConfig> = emptyMap(),
    onSelectCircle: (Circle?) -> Unit,
    onUpdateCircleConfig: ((CircleGroupConfig) -> Unit)? = null,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isCustomizingIcon by remember { mutableStateOf(false) }
    var customizingCircle by remember { mutableStateOf<Circle?>(null) }
    var allGroupsIconKey by remember { mutableStateOf(GroupIconRegistry.allGroupsIconKey) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = 24.dp
                )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Groups",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Filter by relationship group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onCloseDrawer) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Drawer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable List of Groups
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Groups" Item
                GroupListItem(
                    title = "All Groups",
                    iconKey = allGroupsIconKey,
                    isSelected = selectedCircle == null,
                    onClick = { onSelectCircle(null) },
                    onCustomizeIconClick = if (onUpdateCircleConfig != null) {
                        {
                            customizingCircle = null
                            isCustomizingIcon = true
                        }
                    } else {
                        null
                    }
                )

                Circle.entries.forEach { circle ->
                    val currentConfig = circleConfigs[circle] ?: CircleGroupConfig(circle = circle)
                    GroupListItem(
                        title = circle.displayName,
                        iconKey = currentConfig.iconKey,
                        isSelected = selectedCircle == circle,
                        onClick = { onSelectCircle(circle) },
                        onCustomizeIconClick = if (onUpdateCircleConfig != null) {
                            {
                                customizingCircle = circle
                                isCustomizingIcon = true
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    if (isCustomizingIcon && onUpdateCircleConfig != null) {
        val targetCircle = customizingCircle
        val currentIconKey = if (targetCircle != null) {
            (circleConfigs[targetCircle] ?: CircleGroupConfig(circle = targetCircle)).iconKey
        } else {
            allGroupsIconKey
        }
        val groupLabel = targetCircle?.displayName ?: "All Groups"
        AlertDialog(
            onDismissRequest = { isCustomizingIcon = false },
            title = { Text("Customize $groupLabel Icon") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HelloIconKey.customizableGroupIcons.forEach { candidateKey ->
                        val isCurrent = currentIconKey == candidateKey
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                                .clickable {
                                    if (targetCircle != null) {
                                        val currentConfig = circleConfigs[targetCircle]
                                            ?: CircleGroupConfig(circle = targetCircle)
                                        onUpdateCircleConfig(currentConfig.copy(iconKey = candidateKey))
                                    } else {
                                        GroupIconRegistry.setIconKey(null, candidateKey)
                                        allGroupsIconKey = candidateKey
                                    }
                                    isCustomizingIcon = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            CustomizableIcon(
                                iconKey = candidateKey,
                                size = FeatherIconSize.UI,
                                tint = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                contentDescription = candidateKey.label
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isCustomizingIcon = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun GroupListItem(
    title: String,
    iconKey: HelloIconKey,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCustomizeIconClick: (() -> Unit)?
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        label = "GroupItemBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .then(
                        if (onCustomizeIconClick != null) {
                            Modifier.clickable(onClick = onCustomizeIconClick)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CustomizableIcon(
                    iconKey = iconKey,
                    size = FeatherIconSize.UI,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    contentDescription = title
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
