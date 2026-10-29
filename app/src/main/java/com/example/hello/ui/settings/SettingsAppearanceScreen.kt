package com.example.hello.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hello.data.preferences.ThemeMode

@Composable
fun SettingsAppearanceScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = "Appearance",
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSubsection(title = "Theme Mode") {
                    val entries = ThemeMode.entries
                    entries.forEachIndexed { index, mode ->
                        SelectableSettingItem(
                            title = mode.title,
                            subtitle = when (mode) {
                                ThemeMode.SYSTEM -> "Follow device dark mode settings"
                                ThemeMode.LIGHT -> "Always use warm cream & terracotta light theme"
                                ThemeMode.DARK -> "Always use dark oatmeal & burnt orange theme"
                            },
                            selected = uiState.themeMode == mode,
                            onClick = { onEvent(SettingsUiEvent.OnSelectThemeMode(mode)) },
                            shape = shapeFor(index, entries.size)
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingsSubsection(title = "Material You") {
                        SwitchSettingItem(
                            title = "Dynamic Color",
                            subtitle = "Derive accents and surfaces dynamically from system wallpaper",
                            checked = uiState.dynamicColor,
                            onCheckedChange = { onEvent(SettingsUiEvent.OnToggleDynamicColor(it)) },
                            shape = shapeFor(0, 1)
                        )
                    }
                }
            }

            item {
                SettingsSubsection(title = "Motion & Accessibility") {
                    SwitchSettingItem(
                        title = "Reduce Animations",
                        subtitle = "Turn off transitions and spring animations for instant 0ms cuts and reduced motion",
                        checked = uiState.reduceAnimations,
                        onCheckedChange = { onEvent(SettingsUiEvent.OnToggleReduceAnimations(it)) },
                        shape = shapeFor(0, 1)
                    )
                }
            }
        }
    }
}
