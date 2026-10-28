package com.example.hello.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.work.WorkManager
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.ui.components.ScreenDepthWrapper
import com.example.hello.ui.contacts.ContactsScreen
import com.example.hello.ui.contacts.ContactsUiEvent
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.detail.PersonDetailScreen
import com.example.hello.ui.detail.PersonDetailViewModel
import com.example.hello.ui.edit.PersonEditScreen
import com.example.hello.ui.edit.PersonEditViewModel
import com.example.hello.ui.home.HomeScreen
import com.example.hello.ui.home.HomeUiEvent
import com.example.hello.ui.home.HomeViewModel
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.navigation.HomeRoute
import com.example.hello.ui.navigation.NavigationTransitionConfig
import com.example.hello.ui.navigation.OnboardingRoute
import com.example.hello.ui.navigation.PeopleRoute
import com.example.hello.ui.navigation.PersonDetailMotionConfig
import com.example.hello.ui.navigation.PersonDetailRoute
import com.example.hello.ui.navigation.PersonEditRoute
import com.example.hello.ui.navigation.SettingsRoute
import com.example.hello.ui.navigation.aospSharedAxisEnter
import com.example.hello.ui.navigation.aospSharedAxisExit
import com.example.hello.ui.navigation.aospSharedAxisPopEnter
import com.example.hello.ui.navigation.aospSharedAxisPopExit
import com.example.hello.ui.navigation.enterTransition
import com.example.hello.ui.navigation.exitTransition
import com.example.hello.ui.navigation.isMainRootRoute
import com.example.hello.ui.navigation.mainRootEnterTransition
import com.example.hello.ui.navigation.mainRootExitTransition
import com.example.hello.ui.navigation.navigateSafely
import com.example.hello.ui.navigation.navigateToTopLevelSafely
import com.example.hello.ui.navigation.popEnterTransition
import com.example.hello.ui.navigation.popExitTransition
import com.example.hello.ui.navigation.shouldHideFloatingDock
import com.example.hello.ui.onboarding.OnboardingScreen
import com.example.hello.ui.onboarding.OnboardingViewModel
import com.example.hello.ui.quickaction.QuickActionSheetHost
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionViewModel
import com.example.hello.ui.settings.SettingsScreen
import com.example.hello.ui.settings.SettingsViewModel
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens
import kotlinx.coroutines.launch

