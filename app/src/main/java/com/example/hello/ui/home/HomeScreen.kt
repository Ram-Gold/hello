package com.example.hello.ui.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.home.components.HomeTopBar
import com.example.hello.ui.home.components.RecentEncountersSection
import com.example.hello.ui.home.components.UpcomingFeedSection
import com.example.hello.ui.theme.HelloTheme

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    showBottomDock: Boolean = true,
    onNavigateToPeople: () -> Unit = { onEvent(HomeUiEvent.OnNavTabSelect(HomeNavTab.PEOPLE)) },
    onContactClick: (String) -> Unit = { onEvent(HomeUiEvent.OnContactClick(it)) },
    onSettingsClick: () -> Unit = { onEvent(HomeUiEvent.OnSettingsClick) },
    onLogEncounterClick: () -> Unit = {},
    onLogEncounterForContact: (String, String?) -> Unit = { _, _ -> },
    onAddContactClick: () -> Unit = {},
    terminology: RelationshipTerminology = RelationshipTerminology.DEFAULT
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Display feedback messages via Snackbar only when standalone (MainAppShell handles it otherwise)
    if (showBottomDock) {
        LaunchedEffect(uiState.userMessage) {
            uiState.userMessage?.let { message ->
                snackbarHostState.showSnackbar(message)
                onEvent(HomeUiEvent.OnDismissUserMessage)
            }
        }
    }

    val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
        Box(
            modifier = contentModifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Vertically Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() / 2,
                        bottom = 120.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // 1. Top Bar & Headline matching People and Timeline screens
                HomeTopBar(
                    userName = uiState.userName,
                    onSettingsClick = onSettingsClick
                )

                // 2. "Upcoming" Feed (Horizontal Alert Carousel)
                UpcomingFeedSection(
                    alerts = uiState.upcomingAlerts,
                    onReminderDone = remember(onEvent) { { onEvent(HomeUiEvent.OnReminderDone(it)) } },
                    onReminderSnooze = remember(onEvent) { { onEvent(HomeUiEvent.OnReminderSnooze(it)) } }
                )

                // 3. Recent Encounters List
                RecentEncountersSection(
                    contacts = uiState.recentContacts,
                    onContactClick = onContactClick,
                    onNavigateToPeople = onNavigateToPeople,
                    hasContacts = uiState.recentContacts.isNotEmpty(),
                    onLogEncounterClick = onLogEncounterClick,
                    onAddContactClick = onAddContactClick,
                    todayBirthdays = uiState.todayBirthdays,
                    onLogBirthdayEncounter = { contactId, contactName ->
                        onLogEncounterForContact(contactId, "Celebrated $contactName's Birthday")
                    },
                    terminology = terminology
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showBottomDock) {
                // Scrim to dismiss Speed Dial when tapping outside
                AnimatedVisibility(
                    visible = uiState.isSpeedDialOpen,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onEvent(HomeUiEvent.OnCloseSpeedDial)
                            }
                    )
                }

                // Floating Bottom Navigation Dock & Companion Quick-Add FAB
                HomeFloatingBottomDock(
                    selectedTab = uiState.selectedNavTab,
                    onTabSelect = { tab ->
                        if (uiState.isSpeedDialOpen) {
                            onEvent(HomeUiEvent.OnCloseSpeedDial)
                        }
                        onEvent(HomeUiEvent.OnNavTabSelect(tab))
                    },
                    isSpeedDialOpen = uiState.isSpeedDialOpen,
                    onToggleSpeedDial = { onEvent(HomeUiEvent.OnToggleSpeedDial) },
                    onSpeedDialAction = { onEvent(HomeUiEvent.OnSpeedDialAction(it)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                )
            }
        }
    }

    if (showBottomDock) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                Box {
                    SnackbarHost(hostState = snackbarHostState)
                }
            }
        ) { innerPadding ->
            mainContent(Modifier.padding(innerPadding))
        }
    } else {
        mainContent(modifier)
    }
}

// -------------------------------------------------------------
// Previews
// -------------------------------------------------------------

@Preview(name = "Light Mode", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenLightPreview() {
    val sampleState = HomeViewModel.createInitialState()
    HelloTheme(darkTheme = false) {
        HomeScreen(
            uiState = sampleState,
            onEvent = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenDarkPreview() {
    val sampleState = HomeViewModel.createInitialState()
    HelloTheme(darkTheme = true) {
        HomeScreen(
            uiState = sampleState,
            onEvent = {}
        )
    }
}

@Preview(name = "Empty Upcoming State", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenEmptyUpcomingPreview() {
    val sampleState = HomeViewModel.createInitialState().copy(
        upcomingAlerts = emptyList()
    )
    HelloTheme(darkTheme = false) {
        HomeScreen(
            uiState = sampleState,
            onEvent = {}
        )
    }
}
