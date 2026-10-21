package com.example.hello.ui.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.CardType
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.home.components.FullScreenCardDialog
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.home.components.HomeTopBar
import com.example.hello.ui.home.components.MyCardWidget
import com.example.hello.ui.home.components.RecentActionsAccordion
import com.example.hello.ui.home.components.RecentEncountersSection
import com.example.hello.ui.home.components.UpcomingFeedSection
import com.example.hello.ui.theme.HelloTheme

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    showBottomDock: Boolean = true,
    onNavigateToPeople: () -> Unit = { onEvent(HomeUiEvent.OnNavTabSelect(HomeNavTab.PEOPLE)) }
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val activeCard = uiState.myCards[uiState.activeCardType]
        ?: uiState.myCards.values.firstOrNull()

    // Display feedback messages via Snackbar
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onEvent(HomeUiEvent.OnDismissUserMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            Box {
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                // 1. Top Bar (Settings on Left & Drawer on Right)
                HomeTopBar(
                    onSettingsClick = { onEvent(HomeUiEvent.OnSettingsClick) },
                    onDrawerClick = { onEvent(HomeUiEvent.OnOpenDrawer) }
                )

                // 2. Greeting Column (under the top bar)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Hello, ${uiState.userName}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "👋",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "Cultivate your circle today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 3. The "My Card" Widget (Hero Component)
                activeCard?.let { card ->
                    MyCardWidget(
                        cardData = card,
                        activeCardType = uiState.activeCardType,
                        onCardTypeSelect = { onEvent(HomeUiEvent.OnCardTypeSelect(it)) },
                        onExpandClick = { onEvent(HomeUiEvent.OnExpandQr(true)) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // 3. "Upcoming" Feed (Horizontal Alert Carousel)
                UpcomingFeedSection(
                    alerts = uiState.upcomingAlerts,
                    onReminderDone = { onEvent(HomeUiEvent.OnReminderDone(it)) },
                    onReminderSnooze = { onEvent(HomeUiEvent.OnReminderSnooze(it)) }
                )

                // 4. Stacked Accordion Action Buttons ("Add Contact" / "Update Contact")
                RecentActionsAccordion(
                    expandedType = uiState.expandedAccordion,
                    onToggle = { onEvent(HomeUiEvent.OnToggleAccordion(it)) },
                    recentContacts = uiState.recentContacts,
                    onAddContactSubmit = { name, circles, notes ->
                        onEvent(HomeUiEvent.OnQuickAddContact(name, circles, notes))
                    },
                    onUpdateContactSubmit = { id, notes ->
                        onEvent(HomeUiEvent.OnQuickUpdateContact(id, notes))
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // 5. Recent Encounters List
                RecentEncountersSection(
                    contacts = uiState.recentContacts,
                    onContactClick = { onEvent(HomeUiEvent.OnContactClick(it)) },
                    onNavigateToPeople = onNavigateToPeople
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showBottomDock) {
                // Scrim to dismiss Speed Dial when tapping outside
                AnimatedVisibility(
                    visible = uiState.isSpeedDialOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onEvent(HomeUiEvent.OnCloseSpeedDial)
                            }
                    )
                }

                // 6. Floating Bottom Navigation Dock & Companion Quick-Add FAB
                HomeFloatingBottomDock(
                    selectedTab = uiState.selectedNavTab,
                    onTabSelect = { onEvent(HomeUiEvent.OnNavTabSelect(it)) },
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

            // Full-Screen QR Dialog Preview
            if (uiState.isQrExpanded && activeCard != null) {
                FullScreenCardDialog(
                    cardData = activeCard,
                    onDismiss = { onEvent(HomeUiEvent.OnExpandQr(false)) }
                )
            }
        }
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