@Composable
fun MainAppShell(
    preferencesRepository: UserPreferencesRepository? = null,
    contactRepository: ContactRepository? = null,
    initialHomeViewModel: HomeViewModel? = null,
    initialContactsViewModel: ContactsViewModel? = null,
    initialQuickActionViewModel: QuickActionViewModel? = null,
    initialContactId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedPreferencesRepository = remember(preferencesRepository, context) {
        preferencesRepository ?: DataStoreUserPreferencesRepository(context.helloDataStore)
    }
    val repository = remember(contactRepository) {
        contactRepository ?: ContactRepository(
            database = HelloDatabase.getInstance(context),
            alarmScheduler = FollowUpAlarmScheduler(context)
        )
    }
    val quickActionViewModel = remember(initialQuickActionViewModel, repository) {
        initialQuickActionViewModel ?: QuickActionViewModel(
            repository = repository,
            locationService = LocationService(context)
        )
    }
    val homeViewModel = remember(initialHomeViewModel, repository) {
        initialHomeViewModel ?: HomeViewModel(repository = repository)
    }
    val contactsViewModel = remember(initialContactsViewModel, repository) {
        initialContactsViewModel ?: ContactsViewModel(repository = repository)
    }

    val userPreferences by resolvedPreferencesRepository.userPreferencesFlow.collectAsState(initial = null)

    // ADR 0005: Zero-flicker cold start gating (<50ms). Render solid warm surface while DataStore initial == null
    if (userPreferences == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val preferredTerminology = userPreferences?.preferredTerminology ?: RelationshipTerminology.DEFAULT
    val hasCompletedOnboarding = userPreferences?.hasCompletedOnboarding == true
    val terminologyState = rememberUpdatedState(preferredTerminology)

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Deep link from FollowUp notification content tap
    LaunchedEffect(initialContactId, hasCompletedOnboarding) {
        if (!initialContactId.isNullOrBlank() && hasCompletedOnboarding) {
            navController.navigateSafely(PersonDetailRoute.createRoute(initialContactId))
        }
    }

    // Active tab derived directly from NavController destination (ADR 0003)
    val activeTab by remember(currentRoute) {
        derivedStateOf {
            if (currentRoute == PeopleRoute.ROUTE_NAME) HomeNavTab.PEOPLE else HomeNavTab.HOME
        }
    }

    // Hide floating dock on non-root destinations using pure GPU translation (matches PixelPlayer)
    val shouldHideDock by remember(currentRoute) {
        derivedStateOf {
            shouldHideFloatingDock(currentRoute)
        }
    }
    val dockVisibilityProgressState = animateFloatAsState(
        targetValue = if (shouldHideDock) 0f else 1f,
        animationSpec = tween(
            durationMillis = MotionTokens.NAV_BAR_HIDE_DURATION_MS,
            easing = MotionTokens.NavBarHideEasing
        ),
        label = "DockVisibilityProgress"
    )
    val shouldRenderDock by remember(shouldHideDock) {
        derivedStateOf {
            !shouldHideDock || dockVisibilityProgressState.value > 0.01f
        }
    }
    var dockHeightPx by remember { mutableIntStateOf(0) }

    // Synchronize shared ContactRepository streams when custom ViewModels are injected in tests
    LaunchedEffect(repository, homeViewModel) {
        if (homeViewModel.repository !== repository) {
            launch {
                repository.recentContacts.collect { recents ->
                    homeViewModel.syncRepositoryData(
                        recentContacts = recents,
                        upcomingAlerts = repository.upcomingAlerts.value
                    )
                }
            }
            launch {
                repository.upcomingAlerts.collect { alerts ->
                    homeViewModel.syncRepositoryData(
                        recentContacts = repository.recentContacts.value,
                        upcomingAlerts = alerts
                    )
                }
            }
        }
    }
    LaunchedEffect(repository, contactsViewModel) {
        if (contactsViewModel.repository !== repository) {
            repository.contacts.collect { contacts ->
                contactsViewModel.syncContacts(contacts)
            }
        }
    }

    SnackbarFeedbackCollector(
        homeViewModel = homeViewModel,
        contactsViewModel = contactsViewModel,
        quickActionViewModel = quickActionViewModel,
        snackbarHostState = snackbarHostState
    )

    val startDestination = if (hasCompletedOnboarding) HomeRoute.ROUTE_NAME else OnboardingRoute.ROUTE_NAME

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
            if (hasCompletedOnboarding || (currentRoute != null && currentRoute != OnboardingRoute.ROUTE_NAME)) {
                WarmRootTabsHost(
                    navController = navController,
                    currentRoute = currentRoute,
                    activeTab = activeTab,
                    homeViewModel = homeViewModel,
                    contactsViewModel = contactsViewModel,
                    quickActionViewModel = quickActionViewModel,
                    terminologyState = terminologyState
                )
            }

            MainAppNavHost(
                navController = navController,
                startDestination = startDestination,
                preferencesRepository = resolvedPreferencesRepository,
                quickActionViewModel = quickActionViewModel,
                snackbarHostState = snackbarHostState
            )

            // Scrim & Floating Bottom Navigation Dock scoped to prevent NavHost recomposition
            FloatingDockOverlay(
                navController = navController,
                currentRoute = currentRoute,
                activeTab = activeTab,
                shouldRenderDock = shouldRenderDock,
                dockVisibilityProgressState = dockVisibilityProgressState,
                dockHeightPx = dockHeightPx,
                onDockHeightChanged = { dockHeightPx = it },
                homeViewModel = homeViewModel,
                quickActionViewModel = quickActionViewModel,
                preferredTerminology = preferredTerminology,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Quick Action Sheet Host (Flow A: Add Contact card flow, Flow B: Met Again encounter sheet)
            QuickActionSheetHostContainer(quickActionViewModel = quickActionViewModel)
        }
    }
}

/**
 * Keeps both [HomeScreen] and [ContactsScreen] permanently composed and laid out in memory
 * so switching between `HomeRoute` and `PeopleRoute` executes with zero composition or layout
 * passes on Frame 1. Drives the PixelPlayer 380ms 50%-width slide + 190ms half-duration fade
 * with `CubicBezierEasing(0.2f, 0f, 0f, 1f)` purely in the GPU draw phase (`Modifier.graphicsLayer`).
 */
