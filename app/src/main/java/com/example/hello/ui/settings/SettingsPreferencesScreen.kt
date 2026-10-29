package com.example.hello.ui.settings

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

@Composable
fun SettingsPreferencesScreen(
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

    val cadenceOptions = listOf(
        14 to Pair("2 Weeks (14d)", "High-cadence check-ins for active relationships"),
        30 to Pair("1 Month (30d)", "Standard monthly rhythm (recommended default)"),
        60 to Pair("2 Months (60d)", "Bi-monthly check-ins for steady connections"),
        90 to Pair("Quarterly (90d)", "Seasonal check-ins for extended network")
    )

    val notificationTimes = listOf(
        8 to Pair("08:00 AM", "Early morning reminder delivery"),
        9 to Pair("09:00 AM", "Standard morning delivery (default)"),
        10 to Pair("10:00 AM", "Mid-morning delivery"),
        18 to Pair("06:00 PM", "Evening review delivery")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PixelStyleTopBar(
                title = "Preferences",
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
                SettingsSubsection(title = "Default Reconnection Cadence") {
                    cadenceOptions.forEachIndexed { index, (days, info) ->
                        SelectableSettingItem(
                            title = info.first,
                            subtitle = info.second,
                            selected = uiState.defaultCadenceDays == days,
                            onClick = { onEvent(SettingsUiEvent.OnSetDefaultCadenceDays(days)) },
                            shape = shapeFor(index, cadenceOptions.size)
                        )
                    }
                }
            }

            item {
                SettingsSubsection(title = "Reminder Notification Delivery") {
                    notificationTimes.forEachIndexed { index, (hour, info) ->
                        SelectableSettingItem(
                            title = info.first,
                            subtitle = info.second,
                            selected = uiState.notificationHour == hour,
                            onClick = { onEvent(SettingsUiEvent.OnSetNotificationTime(hour, 0)) },
                            shape = shapeFor(index, notificationTimes.size)
                        )
                    }
                }
            }

            item {
                SettingsSubsection(title = "Encounter Capture") {
                    SwitchSettingItem(
                        title = "Default GPS Location",
                        subtitle = "Pre-populate venue coordinates strictly at the instant of an encounter (foreground only)",
                        checked = uiState.defaultIncludeLocation,
                        onCheckedChange = { onEvent(SettingsUiEvent.OnToggleDefaultIncludeLocation(it)) },
                        shape = shapeFor(0, 1)
                    )
                }
            }
        }
    }
}
