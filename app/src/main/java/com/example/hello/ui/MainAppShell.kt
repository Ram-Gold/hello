package com.example.hello.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
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
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.example.hello.ui.components.HelloToastDefaults
import com.example.hello.ui.components.HelloToastHost
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.components.TOAST_DURATION_ACTIONABLE_MS
import com.example.hello.ui.components.TOAST_DURATION_INFORMATIONAL_MS
import com.example.hello.ui.components.showHelloToast
import com.example.hello.ui.haptics.HelloHaptics
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.haptics.rememberHelloHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.platform.avatar.AndroidNotionistAvatarFetcher
import com.example.hello.platform.avatar.AvatarStorageManager
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.ui.avatar.AvatarPlaygroundScreen
import com.example.hello.ui.avatar.AvatarPlaygroundViewModel
import com.example.hello.ui.components.ScreenDepthWrapper
import com.example.hello.ui.contacts.ContactsScreen
import com.example.hello.ui.contacts.ContactsUiEvent
import com.example.hello.ui.contacts.ArchivedContactsScreen
import com.example.hello.ui.contacts.ContactsViewModel
import com.example.hello.ui.detail.PersonDetailScreen
import com.example.hello.ui.detail.PersonDetailViewModel
import com.example.hello.ui.edit.PersonEditScreen
import com.example.hello.ui.edit.PersonEditUiEvent
import com.example.hello.ui.edit.PersonEditViewModel
import com.example.hello.ui.home.HomeScreen
import com.example.hello.ui.home.HomeUiEvent
import com.example.hello.ui.home.HomeViewModel
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.navigation.ArchivedContactsRoute
import com.example.hello.ui.navigation.AvatarPlaygroundRoute
import com.example.hello.ui.navigation.HomeRoute
import com.example.hello.ui.navigation.NavigationTransitionConfig
import com.example.hello.ui.navigation.OnboardingRoute
import com.example.hello.data.preferences.UserPreferences
import com.example.hello.ui.navigation.PeopleRoute
import com.example.hello.ui.navigation.PersonDetailMotionConfig
import com.example.hello.ui.navigation.PersonDetailRoute
import com.example.hello.ui.navigation.PersonEditRoute
import com.example.hello.ui.navigation.SettingsAboutRoute
import com.example.hello.ui.navigation.SettingsLicensesRoute
import com.example.hello.ui.navigation.SettingsAppearanceRoute
import com.example.hello.ui.navigation.SettingsDataRoute
import com.example.hello.ui.navigation.SettingsFlowsRoute
import com.example.hello.ui.navigation.SettingsMotionConfig
import com.example.hello.ui.navigation.SettingsPreferencesRoute
import com.example.hello.ui.navigation.SettingsRoute
import com.example.hello.ui.navigation.SettingsTagsRoute
import com.example.hello.ui.navigation.SettingsTerminologyRoute
import com.example.hello.ui.navigation.TimelineRoute
import com.example.hello.ui.navigation.PredictiveBackPopHandoff
import com.example.hello.ui.navigation.ScrubbablePredictiveBackHandler
import com.example.hello.ui.navigation.aospSharedAxisEnter
import com.example.hello.ui.navigation.aospSharedAxisExit
import com.example.hello.ui.navigation.aospSharedAxisPopEnter
import com.example.hello.ui.navigation.aospSharedAxisPopExit
import com.example.hello.ui.navigation.enterTransition
import com.example.hello.ui.navigation.exitTransition
import com.example.hello.ui.navigation.floatingDockProfileMorphSpec
import com.example.hello.ui.navigation.floatingDockVisibilitySpec
import com.example.hello.ui.navigation.interpolateTabProgressForPredictiveBack
import com.example.hello.ui.navigation.isMainRootRoute
import com.example.hello.ui.navigation.isPersonDetailRoute
import com.example.hello.ui.navigation.mainRootEnterTransition
import com.example.hello.ui.navigation.mainRootExitTransition
import com.example.hello.ui.navigation.navigateSafely
import com.example.hello.ui.navigation.navigateToTopLevelSafely
import com.example.hello.ui.navigation.popEnterTransition
import com.example.hello.ui.navigation.popExitTransition
import com.example.hello.ui.navigation.predictiveDeeperRoutePopExit
import com.example.hello.ui.navigation.resolveActiveTransitionTabs
import com.example.hello.ui.navigation.resolveDepthFractionWithPredictiveBack
import com.example.hello.ui.navigation.resolveDockVisibilityWithPredictiveBack
import com.example.hello.ui.navigation.resolveProfileMorphProgressWithPredictiveBack
import com.example.hello.ui.navigation.shouldHideFloatingDock
import com.example.hello.ui.navigation.shouldMorphDockToProfilePill
import com.example.hello.ui.navigation.shouldUseRootBridgeForBack
import com.example.hello.ui.onboarding.OnboardingScreen
import com.example.hello.ui.onboarding.OnboardingViewModel
import com.example.hello.ui.quickaction.QuickActionSheetHost
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionViewModel
import com.example.hello.ui.settings.SettingsAboutScreen
import com.example.hello.ui.settings.SettingsLicensesScreen
import com.example.hello.ui.settings.SettingsAppearanceScreen
import com.example.hello.ui.settings.SettingsDataScreen
import com.example.hello.ui.settings.SettingsFlowsScreen
import com.example.hello.ui.settings.SettingsPreferencesScreen
import com.example.hello.ui.settings.SettingsScreen
import com.example.hello.ui.settings.SettingsTagsScreen
import com.example.hello.ui.settings.SettingsTerminologyScreen
import com.example.hello.ui.settings.SettingsViewModel
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens
import com.example.hello.ui.timeline.TimelineScreen
import com.example.hello.ui.timeline.TimelineUiEvent
import com.example.hello.ui.timeline.TimelineViewModel
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
    val quickActionViewModel = remember(initialQuickActionViewModel, repository, resolvedPreferencesRepository) {
        initialQuickActionViewModel ?: QuickActionViewModel(
            repository = repository,
            locationService = LocationService(context),
            preferencesRepository = resolvedPreferencesRepository
        )
    }
    val homeViewModel = remember(initialHomeViewModel, repository, resolvedPreferencesRepository) {
        initialHomeViewModel ?: HomeViewModel(
            repository = repository,
            preferencesRepository = resolvedPreferencesRepository
        )
    }
    val timelineViewModel = remember(repository) {
        TimelineViewModel(repository = repository)
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

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val isReducedMotion = userPreferences?.reduceAnimations == true

    var rootTabBackProgress by remember { mutableFloatStateOf(0f) }
    var deeperRouteBackProgress by remember { mutableFloatStateOf(0f) }
    var selectionBackProgress by remember { mutableFloatStateOf(0f) }
    var isDetailSelectionMode by remember { mutableStateOf(false) }
    var detailSelectionBackProgress by remember { mutableFloatStateOf(0f) }
    val popHandoff = remember { PredictiveBackPopHandoff() }
    val useRootBridgeForCurrentRoute = shouldUseRootBridgeForBack(
        currentRoute = currentRoute,
        previousRoute = previousRoute
    )

    val activeDetailContactId = if (isPersonDetailRoute(currentRoute)) {
        navBackStackEntry?.arguments?.getString(PersonDetailRoute.ARG_CONTACT_ID)
    } else null

    LaunchedEffect(currentRoute) {
        rootTabBackProgress = 0f
        selectionBackProgress = 0f
        if (!isPersonDetailRoute(currentRoute)) {
            isDetailSelectionMode = false
            detailSelectionBackProgress = 0f
        }
        if (!popHandoff.hasActiveCommit()) {
            deeperRouteBackProgress = 0f
        }
    }

    // Deep link from FollowUp notification content tap
    LaunchedEffect(initialContactId, hasCompletedOnboarding) {
        if (!initialContactId.isNullOrBlank() && hasCompletedOnboarding) {
            navController.navigateSafely(PersonDetailRoute.createRoute(initialContactId))
        }
    }

    // Active tab derived directly from NavController destination, retaining last root tab on deeper routes
    var selectedRootTab by rememberSaveable { mutableStateOf(HomeNavTab.HOME) }
    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            HomeRoute.ROUTE_NAME -> {
                selectedRootTab = HomeNavTab.HOME
                contactsViewModel.onEvent(ContactsUiEvent.OnTabDeparture)
            }
            PeopleRoute.ROUTE_NAME -> {
                selectedRootTab = HomeNavTab.PEOPLE
                timelineViewModel.onEvent(TimelineUiEvent.OnClearSelection)
            }
            TimelineRoute.ROUTE_NAME -> {
                selectedRootTab = HomeNavTab.TIMELINE
                contactsViewModel.onEvent(ContactsUiEvent.OnTabDeparture)
            }
            else -> {
                homeViewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
                contactsViewModel.onEvent(ContactsUiEvent.OnTabDeparture)
                timelineViewModel.onEvent(TimelineUiEvent.OnClearSelection)
            }
        }
    }
    val activeTab = selectedRootTab

    val isContactsSelectionMode by remember(contactsViewModel) {
        contactsViewModel.uiState.map { it.isSelectionMode }.distinctUntilChanged()
    }.collectAsState(initial = false)

    val isTimelineSelectionMode by remember(timelineViewModel) {
        timelineViewModel.uiState.map { it.isSelectionMode }.distinctUntilChanged()
    }.collectAsState(initial = false)

    // Hide floating dock vertically on non-root/non-detail destinations or when the active surface is in selection mode.
    // On PersonDetailRoute (outside Timeline Selection Mode), the dock stays vertically stationary and morphs in place
    // into the Profile Encounter Pill ("+ Catching up?") per ADR-0034.
    val shouldHideDock by remember(
        currentRoute,
        activeTab,
        isContactsSelectionMode,
        isTimelineSelectionMode,
        isDetailSelectionMode
    ) {
        derivedStateOf {
            shouldHideFloatingDock(
                currentRoute = currentRoute,
                isContactsSelectionMode = isContactsSelectionMode,
                isTimelineSelectionMode = isTimelineSelectionMode,
                activeTab = activeTab,
                isDetailSelectionMode = isDetailSelectionMode
            )
        }
    }
    val isProfileDetailRoute = isPersonDetailRoute(currentRoute)
    val targetDockVisibility = if (shouldHideDock) 0f else 1f
    val targetProfileMorph = if (shouldMorphDockToProfilePill(currentRoute)) 1f else 0f

    val dockVisibilityAnimatable = remember { Animatable(targetDockVisibility) }
    val profileMorphAnimatable = remember { Animatable(targetProfileMorph) }
    var isProfileMorphReversing by remember { mutableStateOf(false) }

    val effectiveSelectionBackProgress = maxOf(selectionBackProgress, detailSelectionBackProgress).coerceIn(0f, 1f)
    val isProfileToRootBackScrub =
        isProfileDetailRoute &&
            !isDetailSelectionMode &&
            useRootBridgeForCurrentRoute &&
            deeperRouteBackProgress > 0f &&
            !isReducedMotion
    val activeDockScrub = if (isProfileDetailRoute && !isDetailSelectionMode) {
        0f
    } else {
        maxOf(deeperRouteBackProgress, effectiveSelectionBackProgress).coerceIn(0f, 1f)
    }

    LaunchedEffect(
        activeDockScrub,
        deeperRouteBackProgress,
        effectiveSelectionBackProgress,
        targetDockVisibility,
        isReducedMotion,
        isProfileDetailRoute,
        isDetailSelectionMode
    ) {
        if (activeDockScrub > 0f && !isReducedMotion) {
            val scrubDeeper = if (isProfileDetailRoute) 0f else deeperRouteBackProgress
            dockVisibilityAnimatable.snapTo(
                resolveDockVisibilityWithPredictiveBack(
                    animatedDockVisibility = targetDockVisibility,
                    deeperBackProgress = scrubDeeper,
                    selectionBackProgress = effectiveSelectionBackProgress
                )
            )
        } else if (dockVisibilityAnimatable.value != targetDockVisibility || dockVisibilityAnimatable.targetValue != targetDockVisibility) {
            if (isReducedMotion) {
                dockVisibilityAnimatable.snapTo(targetDockVisibility)
            } else {
                dockVisibilityAnimatable.animateTo(
                    targetValue = targetDockVisibility,
                    animationSpec = floatingDockVisibilitySpec(false)
                )
            }
        }
    }

    LaunchedEffect(
        isProfileToRootBackScrub,
        isProfileDetailRoute,
        isDetailSelectionMode,
        deeperRouteBackProgress,
        targetProfileMorph,
        isReducedMotion
    ) {
        if (isProfileToRootBackScrub) {
            isProfileMorphReversing = true
            profileMorphAnimatable.snapTo(
                resolveProfileMorphProgressWithPredictiveBack(
                    isPersonDetailRoute = true,
                    isDetailSelectionMode = isDetailSelectionMode,
                    targetMorphProgress = targetProfileMorph,
                    deeperBackProgress = deeperRouteBackProgress
                )
            )
        } else if (profileMorphAnimatable.value != targetProfileMorph || profileMorphAnimatable.targetValue != targetProfileMorph) {
            isProfileMorphReversing = targetProfileMorph < profileMorphAnimatable.value
            if (isReducedMotion || dockVisibilityAnimatable.value <= 0.01f) {
                profileMorphAnimatable.snapTo(targetProfileMorph)
            } else {
                val remaining = kotlin.math.abs(targetProfileMorph - profileMorphAnimatable.value)
                profileMorphAnimatable.animateTo(
                    targetValue = targetProfileMorph,
                    animationSpec = floatingDockProfileMorphSpec(
                        isReducedMotion = false,
                        remainingFraction = remaining
                    )
                )
            }
        }
    }

    val dockVisibilityProgressState = dockVisibilityAnimatable.asState()
    val profileMorphProgressState = profileMorphAnimatable.asState()
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
                        upcomingAlerts = repository.upcomingAlerts.value,
                        favoriteContacts = repository.favoriteContacts.value
                    )
                }
            }
            launch {
                repository.upcomingAlerts.collect { alerts ->
                    homeViewModel.syncRepositoryData(
                        recentContacts = repository.recentContacts.value,
                        upcomingAlerts = alerts,
                        favoriteContacts = repository.favoriteContacts.value
                    )
                }
            }
            launch {
                repository.favoriteContacts.collect { favs ->
                    homeViewModel.syncRepositoryData(
                        recentContacts = repository.recentContacts.value,
                        upcomingAlerts = repository.upcomingAlerts.value,
                        favoriteContacts = favs
                    )
                }
            }
        }
    }
    LaunchedEffect(repository, contactsViewModel) {
        if (contactsViewModel.repository !== repository) {
            launch {
                repository.contacts.collect { contacts ->
                    contactsViewModel.syncContacts(contacts)
                }
            }
            launch {
                repository.favoriteContacts.collect { favs ->
                    contactsViewModel.syncFavorites(favs)
                }
            }
            launch {
                repository.tags.collect { tags ->
                    contactsViewModel.syncTags(tags)
                }
            }
        }
    }

    val helloHaptics = rememberHelloHaptics()

    SnackbarFeedbackCollector(
        homeViewModel = homeViewModel,
        timelineViewModel = timelineViewModel,
        contactsViewModel = contactsViewModel,
        quickActionViewModel = quickActionViewModel,
        snackbarHostState = snackbarHostState,
        helloHaptics = helloHaptics
    )

    val startDestination = if (hasCompletedOnboarding) HomeRoute.ROUTE_NAME else OnboardingRoute.ROUTE_NAME
    val density = LocalDensity.current
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val restingDockHeight = run {
        val measuredDockDp = with(density) { dockHeightPx.toDp() }
        if (measuredDockDp > 0.dp) measuredDockDp else (84.dp + navBarBottomPadding)
    }
    val restingToastBottomPadding = maxOf(navBarBottomPadding, restingDockHeight) + HelloToastDefaults.BOTTOM_MARGIN_DP
    val hiddenToastDeltaDp = (restingDockHeight - navBarBottomPadding).coerceAtLeast(0.dp)

    CompositionLocalProvider(
        LocalHelloHaptics provides helloHaptics,
        LocalReducedMotion provides isReducedMotion
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                HelloToastHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(bottom = restingToastBottomPadding)
                        .graphicsLayer {
                            translationY = (hiddenToastDeltaDp * (1f - dockVisibilityProgressState.value)).toPx()
                        }
                )
            }
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
                        timelineViewModel = timelineViewModel,
                        contactsViewModel = contactsViewModel,
                        quickActionViewModel = quickActionViewModel,
                        terminologyState = terminologyState,
                        isReducedMotion = isReducedMotion,
                        rootTabBackProgress = rootTabBackProgress,
                        deeperRouteBackProgress = deeperRouteBackProgress,
                        selectionBackProgress = selectionBackProgress
                    )
                }

                MainAppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    preferencesRepository = resolvedPreferencesRepository,
                    quickActionViewModel = quickActionViewModel,
                    contactsViewModel = contactsViewModel,
                    preferredTerminology = preferredTerminology,
                    snackbarHostState = snackbarHostState,
                    userPreferences = userPreferences,
                    useRootBridgeForCurrentRoute = useRootBridgeForCurrentRoute,
                    deeperRouteBackProgress = deeperRouteBackProgress,
                    popHandoff = popHandoff,
                    onDeeperRouteBackProgress = { deeperRouteBackProgress = it },
                    onDeeperRouteBackCommit = { entryId ->
                        popHandoff.markCommitted(entryId)
                        navController.popBackStack()
                    },
                    onDetailSelectionModeChanged = { isDetailSelectionMode = it },
                    onDetailSelectionBackProgress = { detailSelectionBackProgress = it }
                )

                RootTabBackCoordinator(
                    currentRoute = currentRoute,
                    isContactsSelectionMode = isContactsSelectionMode,
                    isTimelineSelectionMode = isTimelineSelectionMode,
                    isReducedMotion = isReducedMotion,
                    onRootTabBackProgress = { rootTabBackProgress = it },
                    onRootTabBackCommit = { navController.popBackStack() },
                    onSelectionBackProgress = { selectionBackProgress = it },
                    onClearContactsSelection = { contactsViewModel.onEvent(ContactsUiEvent.OnClearSelection) },
                    onClearTimelineSelection = { timelineViewModel.onEvent(TimelineUiEvent.OnClearSelection) }
                )

                // Scrim & Floating Bottom Navigation Dock / Profile Encounter Pill scoped to prevent NavHost recomposition
                FloatingDockOverlay(
                    navController = navController,
                    currentRoute = currentRoute,
                    activeTab = activeTab,
                    shouldRenderDock = shouldRenderDock,
                    dockVisibilityProgressState = dockVisibilityProgressState,
                    profileMorphProgressState = profileMorphProgressState,
                    isProfileMorphReversing = { isProfileMorphReversing },
                    isProfileEncounterMode = isProfileDetailRoute,
                    onProfileEncounterClick = {
                        val contactId = activeDetailContactId
                        if (!contactId.isNullOrBlank()) {
                            val summary = quickActionViewModel.repository.contacts.value.firstOrNull { it.id == contactId }
                                ?: quickActionViewModel.repository.getContact(contactId)?.let { contact ->
                                    ContactSummary(
                                        id = contact.id,
                                        name = contact.name,
                                        contextSubtitle = contact.role ?: "",
                                        avatarInitials = extractAlphanumericInitials(contact.name)
                                    )
                                }
                                ?: ContactSummary(
                                    id = contactId,
                                    name = "Contact",
                                    contextSubtitle = "",
                                    avatarInitials = ""
                                )
                            quickActionViewModel.onEvent(QuickActionUiEvent.OnOpenLogEncounterForContact(summary))
                        }
                    },
                    overridePillProgress = if (rootTabBackProgress > 0f && isMainRootRoute(currentRoute) && activeTab != HomeNavTab.HOME) {
                        interpolateTabProgressForPredictiveBack(
                            currentTab = activeTab,
                            targetTab = HomeNavTab.HOME,
                            backProgress = rootTabBackProgress
                        )
                    } else null,
                    dockHeightPx = dockHeightPx,
                    onDockHeightChanged = { if (it > 0) dockHeightPx = it },
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
    timelineViewModel: TimelineViewModel,
    contactsViewModel: ContactsViewModel,
    quickActionViewModel: QuickActionViewModel,
    terminologyState: State<RelationshipTerminology>,
    isReducedMotion: Boolean = false,
    rootTabBackProgress: Float = 0f,
    deeperRouteBackProgress: Float = 0f,
    selectionBackProgress: Float = 0f
) {
    val targetProgress = when (activeTab) {
        HomeNavTab.HOME -> 0f
        HomeNavTab.PEOPLE -> 1f
        HomeNavTab.TIMELINE -> 2f
    }
    val slideAnimatable = remember { Animatable(targetProgress) }
    val fadeAnimatable = remember { Animatable(targetProgress) }
    val isTabPredictiveBack = rootTabBackProgress > 0f && isMainRootRoute(currentRoute) && activeTab != HomeNavTab.HOME && !isReducedMotion

    LaunchedEffect(isTabPredictiveBack, rootTabBackProgress, activeTab, isReducedMotion) {
        if (isTabPredictiveBack) {
            val scrubbed = interpolateTabProgressForPredictiveBack(
                currentTab = activeTab,
                targetTab = HomeNavTab.HOME,
                backProgress = rootTabBackProgress
            )
            slideAnimatable.snapTo(scrubbed)
            fadeAnimatable.snapTo(scrubbed)
        } else {
            if (slideAnimatable.value != targetProgress || slideAnimatable.targetValue != targetProgress) {
                launch {
                    if (isReducedMotion) {
                        slideAnimatable.snapTo(targetProgress)
                    } else {
                        slideAnimatable.animateTo(targetProgress, animationSpec = MotionTokens.tabTransitionTween())
                    }
                }
            }
            if (fadeAnimatable.value != targetProgress || fadeAnimatable.targetValue != targetProgress) {
                launch {
                    if (isReducedMotion) {
                        fadeAnimatable.snapTo(targetProgress)
                    } else {
                        fadeAnimatable.animateTo(targetProgress, animationSpec = MotionTokens.screenFadeSpec())
                    }
                }
            }
        }
    }
    val slideFractionState = slideAnimatable.asState()
    val fadeFractionState = fadeAnimatable.asState()

    val isDeeperRoute = currentRoute != null && !isMainRootRoute(currentRoute)
    val targetDepth = if (isDeeperRoute) 1f else 0f
    val depthAnimatable = remember { Animatable(targetDepth) }
    LaunchedEffect(isDeeperRoute, deeperRouteBackProgress, targetDepth, isReducedMotion) {
        if (isDeeperRoute && deeperRouteBackProgress > 0f && !isReducedMotion) {
            depthAnimatable.snapTo(
                resolveDepthFractionWithPredictiveBack(
                    isDeeperRoute = true,
                    animatedDepthFraction = 1f,
                    deeperBackProgress = deeperRouteBackProgress
                )
            )
        } else if (depthAnimatable.value != targetDepth || depthAnimatable.targetValue != targetDepth) {
            if (isReducedMotion) {
                depthAnimatable.snapTo(targetDepth)
            } else {
                depthAnimatable.animateTo(
                    targetValue = targetDepth,
                    animationSpec = tween(
                        durationMillis = MotionTokens.AOSP_TRANSITION_DURATION,
                        easing = MotionTokens.M3EmphasizedEasing
                    )
                )
            }
        }
    }
    val depthFractionState = depthAnimatable.asState()

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
    val onNavigateToArchivedContacts = remember(navController) {
        {
            navController.navigateSafely(ArchivedContactsRoute.ROUTE_NAME)
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
    val onLogEncounterForContact = remember(quickActionViewModel) {
        { contactId: String, initialNote: String? ->
            val summary = quickActionViewModel.repository.contacts.value.firstOrNull { it.id == contactId }
            if (summary != null) {
                quickActionViewModel.onEvent(QuickActionUiEvent.OnOpenLogEncounterForContact(summary, initialNote))
            } else {
                quickActionViewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
            }
        }
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
        // Slot 0: Home Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (activeTab == HomeNavTab.HOME) 1f else 0f)
                .graphicsLayer {
                    val delta = 0f - slideFractionState.value
                    val fade = (1f - kotlin.math.abs(0f - fadeFractionState.value)).coerceIn(0f, 1f)
                    if (kotlin.math.abs(delta) >= 0.999f && fade <= 0.001f) {
                        translationX = -size.width * 2f
                        alpha = 0f
                    } else {
                        translationX = delta * (size.width * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION)
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
                onLogEncounterForContact = onLogEncounterForContact,
                onAddContactClick = onAddContactClick,
                terminology = terminologyState.value
            )
        }

        // Slot 1: People / Relationship Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (activeTab == HomeNavTab.PEOPLE) 1f else 0f)
                .graphicsLayer {
                    val delta = 1f - slideFractionState.value
                    val fade = (1f - kotlin.math.abs(1f - fadeFractionState.value)).coerceIn(0f, 1f)
                    if (kotlin.math.abs(delta) >= 0.999f && fade <= 0.001f) {
                        translationX = size.width * 2f
                        alpha = 0f
                    } else {
                        translationX = delta * (size.width * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION)
                        alpha = fade
                    }
                }
        ) {
            PeopleTabDestination(
                contactsViewModel = contactsViewModel,
                onNavigateToHome = onNavigateToHome,
                onNavigateToArchivedContacts = onNavigateToArchivedContacts,
                onContactClick = onContactClick,
                onAddContactClick = onAddContactClick,
                terminology = terminologyState.value,
                isReducedMotion = isReducedMotion,
                selectionBackProgress = if (activeTab == HomeNavTab.PEOPLE) selectionBackProgress else 0f
            )
        }

        // Slot 2: Timeline Screen (App-Wide Living Timeline)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (activeTab == HomeNavTab.TIMELINE) 1f else 0f)
                .graphicsLayer {
                    val delta = 2f - slideFractionState.value
                    val fade = (1f - kotlin.math.abs(2f - fadeFractionState.value)).coerceIn(0f, 1f)
                    if (kotlin.math.abs(delta) >= 0.999f && fade <= 0.001f) {
                        translationX = size.width * 2f
                        alpha = 0f
                    } else {
                        translationX = delta * (size.width * NavigationTransitionConfig.MAIN_ROOT_SLIDE_OFFSET_FRACTION)
                        alpha = fade
                    }
                }
        ) {
            TimelineTabDestination(
                timelineViewModel = timelineViewModel,
                onContactClick = onContactClick,
                onLogEncounterForContact = onLogEncounterForContact,
                onLogEncounterClick = onLogEncounterClick,
                selectionBackProgress = if (activeTab == HomeNavTab.TIMELINE) selectionBackProgress else 0f
            )
        }
    }
}