@Composable
private fun WarmRootTabsHost(
    navController: NavHostController,
    currentRoute: String?,
    activeTab: HomeNavTab,
    homeViewModel: HomeViewModel,
    contactsViewModel: ContactsViewModel,
    quickActionViewModel: QuickActionViewModel,
    terminologyState: State<RelationshipTerminology>
) {
    val targetProgress = if (activeTab == HomeNavTab.PEOPLE) 1f else 0f
    val slideFractionState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = MotionTokens.tabTransitionTween(),
        label = "RootTabSlideFraction"
    )
    val fadeFractionState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = MotionTokens.screenFadeSpec(),
        label = "RootTabFadeFraction"
    )

    val isDeeperRoute = currentRoute != null && !isMainRootRoute(currentRoute)
    val depthFractionState = animateFloatAsState(
        targetValue = if (isDeeperRoute) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.AOSP_TRANSITION_DURATION,
            easing = MotionTokens.M3EmphasizedEasing
        ),
        label = "RootDepthFraction"
    )

    val onNavigateToPeople = remember(navController) {
        {
            navController.navigateToTopLevelSafely(PeopleRoute.ROUTE_NAME)
            Unit
        }
    }
    val onNavigateToHome = remember(navController) {
        {
            navController.navigateToTopLevelSafely(HomeRoute.ROUTE_NAME)
            Unit
        }
    }
    val onContactClick = remember(navController) {
        { contactId: String ->
            navController.navigateSafely(PersonDetailRoute.createRoute(contactId))
            Unit
        }
    }
    val onSettingsClick = remember(navController) {
        {
            navController.navigateSafely(SettingsRoute.ROUTE_NAME)
            Unit
        }
    }
    val onLogEncounterClick = remember(quickActionViewModel) {
        { quickActionViewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter) }
    }
    val onAddContactClick = remember(quickActionViewModel) {
        { quickActionViewModel.onEvent(QuickActionUiEvent.OnSelectAddContact) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val depth = depthFractionState.value
                if (depth >= 0.999f) {
                    translationX = -size.width * 2f
                    alpha = 0f
                } else {
                    translationX = -depth * (size.width / 3f)
                    val scale = 1f - (0.08f * depth)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f
                }
            }
    ) {
        // Slot 0: Home Screen (warm in composition, GPU-translated during transitions)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (activeTab == HomeNavTab.HOME) 1f else 0f)
                .graphicsLayer {
                    val slide = slideFractionState.value
                    val fade = (1f - fadeFractionState.value).coerceIn(0f, 1f)
                    if (slide >= 0.999f && fade <= 0.001f) {
                        translationX = -size.width * 2f
                        alpha = 0f
                    } else {
                        translationX = -slide * (size.width * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION)
                        alpha = fade
                    }
                }
        ) {
            HomeTabDestination(
                homeViewModel = homeViewModel,
                onNavigateToPeople = onNavigateToPeople,
                onContactClick = onContactClick,
                onSettingsClick = onSettingsClick,
                onLogEncounterClick = onLogEncounterClick,
                onAddContactClick = onAddContactClick,
                terminology = terminologyState.value
            )
        }

        // Slot 1: People / Relationship Screen (warm in composition, GPU-translated during transitions)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (activeTab == HomeNavTab.PEOPLE) 1f else 0f)
                .graphicsLayer {
                    val slide = slideFractionState.value
                    val fade = fadeFractionState.value.coerceIn(0f, 1f)
                    if (slide <= 0.001f && fade <= 0.001f) {
                        translationX = size.width * 2f
                        alpha = 0f
                    } else {
                        translationX = (1f - slide) * (size.width * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION)
                        alpha = fade
                    }
                }
        ) {
            PeopleTabDestination(
                contactsViewModel = contactsViewModel,
                onNavigateToHome = onNavigateToHome,
                onContactClick = onContactClick,
                onAddContactClick = onAddContactClick,
                terminology = terminologyState.value
            )
        }
    }
}

@Composable
private fun HomeTabDestination(
    homeViewModel: HomeViewModel,
    onNavigateToPeople: () -> Unit,
    onContactClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogEncounterClick: () -> Unit,
    onAddContactClick: () -> Unit,
    terminology: RelationshipTerminology
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    ScreenDepthWrapper(isDepthVisible = false) {
        HomeScreen(
            uiState = homeUiState,
            onEvent = homeViewModel::onEvent,
            showBottomDock = false,
            onNavigateToPeople = onNavigateToPeople,
            onContactClick = onContactClick,
            onSettingsClick = onSettingsClick,
            onLogEncounterClick = onLogEncounterClick,
            onAddContactClick = onAddContactClick,
            terminology = terminology
        )
    }
}

