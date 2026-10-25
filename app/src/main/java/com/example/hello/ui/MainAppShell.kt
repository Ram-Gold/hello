package com.example.hello.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.ui.contacts.ContactsScreen
import com.example.hello.ui.contacts.ContactsUiEvent
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.detail.PersonDetailScreen
import com.example.hello.ui.detail.PersonDetailUiEvent
import com.example.hello.ui.detail.PersonDetailViewModel
import com.example.hello.ui.edit.PersonEditScreen
import com.example.hello.ui.edit.PersonEditViewModel
import com.example.hello.ui.home.HomeScreen
import com.example.hello.ui.home.HomeUiEvent
import com.example.hello.ui.home.HomeViewModel
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.navigation.PersonDetailRoute
import com.example.hello.ui.navigation.PersonEditRoute
import com.example.hello.ui.quickaction.QuickActionSheetHost
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionViewModel
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens
import kotlinx.coroutines.launch

@Composable
fun MainAppShell(
    homeViewModel: HomeViewModel = remember { HomeViewModel() },
    contactsViewModel: ContactsViewModel = remember { ContactsViewModel() },
    quickActionViewModel: QuickActionViewModel = remember { QuickActionViewModel() },
    modifier: Modifier = Modifier
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    val quickActionUiState by quickActionViewModel.uiState.collectAsState()
    val repoContacts by quickActionViewModel.repository.contacts.collectAsState()
    val repoRecentContacts by quickActionViewModel.repository.recentContacts.collectAsState()
    val repoUpcomingAlerts by quickActionViewModel.repository.upcomingAlerts.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    // Single source of truth: PagerState is the authority for active tab when on Root screen
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val activeTab by remember {
        derivedStateOf {
            if (pagerState.targetPage == 0) HomeNavTab.HOME else HomeNavTab.PEOPLE
        }
    }

    // Predictive / System Back on People (page 1) smoothly slides back to Home (page 0)
    BackHandler(enabled = pagerState.currentPage == 1 || pagerState.targetPage == 1) {
        scope.launch {
            pagerState.animateScrollToPage(
                page = 0,
                animationSpec = MotionTokens.tabTransitionTween()
            )
        }
    }

    // Synchronize shared ContactRepository streams with Home and People ViewModels
    LaunchedEffect(repoRecentContacts, repoUpcomingAlerts) {
        homeViewModel.syncRepositoryData(
            recentContacts = repoRecentContacts,
            upcomingAlerts = repoUpcomingAlerts
        )
    }
    LaunchedEffect(repoContacts) {
        contactsViewModel.syncContacts(repoContacts)
    }

    // Surface feedback messages
    LaunchedEffect(contactsUiState.userMessage) {
        contactsUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            contactsViewModel.onEvent(ContactsUiEvent.OnDismissUserMessage)
        }
    }
    LaunchedEffect(homeUiState.userMessage) {
        homeUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            homeViewModel.onEvent(HomeUiEvent.OnDismissUserMessage)
        }
    }
    LaunchedEffect(quickActionUiState.userMessage) {
        quickActionUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            quickActionViewModel.onEvent(QuickActionUiEvent.OnDismissUserMessage)
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
            NavHost(
                navController = navController,
                startDestination = "root",
                modifier = Modifier.fillMaxSize()
            ) {
                composable(
                    route = "root",
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            initialOffsetX = { -it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeIn(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            targetOffsetX = { -it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeOut(animationSpec = MotionTokens.screenFadeSpec())
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // HorizontalPager provides fluid directional screen transitions driven by PagerState
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = false,
                            beyondViewportPageCount = 1,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { clip = true }
                        ) { page ->
                            when (page) {
                                0 -> HomeScreen(
                                    uiState = homeUiState,
                                    onEvent = homeViewModel::onEvent,
                                    showBottomDock = false,
                                    onNavigateToPeople = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                page = 1,
                                                animationSpec = MotionTokens.tabTransitionTween()
                                            )
                                        }
                                    },
                                    onContactClick = { contactId ->
                                        navController.navigate(PersonDetailRoute.createRoute(contactId))
                                    }
                                )
                                1 -> ContactsScreen(
                                    uiState = contactsUiState,
                                    onEvent = contactsViewModel::onEvent,
                                    onNavigateToHome = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                page = 0,
                                                animationSpec = MotionTokens.tabTransitionTween()
                                            )
                                        }
                                    },
                                    onContactClick = { contactId ->
                                        navController.navigate(PersonDetailRoute.createRoute(contactId))
                                    }
                                )
                            }
                        }

                        // Scrim to dismiss the 2-button Speed Dial column when tapping outside
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

                        // Shared Floating Bottom Navigation Dock & Companion Quick-Add [ + ] FAB
                        HomeFloatingBottomDock(
                            selectedTab = activeTab,
                            onTabSelect = { tab ->
                                val targetPage = when (tab) {
                                    HomeNavTab.HOME -> 0
                                    HomeNavTab.PEOPLE -> 1
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = targetPage,
                                        animationSpec = MotionTokens.tabTransitionTween()
                                    )
                                }
                            },
                            isSpeedDialOpen = homeUiState.isSpeedDialOpen,
                            onToggleSpeedDial = {
                                homeViewModel.onEvent(HomeUiEvent.OnToggleSpeedDial)
                            },
                            onSpeedDialAction = { action ->
                                homeViewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
                                when (action) {
                                    SpeedDialAction.NEW_CONTACT -> quickActionViewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
                                    SpeedDialAction.LOG_ENCOUNTER -> quickActionViewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                )
                        )
                    }
                }

                composable(
                    route = PersonDetailRoute.ROUTE_PATTERN,
                    arguments = listOf(navArgument(PersonDetailRoute.ARG_CONTACT_ID) { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            initialOffsetX = { it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeIn(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            targetOffsetX = { -it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeOut(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            initialOffsetX = { -it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeIn(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            targetOffsetX = { it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeOut(animationSpec = MotionTokens.screenFadeSpec())
                    }
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString(PersonDetailRoute.ARG_CONTACT_ID) ?: ""
                    val detailViewModel = remember(contactId) {
                        PersonDetailViewModel(contactId = contactId, repository = quickActionViewModel.repository)
                    }
                    val detailUiState by detailViewModel.uiState.collectAsState()

                    PersonDetailScreen(
                        uiState = detailUiState,
                        onEvent = { event ->
                            when (event) {
                                PersonDetailUiEvent.OnBackClick -> navController.popBackStack()
                                PersonDetailUiEvent.OnEditClick -> navController.navigate(PersonEditRoute.createRoute(contactId))
                                PersonDetailUiEvent.OnLogEncounterClick -> {
                                    val summary = quickActionViewModel.repository.contacts.value.firstOrNull { it.id == contactId }
                                        ?: detailUiState.contactSummary
                                        ?: ContactSummary(
                                            id = contactId,
                                            name = detailUiState.contact?.name ?: "Contact",
                                            contextSubtitle = "",
                                            circles = detailUiState.contact?.circles ?: emptyList(),
                                            avatarInitials = detailUiState.contact?.name?.take(2)?.uppercase() ?: "👋"
                                        )
                                    quickActionViewModel.onEvent(QuickActionUiEvent.OnOpenLogEncounterForContact(summary))
                                }
                                else -> detailViewModel.onEvent(event)
                            }
                        }
                    )
                }

                composable(
                    route = PersonEditRoute.ROUTE_PATTERN,
                    arguments = listOf(navArgument(PersonEditRoute.ARG_CONTACT_ID) { type = NavType.StringType }),
                    enterTransition = {
                        slideInVertically(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            initialOffsetY = { it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeIn(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    exitTransition = {
                        slideOutVertically(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            targetOffsetY = { -it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeOut(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    popEnterTransition = {
                        slideInVertically(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            initialOffsetY = { -it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeIn(animationSpec = MotionTokens.screenFadeSpec())
                    },
                    popExitTransition = {
                        slideOutVertically(
                            animationSpec = MotionTokens.screenSlideSpec(),
                            targetOffsetY = { it / MotionTokens.SCREEN_SLIDE_OFFSET_DIVISOR }
                        ) + fadeOut(animationSpec = MotionTokens.screenFadeSpec())
                    }
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString(PersonEditRoute.ARG_CONTACT_ID) ?: ""
                    val editViewModel = remember(contactId) {
                        PersonEditViewModel(contactId = contactId, repository = quickActionViewModel.repository)
                    }
                    val editUiState by editViewModel.uiState.collectAsState()

                    PersonEditScreen(
                        uiState = editUiState,
                        onEvent = editViewModel::onEvent,
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = {
                            scope.launch { snackbarHostState.showSnackbar("Contact updated ✨") }
                            navController.popBackStack()
                        },
                        onLifecycleSuccess = {
                            navController.popBackStack("root", inclusive = false)
                        }
                    )
                }
            }

            // Quick Action Sheet Host (Flow A: Add Contact card flow, Flow B: Met Again encounter sheet)
            QuickActionSheetHost(
                uiState = quickActionUiState,
                onEvent = quickActionViewModel::onEvent
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
