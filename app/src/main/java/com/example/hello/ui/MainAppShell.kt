package com.example.hello.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.ui.contacts.ContactsScreen
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.home.HomeScreen
import com.example.hello.ui.home.HomeUiEvent
import com.example.hello.ui.home.HomeViewModel
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.navigation.HomeRoute
import com.example.hello.ui.navigation.PeopleRoute
import com.example.hello.ui.navigation.navigateToRootTab
import com.example.hello.ui.navigation.resolveRouteSlideDirection
import com.example.hello.ui.navigation.sharedAxisEnterTransition
import com.example.hello.ui.navigation.sharedAxisExitTransition
import com.example.hello.ui.theme.HelloTheme

@Composable
fun MainAppShell(
    homeViewModel: HomeViewModel = remember { HomeViewModel() },
    contactsViewModel: ContactsViewModel = remember { ContactsViewModel() },
    modifier: Modifier = Modifier
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: HomeRoute.ROUTE_NAME
    val activeTab = if (currentRoute == PeopleRoute.ROUTE_NAME) HomeNavTab.PEOPLE else HomeNavTab.HOME
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface feedback messages
    LaunchedEffect(contactsUiState.userMessage) {
        contactsUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            contactsViewModel.onEvent(com.example.hello.ui.contacts.ContactsUiEvent.OnDismissUserMessage)
        }
    }
    LaunchedEffect(homeUiState.userMessage) {
        homeUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            homeViewModel.onEvent(HomeUiEvent.OnDismissUserMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Single NavHost with route-aware shared-axis horizontal transitions & hardware clipping
            NavHost(
                navController = navController,
                startDestination = HomeRoute.ROUTE_NAME,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = true }
            ) {
                composable(
                    route = HomeRoute.ROUTE_NAME,
                    enterTransition = {
                        val dir = resolveRouteSlideDirection(initialState.destination.route, targetState.destination.route)
                        sharedAxisEnterTransition(dir)
                    },
                    exitTransition = {
                        val dir = resolveRouteSlideDirection(initialState.destination.route, targetState.destination.route)
                        sharedAxisExitTransition(dir)
                    }
                ) {
                    HomeScreen(
                        uiState = homeUiState,
                        onEvent = homeViewModel::onEvent,
                        showBottomDock = false,
                        onNavigateToPeople = {
                            navController.navigateToRootTab(PeopleRoute.ROUTE_NAME)
                        }
                    )
                }
                composable(
                    route = PeopleRoute.ROUTE_NAME,
                    enterTransition = {
                        val dir = resolveRouteSlideDirection(initialState.destination.route, targetState.destination.route)
                        sharedAxisEnterTransition(dir)
                    },
                    exitTransition = {
                        val dir = resolveRouteSlideDirection(initialState.destination.route, targetState.destination.route)
                        sharedAxisExitTransition(dir)
                    }
                ) {
                    ContactsScreen(
                        uiState = contactsUiState,
                        onEvent = contactsViewModel::onEvent,
                        onNavigateToHome = {
                            navController.navigateToRootTab(HomeRoute.ROUTE_NAME)
                        }
                    )
                }
            }

            // Scrim to dismiss Speed Dial when tapping outside
            AnimatedVisibility(
                visible = homeUiState.isSpeedDialOpen,
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
                            homeViewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
                        }
                )
            }

            // Shared Floating Bottom Navigation Dock & Companion Quick-Add FAB
            HomeFloatingBottomDock(
                selectedTab = activeTab,
                onTabSelect = { tab ->
                    val target = when (tab) {
                        HomeNavTab.HOME -> HomeRoute.ROUTE_NAME
                        HomeNavTab.PEOPLE -> PeopleRoute.ROUTE_NAME
                    }
                    navController.navigateToRootTab(target)
                },
                isSpeedDialOpen = homeUiState.isSpeedDialOpen,
                onToggleSpeedDial = { homeViewModel.onEvent(HomeUiEvent.OnToggleSpeedDial) },
                onSpeedDialAction = { action ->
                    homeViewModel.onEvent(HomeUiEvent.OnSpeedDialAction(action))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
            )
        }
    }
}

@Preview(name = "App Shell Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MainAppShellLightPreview() {
    HelloTheme(darkTheme = false) {
        MainAppShell()
    }
}

@Preview(name = "App Shell Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 844)
@Composable
fun MainAppShellDarkPreview() {
    HelloTheme(darkTheme = true) {
        MainAppShell()
    }
}