@Composable
private fun PeopleTabDestination(
    contactsViewModel: ContactsViewModel,
    onNavigateToHome: () -> Unit,
    onContactClick: (String) -> Unit,
    onAddContactClick: () -> Unit,
    terminology: RelationshipTerminology
) {
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    ScreenDepthWrapper(isDepthVisible = false) {
        ContactsScreen(
            uiState = contactsUiState,
            onEvent = contactsViewModel::onEvent,
            onNavigateToHome = onNavigateToHome,
            onContactClick = onContactClick,
            onAddContactClick = onAddContactClick,
            terminology = terminology
        )
    }
}

@Composable
private fun MainAppNavHost(
    navController: NavHostController,
    startDestination: String,
    preferencesRepository: UserPreferencesRepository,
    quickActionViewModel: QuickActionViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val navGraph = remember(navController, startDestination) {
        navController.createGraph(startDestination = startDestination) {
            composable(
                route = OnboardingRoute.ROUTE_NAME,
                enterTransition = { fadeIn(animationSpec = MotionTokens.screenFadeSpec()) },
                exitTransition = { fadeOut(animationSpec = MotionTokens.screenFadeSpec()) }
            ) {
                val onboardingViewModel = remember { OnboardingViewModel(preferencesRepository) }
                val onboardingUiState by onboardingViewModel.uiState.collectAsState()

                OnboardingScreen(
                    uiState = onboardingUiState,
                    onEvent = onboardingViewModel::onEvent,
                    onFinish = {
                        navController.navigate(HomeRoute.ROUTE_NAME) {
                            popUpTo(OnboardingRoute.ROUTE_NAME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = HomeRoute.ROUTE_NAME,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                }
            ) {
                // Visual content is kept warm in WarmRootTabsHost for 0ms first-frame latency
            }

            composable(
                route = PeopleRoute.ROUTE_NAME,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = enterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = exitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = popExitTransition()
                    )
                }
            ) {
                // Visual content is kept warm in WarmRootTabsHost for 0ms first-frame latency
            }

            composable(
                route = SettingsRoute.ROUTE_NAME
            ) {
                ScreenDepthWrapper(isDepthVisible = false) {
                    val settingsViewModel = remember {
                        val wm = try { WorkManager.getInstance(context) } catch (_: Exception) { null }
                        SettingsViewModel(preferencesRepository, quickActionViewModel.repository, wm)
                    }
                    val settingsUiState by settingsViewModel.uiState.collectAsState()

                    SettingsScreen(
                        uiState = settingsUiState,
                        onEvent = settingsViewModel::onEvent,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = PersonDetailRoute.ROUTE_PATTERN,
                arguments = listOf(navArgument(PersonDetailRoute.ARG_CONTACT_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                ScreenDepthWrapper(isDepthVisible = false) {
                    val contactId = backStackEntry.arguments?.getString(PersonDetailRoute.ARG_CONTACT_ID) ?: ""
                    val detailViewModel = remember(contactId) {
                        PersonDetailViewModel(contactId = contactId, repository = quickActionViewModel.repository)
                    }
                    val detailUiState by detailViewModel.uiState.collectAsState()

                    PersonDetailScreen(
                        uiState = detailUiState,
                        onEvent = detailViewModel::onEvent,
                        onNavigateBack = { navController.popBackStack() },
                        onEditClick = { navController.navigateSafely(PersonEditRoute.createRoute(contactId)) },
                        onLogEncounterClick = {
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
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(PersonDetailMotionConfig.DETAIL_Z_INDEX)
                    )
                }
            }

            composable(
                route = PersonEditRoute.ROUTE_PATTERN,
                arguments = listOf(navArgument(PersonEditRoute.ARG_CONTACT_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                ScreenDepthWrapper(isDepthVisible = false) {
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
                            if (!navController.popBackStack(PersonDetailRoute.ROUTE_PATTERN, inclusive = true)) {
                                navController.popBackStack(HomeRoute.ROUTE_NAME, inclusive = false)
                            }
                        }
                    )
                }
            }
        }
    }

    NavHost(
        navController = navController,
        graph = navGraph,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { aospSharedAxisEnter() },
        exitTransition = { aospSharedAxisExit() },
        popEnterTransition = { aospSharedAxisPopEnter() },
        popExitTransition = { aospSharedAxisPopExit() }
    )
}

@Composable
private fun FloatingDockOverlay(
    navController: NavHostController,
    currentRoute: String?,
    activeTab: HomeNavTab,
    shouldRenderDock: Boolean,
    dockVisibilityProgressState: State<Float>,
    dockHeightPx: Int,
    onDockHeightChanged: (Int) -> Unit,
    homeViewModel: HomeViewModel,
    quickActionViewModel: QuickActionViewModel,
    preferredTerminology: RelationshipTerminology,
    modifier: Modifier = Modifier
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val isSpeedDialOpen = homeUiState.isSpeedDialOpen

    // Scrim to dismiss the 2-button Speed Dial column when tapping outside on root tabs
    AnimatedVisibility(
        visible = isSpeedDialOpen && isMainRootRoute(currentRoute),
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
                    homeViewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
                }
        )
    }

    if (shouldRenderDock) {
        HomeFloatingBottomDock(
            selectedTab = activeTab,
            onTabSelect = { tab ->
                if (isSpeedDialOpen) {
                    homeViewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
                }
                homeViewModel.onEvent(HomeUiEvent.OnCloseDrawer)
                val targetRoute = when (tab) {
                    HomeNavTab.HOME -> HomeRoute.ROUTE_NAME
                    HomeNavTab.PEOPLE -> PeopleRoute.ROUTE_NAME
                }
                navController.navigateToTopLevelSafely(targetRoute)
            },
            isSpeedDialOpen = isSpeedDialOpen,
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
            peopleTabLabel = preferredTerminology.pluralTitle,
            addContactLabel = preferredTerminology.addActionTitle(),
            modifier = modifier
                .onSizeChanged { onDockHeightChanged(it.height) }
                .graphicsLayer {
                    val progress = dockVisibilityProgressState.value
                    val hideFraction = (1f - progress).coerceIn(0f, 1f)
                    translationY = dockHeightPx * hideFraction
                    alpha = progress
                }
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
        )
    }
}

@Composable
private fun QuickActionSheetHostContainer(
    quickActionViewModel: QuickActionViewModel
) {
    val quickActionUiState by quickActionViewModel.uiState.collectAsState()
    QuickActionSheetHost(
        uiState = quickActionUiState,
        onEvent = quickActionViewModel::onEvent
    )
}

@Composable
private fun SnackbarFeedbackCollector(
    homeViewModel: HomeViewModel,
    contactsViewModel: ContactsViewModel,
    quickActionViewModel: QuickActionViewModel,
    snackbarHostState: SnackbarHostState
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    val quickActionUiState by quickActionViewModel.uiState.collectAsState()

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
}

@Preview(name = "App Shell Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun MainAppShellLightPreview() {
    HelloTheme(darkTheme = false) {
        MainAppShell(
            preferencesRepository = remember {
                object : UserPreferencesRepository {
                    override val userPreferencesFlow = kotlinx.coroutines.flow.flowOf(
                        com.example.hello.data.preferences.UserPreferences(
                            hasCompletedOnboarding = true,
                            preferredTerminology = RelationshipTerminology.PEOPLE
                        )
                    )
                    override suspend fun setOnboardingCompleted(completed: Boolean) {}
                    override suspend fun setPreferredTerminology(terminology: RelationshipTerminology) {}
                }
            }
        )
    }
}

@Preview(name = "App Shell Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 390, heightDp = 844)
@Composable
fun MainAppShellDarkPreview() {
    HelloTheme(darkTheme = true) {
        MainAppShell(
            preferencesRepository = remember {
                object : UserPreferencesRepository {
                    override val userPreferencesFlow = kotlinx.coroutines.flow.flowOf(
                        com.example.hello.data.preferences.UserPreferences(
                            hasCompletedOnboarding = true,
                            preferredTerminology = RelationshipTerminology.FRIENDS
                        )
                    )
                    override suspend fun setOnboardingCompleted(completed: Boolean) {}
                    override suspend fun setPreferredTerminology(terminology: RelationshipTerminology) {}
                }
            }
        )
    }
}