@Composable
private fun TimelineTabDestination(
    timelineViewModel: TimelineViewModel,
    onContactClick: (String) -> Unit,
    onLogEncounterForContact: (String, String?) -> Unit,
    onLogEncounterClick: () -> Unit,
    selectionBackProgress: Float = 0f
) {
    val timelineUiState by timelineViewModel.uiState.collectAsState()
    ScreenDepthWrapper(isDepthVisible = false) {
        TimelineScreen(
            uiState = timelineUiState,
            onEvent = timelineViewModel::onEvent,
            onContactClick = onContactClick,
            onLogEncounterForContact = onLogEncounterForContact,
            onLogEncounterClick = onLogEncounterClick,
            selectionBackProgress = selectionBackProgress,
            registerSelectionBackHandler = false
        )
    }
}

@Composable
private fun HomeTabDestination(
    homeViewModel: HomeViewModel,
    onNavigateToPeople: () -> Unit,
    onContactClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogEncounterClick: () -> Unit,
    onLogEncounterForContact: (String, String?) -> Unit,
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
            onLogEncounterForContact = onLogEncounterForContact,
            onAddContactClick = onAddContactClick,
            terminology = terminology
        )
    }
}

@Composable
private fun PeopleTabDestination(
    contactsViewModel: ContactsViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToArchivedContacts: () -> Unit,
    onContactClick: (String) -> Unit,
    onAddContactClick: () -> Unit,
    terminology: RelationshipTerminology,
    isReducedMotion: Boolean = false,
    selectionBackProgress: Float = 0f
) {
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    ScreenDepthWrapper(isDepthVisible = false) {
        ContactsScreen(
            uiState = contactsUiState,
            onEvent = contactsViewModel::onEvent,
            onNavigateToHome = onNavigateToHome,
            onNavigateToArchivedContacts = onNavigateToArchivedContacts,
            onContactClick = onContactClick,
            onAddContactClick = onAddContactClick,
            terminology = terminology,
            isReducedMotion = isReducedMotion,
            selectionBackProgress = selectionBackProgress,
            registerSelectionBackHandler = false
        )
    }
}

