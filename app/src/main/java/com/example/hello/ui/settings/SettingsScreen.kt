package com.example.hello.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.ui.theme.HelloTheme

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToTerminology: () -> Unit = {},
    onNavigateToTags: () -> Unit = {},
    onNavigateToData: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToPreferences: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onEvent(SettingsUiEvent.OnDismissUserMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PixelStyleTopBar(
                title = "Settings",
                onNavigateBack = onNavigateBack,
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val totalItems = 6
                ExpressiveSettingsGroup {
                    // 1. Terminology
                    val termColors = if (isDark) {
                        Color(0xFF5E1700) to Color(0xFFFFB59B)
                    } else {
                        Color(0xFFFFDBCF) to Color(0xFFB84A1A)
                    }
                    ExpressiveNavigationItem(
                        title = "Display Terminology",
                        subtitle = "${uiState.terminology.pluralTitle} • Active presentation vocabulary",
                        icon = Icons.Rounded.Tune,
                        colors = termColors,
                        onClick = onNavigateToTerminology,
                        shape = shapeFor(0, totalItems)
                    )

                    // 2. Tag Taxonomy
                    val tagColors = if (isDark) {
                        Color(0xFF2B4426) to Color(0xFFB5CBAE)
                    } else {
                        Color(0xFFE2EEDF) to Color(0xFF436B3D)
                    }
                    ExpressiveNavigationItem(
                        title = "Tag Taxonomy",
                        subtitle = "${uiState.tags.size} active relationship labels",
                        icon = Icons.AutoMirrored.Rounded.Label,
                        colors = tagColors,
                        onClick = onNavigateToTags,
                        shape = shapeFor(1, totalItems),
                        trailingBadge = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "${uiState.tags.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    )

                    // 3. Data & Sovereign Storage
                    val dataColors = if (isDark) {
                        Color(0xFF1E3852) to Color(0xFFB8D4EE)
                    } else {
                        Color(0xFFDBE7F3) to Color(0xFF265077)
                    }
                    ExpressiveNavigationItem(
                        title = "Data & Sovereign Storage",
                        subtitle = "SQLite canonical truth, export bundles, and backups",
                        icon = Icons.Rounded.Storage,
                        colors = dataColors,
                        onClick = onNavigateToData,
                        shape = shapeFor(2, totalItems)
                    )

                    // 4. Appearance
                    val appColors = if (isDark) {
                        Color(0xFF4C182C) to Color(0xFFFFAFD0)
                    } else {
                        Color(0xFFFFD8E4) to Color(0xFF7D2B4A)
                    }
                    val motionSubtitle = if (uiState.reduceAnimations) "Reduced motion (0ms)" else "Standard motion"
                    ExpressiveNavigationItem(
                        title = "Appearance",
                        subtitle = "${uiState.themeMode.title} • $motionSubtitle",
                        icon = Icons.Rounded.Palette,
                        colors = appColors,
                        onClick = onNavigateToAppearance,
                        shape = shapeFor(3, totalItems)
                    )

                    // 5. Preferences
                    val prefColors = if (isDark) {
                        Color(0xFF522E00) to Color(0xFFFFB77C)
                    } else {
                        Color(0xFFFFE0C2) to Color(0xFF8C4C00)
                    }
                    ExpressiveNavigationItem(
                        title = "Preferences",
                        subtitle = "Cadence: ${uiState.defaultCadenceDays}d • Notif: ${uiState.notificationHour}:00 • Location",
                        icon = Icons.Rounded.Settings,
                        colors = prefColors,
                        onClick = onNavigateToPreferences,
                        shape = shapeFor(4, totalItems)
                    )

                    // 6. About Hello
                    val aboutColors = if (isDark) {
                        Color(0xFF383431) to Color(0xFFD6CEC7)
                    } else {
                        Color(0xFFEBE5DF) to Color(0xFF4B4642)
                    }
                    ExpressiveNavigationItem(
                        title = "About Hello",
                        subtitle = "v1.0.0-alpha • 100% Local-First & Zero Backend",
                        icon = Icons.Rounded.Info,
                        colors = aboutColors,
                        onClick = onNavigateToAbout,
                        shape = shapeFor(5, totalItems)
                    )
                }
            }
        }
    }
}

@Preview(name = "Settings Hub Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenLightPreview() {
    HelloTheme(darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(terminology = RelationshipTerminology.PEOPLE),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Settings Hub Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenDarkPreview() {
    HelloTheme(darkTheme = true) {
        SettingsScreen(
            uiState = SettingsUiState(terminology = RelationshipTerminology.FRIENDS),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