@Composable
private fun MainAppNavHost(
    navController: NavHostController,
    startDestination: String,
    preferencesRepository: UserPreferencesRepository,
    quickActionViewModel: QuickActionViewModel,
    contactsViewModel: ContactsViewModel,
    preferredTerminology: RelationshipTerminology,
    snackbarHostState: SnackbarHostState,
    userPreferences: UserPreferences?,
    useRootBridgeForCurrentRoute: Boolean,
    deeperRouteBackProgress: Float,
    popHandoff: PredictiveBackPopHandoff,
    onDeeperRouteBackProgress: (Float) -> Unit,
    onDeeperRouteBackCommit: (String) -> Unit,
    onDetailSelectionModeChanged: (Boolean) -> Unit,
    onDetailSelectionBackProgress: (Float) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val helloHaptics = LocalHelloHaptics.current
    val isReducedMotion = userPreferences?.reduceAnimations == true
    val motionDuration = if (isReducedMotion) 0 else MotionTokens.AOSP_TRANSITION_DURATION
    val useRootBridgeState = rememberUpdatedState(useRootBridgeForCurrentRoute)
    val deeperBackProgressState = rememberUpdatedState(deeperRouteBackProgress)

    val settingsViewModel = remember {
        val wm = try { WorkManager.getInstance(context) } catch (_: Exception) { null }
        SettingsViewModel(preferencesRepository, quickActionViewModel.repository, wm)
    }

    val navGraph = remember(navController, startDestination, isReducedMotion) {
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
                        fallback = PersonDetailMotionConfig.rootPopEnterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootExitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootPopEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootExitTransition()
                    )
                }
            ) {
                // Visual content is kept warm in WarmRootTabsHost for 0ms first-frame latency
            }

            composable(
                route = TimelineRoute.ROUTE_NAME,
                enterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootPopEnterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootExitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootPopEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootExitTransition()
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
                        fallback = PersonDetailMotionConfig.rootPopEnterTransition()
                    )
                },
                exitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootExitTransition()
                    )
                },
                popEnterTransition = {
                    mainRootEnterTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootPopEnterTransition()
                    )
                },
                popExitTransition = {
                    mainRootExitTransition(
                        fromRoute = initialState.destination.route,
                        toRoute = targetState.destination.route,
                        fallback = PersonDetailMotionConfig.rootExitTransition()
                    )
                }
            ) {
                // Visual content is kept warm in WarmRootTabsHost for 0ms first-frame latency
            }

            settingsBridgedComposable(
                route = SettingsRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTerminology = { navController.navigateSafely(SettingsTerminologyRoute.ROUTE_NAME) },
                    onNavigateToFlows = { navController.navigateSafely(SettingsFlowsRoute.ROUTE_NAME) },
                    onNavigateToTags = { navController.navigateSafely(SettingsTagsRoute.ROUTE_NAME) },
                    onNavigateToData = { navController.navigateSafely(SettingsDataRoute.ROUTE_NAME) },
                    onNavigateToAppearance = { navController.navigateSafely(SettingsAppearanceRoute.ROUTE_NAME) },
                    onNavigateToPreferences = { navController.navigateSafely(SettingsPreferencesRoute.ROUTE_NAME) },
                    onNavigateToAbout = { navController.navigateSafely(SettingsAboutRoute.ROUTE_NAME) }
                )
            }

            settingsBridgedComposable(
                route = SettingsFlowsRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsFlowsScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = SettingsTerminologyRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsTerminologyScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = SettingsTagsRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsTagsScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = SettingsDataRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsDataScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = SettingsAppearanceRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsAppearanceScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = SettingsPreferencesRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val settingsUiState by settingsViewModel.uiState.collectAsState()
                SettingsPreferencesScreen(
                    uiState = settingsUiState,
                    onEvent = settingsViewModel::onEvent,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = SettingsAboutRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                SettingsAboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLicenses = { navController.navigateSafely(SettingsLicensesRoute.ROUTE_NAME) }
                )
            }

            settingsBridgedComposable(
                route = SettingsLicensesRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                SettingsLicensesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            settingsBridgedComposable(
                route = ArchivedContactsRoute.ROUTE_NAME,
                isReducedMotion = isReducedMotion,
                popHandoff = popHandoff,
                useRootBridge = { useRootBridgeState.value },
                deeperBackProgress = { deeperBackProgressState.value },
                onDeeperBackProgress = onDeeperRouteBackProgress,
                onDeeperBackCommit = onDeeperRouteBackCommit
            ) {
                val contactsUiState by contactsViewModel.uiState.collectAsState()
                ArchivedContactsScreen(
                    contacts = contactsUiState.archivedContacts,
                    onRestoreContact = { contactsViewModel.onEvent(ContactsUiEvent.OnRestoreContact(it)) },
                    onNavigateBack = { navController.popBackStack() },
                    terminology = preferredTerminology,
                    isReducedMotion = isReducedMotion
                )
            }

            composable(
                route = PersonDetailRoute.ROUTE_PATTERN,
                arguments = listOf(navArgument(PersonDetailRoute.ARG_CONTACT_ID) { type = NavType.StringType }),
                enterTransition = { PersonDetailMotionConfig.detailEnterTransition(isReducedMotion) },
                exitTransition = { PersonDetailMotionConfig.detailExitTransition(isReducedMotion) },
                popEnterTransition = { PersonDetailMotionConfig.detailPopEnterTransition(isReducedMotion) },
                popExitTransition = {
                    if (popHandoff.shouldSkipPopExit(initialState.id)) ExitTransition.None
                    else PersonDetailMotionConfig.detailPopExitTransition(isReducedMotion)
                }
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString(PersonDetailRoute.ARG_CONTACT_ID) ?: ""
                val detailViewModel = remember(contactId) {
                    PersonDetailViewModel(contactId = contactId, repository = quickActionViewModel.repository)
                }
                val detailUiState by detailViewModel.uiState.collectAsState()

                LaunchedEffect(detailUiState.isSelectionMode) {
                    onDetailSelectionModeChanged(detailUiState.isSelectionMode)
                    if (!detailUiState.isSelectionMode) {
                        onDetailSelectionBackProgress(0f)
                    }
                }
                DisposableEffect(backStackEntry.id) {
                    onDispose {
                        onDetailSelectionModeChanged(false)
                        onDetailSelectionBackProgress(0f)
                    }
                }

                DeeperRouteBackWrapper(
                    entryId = backStackEntry.id,
                    popHandoff = popHandoff,
                    useRootBridge = useRootBridgeState.value && !detailUiState.isSelectionMode,
                    isReducedMotion = isReducedMotion,
                    backProgress = deeperBackProgressState.value,
                    onBackProgress = onDeeperRouteBackProgress,
                    onBackCommit = { onDeeperRouteBackCommit(backStackEntry.id) }
                ) {
                    ScreenDepthWrapper(isDepthVisible = false) {
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
                                        avatarInitials = extractAlphanumericInitials(detailUiState.contact?.name ?: "")
                                    )
                                quickActionViewModel.onEvent(QuickActionUiEvent.OnOpenLogEncounterForContact(summary))
                            },
                            externalSnackbarHostState = snackbarHostState,
                            onSelectionBackProgressChanged = onDetailSelectionBackProgress,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(PersonDetailMotionConfig.DETAIL_Z_INDEX)
                        )
                    }
                }
            }

            composable(
                route = PersonEditRoute.ROUTE_PATTERN,
                arguments = listOf(navArgument(PersonEditRoute.ARG_CONTACT_ID) { type = NavType.StringType }),
                enterTransition = { PersonDetailMotionConfig.detailEnterTransition(isReducedMotion) },
                exitTransition = { PersonDetailMotionConfig.detailExitTransition(isReducedMotion) },
                popEnterTransition = { PersonDetailMotionConfig.detailPopEnterTransition(isReducedMotion) },
                popExitTransition = {
                    if (popHandoff.shouldSkipPopExit(initialState.id)) ExitTransition.None
                    else PersonDetailMotionConfig.detailPopExitTransition(isReducedMotion)
                }
            ) { backStackEntry ->
                DeeperRouteBackWrapper(
                    entryId = backStackEntry.id,
                    popHandoff = popHandoff,
                    useRootBridge = useRootBridgeState.value,
                    isReducedMotion = isReducedMotion,
                    backProgress = deeperBackProgressState.value,
                    onBackProgress = onDeeperRouteBackProgress,
                    onBackCommit = { onDeeperRouteBackCommit(backStackEntry.id) }
                ) {
                    ScreenDepthWrapper(isDepthVisible = false) {
                        val contactId = backStackEntry.arguments?.getString(PersonEditRoute.ARG_CONTACT_ID) ?: ""
                        val editViewModel = remember(backStackEntry, contactId) {
                            ViewModelProvider(
                                backStackEntry,
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return PersonEditViewModel(
                                            contactId = contactId,
                                            repository = quickActionViewModel.repository,
                                            avatarStorage = AvatarStorageManager(context)
                                        ) as T
                                    }
                                }
                            )[PersonEditViewModel::class.java]
                        }
                        val editUiState by editViewModel.uiState.collectAsState()

                        PersonEditScreen(
                            uiState = editUiState,
                            onEvent = editViewModel::onEvent,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenAvatarPlayground = {
                                navController.navigateSafely(AvatarPlaygroundRoute.createRoute(contactId))
                            },
                            onSaveSuccess = {
                                scope.launch {
                                    snackbarHostState.showHelloToast(
                                        message = "Contact updated",
                                        durationMs = TOAST_DURATION_INFORMATIONAL_MS
                                    )
                                }
                                navController.popBackStack()
                            },
                            onLifecycleSuccess = { isArchived ->
                                scope.launch {
                                    val msg = if (isArchived) "Contact archived" else "Contact deleted"
                                    val result = snackbarHostState.showHelloToast(
                                        message = msg,
                                        actionLabel = "Undo",
                                        durationMs = TOAST_DURATION_ACTIONABLE_MS
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        helloHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                                        val restored = if (isArchived) {
                                            quickActionViewModel.repository.undoLastArchive()
                                        } else {
                                            quickActionViewModel.repository.undoLastDeleteContact()
                                        }
                                        if (restored) {
                                            snackbarHostState.showHelloToast(
                                                message = "Contact restored",
                                                durationMs = TOAST_DURATION_INFORMATIONAL_MS
                                            )
                                        }
                                    }
                                }
                                if (!navController.popBackStack(PersonDetailRoute.ROUTE_PATTERN, inclusive = true)) {
                                    if (!navController.popBackStack(PeopleRoute.ROUTE_NAME, inclusive = false)) {
                                        navController.popBackStack(HomeRoute.ROUTE_NAME, inclusive = false)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            composable(
                route = AvatarPlaygroundRoute.ROUTE_PATTERN,
                arguments = listOf(navArgument(AvatarPlaygroundRoute.ARG_CONTACT_ID) { type = NavType.StringType }),
                enterTransition = { PersonDetailMotionConfig.detailEnterTransition(isReducedMotion) },
                exitTransition = { PersonDetailMotionConfig.detailExitTransition(isReducedMotion) },
                popEnterTransition = { PersonDetailMotionConfig.detailPopEnterTransition(isReducedMotion) },
                popExitTransition = {
                    if (popHandoff.shouldSkipPopExit(initialState.id)) ExitTransition.None
                    else PersonDetailMotionConfig.detailPopExitTransition(isReducedMotion)
                }
            ) { backStackEntry ->
                DeeperRouteBackWrapper(
                    entryId = backStackEntry.id,
                    popHandoff = popHandoff,
                    useRootBridge = useRootBridgeState.value,
                    isReducedMotion = isReducedMotion,
                    backProgress = deeperBackProgressState.value,
                    onBackProgress = onDeeperRouteBackProgress,
                    onBackCommit = { onDeeperRouteBackCommit(backStackEntry.id) }
                ) {
                    ScreenDepthWrapper(isDepthVisible = false) {
                        val contactId = backStackEntry.arguments?.getString(AvatarPlaygroundRoute.ARG_CONTACT_ID) ?: ""
                        val parentEditEntry = remember(backStackEntry) {
                            runCatching { navController.getBackStackEntry(PersonEditRoute.ROUTE_PATTERN) }.getOrNull()
                        }
                        val parentEditViewModel = remember(parentEditEntry) {
                            parentEditEntry?.let { entry ->
                                runCatching {
                                    ViewModelProvider(entry)[PersonEditViewModel::class.java]
                                }.getOrNull()
                            }
                        }

                        val playgroundViewModel = remember(backStackEntry, contactId) {
                            val initialName = parentEditViewModel?.uiState?.value?.name
                                ?: quickActionViewModel.repository.getContact(contactId)?.name.orEmpty()
                            val initialConfig = parentEditViewModel?.uiState?.value?.notionistConfig
                                ?: quickActionViewModel.repository.getContact(contactId)?.notionistConfig
                            ViewModelProvider(
                                backStackEntry,
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        return AvatarPlaygroundViewModel(
                                            contactName = initialName,
                                            initialConfigJson = initialConfig,
                                            fetcher = AndroidNotionistAvatarFetcher(context)
                                        ) as T
                                    }
                                }
                            )[AvatarPlaygroundViewModel::class.java]
                        }
                        val playgroundUiState by playgroundViewModel.uiState.collectAsState()

                        AvatarPlaygroundScreen(
                            uiState = playgroundUiState,
                            onEvent = playgroundViewModel::onEvent,
                            loadTraitThumbnail = playgroundViewModel::loadTraitThumbnail,
                            onNavigateBack = { navController.popBackStack() },
                            onApplyAvatar = { pngBytes, configJson ->
                                parentEditViewModel?.onEvent(
                                    PersonEditUiEvent.OnNotionistAvatarApplied(
                                        pngBytes = pngBytes,
                                        configJson = configJson
                                    )
                                )
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        graph = navGraph,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { aospSharedAxisEnter(durationMillis = motionDuration) },
        exitTransition = { aospSharedAxisExit(durationMillis = motionDuration) },
        popEnterTransition = { aospSharedAxisPopEnter(durationMillis = motionDuration) },
        popExitTransition = { aospSharedAxisPopExit(durationMillis = motionDuration) }
    )
}

@Composable
private fun RootTabBackCoordinator(
    currentRoute: String?,
    isContactsSelectionMode: Boolean,
    isTimelineSelectionMode: Boolean,
    isReducedMotion: Boolean,
    onRootTabBackProgress: (Float) -> Unit,
    onRootTabBackCommit: () -> Unit,
    onSelectionBackProgress: (Float) -> Unit,
    onClearContactsSelection: () -> Unit,
    onClearTimelineSelection: () -> Unit
) {
    val isContactsSelectionActive = currentRoute == PeopleRoute.ROUTE_NAME && isContactsSelectionMode
    val isTimelineSelectionActive = currentRoute == TimelineRoute.ROUTE_NAME && isTimelineSelectionMode
    val isSelectionActive = isContactsSelectionActive || isTimelineSelectionActive
    val isSecondaryRootTab = currentRoute == PeopleRoute.ROUTE_NAME || currentRoute == TimelineRoute.ROUTE_NAME

    ScrubbablePredictiveBackHandler(
        enabled = isSecondaryRootTab && !isSelectionActive,
        isReducedMotion = isReducedMotion,
        onProgressChanged = onRootTabBackProgress,
        finishOnCommit = false,
        onCommit = onRootTabBackCommit
    )

    ScrubbablePredictiveBackHandler(
        enabled = isSelectionActive,
        isReducedMotion = isReducedMotion,
        onProgressChanged = onSelectionBackProgress,
        finishOnCommit = false,
        onCommit = {
            if (isContactsSelectionActive) onClearContactsSelection()
            else if (isTimelineSelectionActive) onClearTimelineSelection()
        }
    )
}

@Composable
private fun DeeperRouteBackWrapper(
    entryId: String,
    popHandoff: PredictiveBackPopHandoff,
    useRootBridge: Boolean,
    isReducedMotion: Boolean,
    backProgress: Float,
    onBackProgress: (Float) -> Unit,
    onBackCommit: () -> Unit,
    content: @Composable () -> Unit
) {
    DisposableEffect(entryId) {
        onDispose {
            if (popHandoff.onExitingEntryDisposed(entryId)) {
                onBackProgress(0f)
            }
        }
    }
    val isCommittedExit = popHandoff.shouldSkipPopExit(entryId)
    ScrubbablePredictiveBackHandler(
        enabled = useRootBridge && !isCommittedExit,
        isReducedMotion = isReducedMotion,
        onProgressChanged = onBackProgress,
        finishOnCommit = true,
        onCommit = onBackCommit
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (useRootBridge || isCommittedExit) {
                    Modifier.predictiveDeeperRoutePopExit {
                        if (isCommittedExit) 1f else backProgress
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

private fun NavGraphBuilder.settingsBridgedComposable(
    route: String,
    isReducedMotion: Boolean,
    popHandoff: PredictiveBackPopHandoff,
    useRootBridge: () -> Boolean,
    deeperBackProgress: () -> Float,
    onDeeperBackProgress: (Float) -> Unit,
    onDeeperBackCommit: (String) -> Unit,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = { SettingsMotionConfig.settingsEnterTransition(isReducedMotion) },
        exitTransition = { SettingsMotionConfig.settingsExitTransition(isReducedMotion) },
        popEnterTransition = { SettingsMotionConfig.settingsPopEnterTransition(isReducedMotion) },
        popExitTransition = {
            if (popHandoff.shouldSkipPopExit(initialState.id)) ExitTransition.None
            else SettingsMotionConfig.settingsPopExitTransition(isReducedMotion)
        }
    ) { backStackEntry ->
        DeeperRouteBackWrapper(
            entryId = backStackEntry.id,
            popHandoff = popHandoff,
            useRootBridge = useRootBridge(),
            isReducedMotion = isReducedMotion,
            backProgress = deeperBackProgress(),
            onBackProgress = onDeeperBackProgress,
            onBackCommit = { onDeeperBackCommit(backStackEntry.id) }
        ) {
            ScreenDepthWrapper(isDepthVisible = false) {
                content(backStackEntry)
            }
        }
    }
}

@Composable
private fun FloatingDockOverlay(
    navController: NavHostController,
    currentRoute: String?,
    activeTab: HomeNavTab,
    shouldRenderDock: Boolean,
    dockVisibilityProgressState: State<Float>,
    profileMorphProgressState: State<Float>,
    isProfileMorphReversing: () -> Boolean,
    isProfileEncounterMode: Boolean,
    onProfileEncounterClick: () -> Unit,
    overridePillProgress: Float? = null,
    dockHeightPx: Int,
    onDockHeightChanged: (Int) -> Unit,
    homeViewModel: HomeViewModel,
    quickActionViewModel: QuickActionViewModel,
    preferredTerminology: RelationshipTerminology,
    modifier: Modifier = Modifier
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val isSpeedDialOpen = homeUiState.isSpeedDialOpen

    androidx.activity.compose.BackHandler(enabled = isSpeedDialOpen && isMainRootRoute(currentRoute)) {
        homeViewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
    }

    // Scrim to dismiss the 2-button Speed Dial column when tapping outside on root tabs
    AnimatedVisibility(
        visible = isSpeedDialOpen && isMainRootRoute(currentRoute),
        enter = fadeIn(animationSpec = tween(durationMillis = MotionTokens.SCRIM_FADE_IN_DURATION_MS, easing = MotionTokens.StrongEaseOut)),
        exit = fadeOut(animationSpec = tween(durationMillis = MotionTokens.SCRIM_FADE_OUT_DURATION_MS, easing = MotionTokens.StrongEaseOut))
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
                val targetRoute = when (tab) {
                    HomeNavTab.HOME -> HomeRoute.ROUTE_NAME
                    HomeNavTab.PEOPLE -> PeopleRoute.ROUTE_NAME
                    HomeNavTab.TIMELINE -> TimelineRoute.ROUTE_NAME
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
            overridePillProgress = overridePillProgress,
            profileMorphProgressProvider = { profileMorphProgressState.value },
            isProfileMorphReversing = isProfileMorphReversing,
            isProfileEncounterMode = isProfileEncounterMode,
            onProfileEncounterClick = onProfileEncounterClick,
            modifier = modifier
                .onSizeChanged { size ->
                    if (size.height > 0) {
                        onDockHeightChanged(size.height)
                    }
                }
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
    timelineViewModel: TimelineViewModel,
    contactsViewModel: ContactsViewModel,
    quickActionViewModel: QuickActionViewModel,
    snackbarHostState: SnackbarHostState,
    helloHaptics: HelloHaptics = LocalHelloHaptics.current
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val timelineUiState by timelineViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    val quickActionUiState by quickActionViewModel.uiState.collectAsState()

    LaunchedEffect(contactsUiState.userMessage) {
        contactsUiState.userMessage?.let { msg ->
            val hasUndo = msg.startsWith("Deleted", ignoreCase = true) ||
                msg.contains("archive", ignoreCase = true) ||
                msg.contains("restored", ignoreCase = true)
            val result = if (hasUndo) {
                snackbarHostState.showHelloToast(
                    message = msg,
                    actionLabel = "Undo",
                    durationMs = TOAST_DURATION_ACTIONABLE_MS
                )
            } else {
                snackbarHostState.showHelloToast(
                    message = msg,
                    durationMs = TOAST_DURATION_INFORMATIONAL_MS
                )
            }
            if (result == SnackbarResult.ActionPerformed) {
                helloHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                if (msg.contains("archive", ignoreCase = true)) {
                    if (quickActionViewModel.repository.undoLastArchive()) {
                        snackbarHostState.showHelloToast(
                            message = "Contact restored",
                            durationMs = TOAST_DURATION_INFORMATIONAL_MS
                        )
                    }
                } else if (msg.contains("restored", ignoreCase = true)) {
                    if (quickActionViewModel.repository.undoLastArchive()) {
                        snackbarHostState.showHelloToast(
                            message = "Contact archived",
                            durationMs = TOAST_DURATION_INFORMATIONAL_MS
                        )
                    }
                } else {
                    if (quickActionViewModel.repository.undoLastDeleteContact()) {
                        snackbarHostState.showHelloToast(
                            message = "Contact restored",
                            durationMs = TOAST_DURATION_INFORMATIONAL_MS
                        )
                    }
                }
            }
            contactsViewModel.onEvent(ContactsUiEvent.OnDismissUserMessage)
        }
    }
    LaunchedEffect(homeUiState.userMessage) {
        homeUiState.userMessage?.let {
            snackbarHostState.showHelloToast(it, durationMs = TOAST_DURATION_INFORMATIONAL_MS)
            homeViewModel.onEvent(HomeUiEvent.OnDismissUserMessage)
        }
    }
    LaunchedEffect(timelineUiState.userMessage) {
        timelineUiState.userMessage?.let { msg ->
            val hasUndo = msg.startsWith("Deleted", ignoreCase = true)
            val result = if (hasUndo) {
                snackbarHostState.showHelloToast(
                    message = msg,
                    actionLabel = "Undo",
                    durationMs = TOAST_DURATION_ACTIONABLE_MS
                )
            } else {
                snackbarHostState.showHelloToast(
                    message = msg,
                    durationMs = TOAST_DURATION_INFORMATIONAL_MS
                )
            }
            if (result == SnackbarResult.ActionPerformed) {
                helloHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                if (quickActionViewModel.repository.undoLastDeleteEncounters()) {
                    snackbarHostState.showHelloToast(
                        message = "Encounter restored",
                        durationMs = TOAST_DURATION_INFORMATIONAL_MS
                    )
                }
            }
            timelineViewModel.onEvent(TimelineUiEvent.OnDismissUserMessage)
        }
    }
    LaunchedEffect(quickActionUiState.userMessage) {
        quickActionUiState.userMessage?.let {
            if (!quickActionUiState.isOpen) {
                helloHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
            }
            snackbarHostState.showHelloToast(it, durationMs = TOAST_DURATION_INFORMATIONAL_MS)
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
                    override suspend fun setUserName(name: String) {}
                    override suspend fun setOnboardingCompleted(completed: Boolean) {}
                    override suspend fun setPreferredTerminology(terminology: RelationshipTerminology) {}
                    override suspend fun setThemeMode(themeMode: com.example.hello.data.preferences.ThemeMode) {}
                    override suspend fun setThemeStyle(themeStyle: com.example.hello.data.preferences.ThemeStyle) {}
                    override suspend fun setDynamicColor(enabled: Boolean) {}
                    override suspend fun setReduceAnimations(enabled: Boolean) {}
                    override suspend fun setDefaultCadenceDays(days: Int) {}
                    override suspend fun setNotificationTime(hour: Int, minute: Int) {}
                    override suspend fun setDefaultIncludeLocation(enabled: Boolean) {}
                    override suspend fun setActiveFlowPresetId(presetId: String) {}
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
                    override suspend fun setUserName(name: String) {}
                    override suspend fun setOnboardingCompleted(completed: Boolean) {}
                    override suspend fun setPreferredTerminology(terminology: RelationshipTerminology) {}
                    override suspend fun setThemeMode(themeMode: com.example.hello.data.preferences.ThemeMode) {}
                    override suspend fun setThemeStyle(themeStyle: com.example.hello.data.preferences.ThemeStyle) {}
                    override suspend fun setDynamicColor(enabled: Boolean) {}
                    override suspend fun setReduceAnimations(enabled: Boolean) {}
                    override suspend fun setDefaultCadenceDays(days: Int) {}
                    override suspend fun setNotificationTime(hour: Int, minute: Int) {}
                    override suspend fun setDefaultIncludeLocation(enabled: Boolean) {}
                    override suspend fun setActiveFlowPresetId(presetId: String) {}
                }
            }
        )
    }
}
