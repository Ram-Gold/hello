package com.example.hello.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.platform.notification.FollowUpNotificationHelper
import com.example.hello.ui.components.HelloToastDefaults
import com.example.hello.ui.components.HelloToastHost
import com.example.hello.ui.components.NotificationExplainerDialog
import com.example.hello.ui.components.TOAST_DURATION_INFORMATIONAL_MS
import com.example.hello.ui.components.showHelloToast

@Composable
fun SettingsPreferencesScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val alarmScheduler = remember(context) { FollowUpAlarmScheduler(context) }
    var notificationsDeliverable by remember { mutableStateOf(FollowUpNotificationHelper.isFullyDeliverable(context)) }
    var exactAlarmsGranted by remember { mutableStateOf(alarmScheduler.canScheduleExactAlarms()) }
    var showNotificationExplainer by remember { mutableStateOf(false) }
    var hasRequestedRuntimePermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        hasRequestedRuntimePermission = true
        notificationsDeliverable = FollowUpNotificationHelper.isFullyDeliverable(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsDeliverable = FollowUpNotificationHelper.isFullyDeliverable(context)
                exactAlarmsGranted = alarmScheduler.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage ?: return@LaunchedEffect
        onEvent(SettingsUiEvent.OnDismissUserMessage)
        snackbarHostState.showHelloToast(
            message = msg,
            durationMs = TOAST_DURATION_INFORMATIONAL_MS
        )
    }

    val notificationTimes = listOf(
        8 to Pair("08:00 AM", "Early morning reminder delivery"),
        9 to Pair("09:00 AM", "Standard morning delivery (default)"),
        10 to Pair("10:00 AM", "Mid-morning delivery"),
        18 to Pair("06:00 PM", "Evening review delivery")
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            HelloToastHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + HelloToastDefaults.BOTTOM_MARGIN_DP)
            )
        },
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
                val showExactAlarmRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                val itemCount = if (showExactAlarmRow) 2 else 1
                SettingsSubsection(title = "Notification Access") {
                    SettingsItem(
                        title = "Reminder Alerts",
                        subtitle = if (notificationsDeliverable) {
                            "Enabled for scheduled on-device reminders"
                        } else {
                            "Turned off in Android settings. Tap to enable alerts."
                        },
                        shape = shapeFor(0, itemCount),
                        leadingIcon = {
                            Icon(
                                imageVector = if (notificationsDeliverable) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.Rounded.NotificationsOff
                                },
                                contentDescription = null,
                                tint = if (notificationsDeliverable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        },
                        trailingContent = {
                            Text(
                                text = if (notificationsDeliverable) "On" else "Off",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (notificationsDeliverable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        },
                        onClick = {
                            if (notificationsDeliverable) {
                                runCatching {
                                    context.startActivity(
                                        FollowUpNotificationHelper.createChannelNotificationSettingsIntent(context)
                                    )
                                }
                            } else {
                                showNotificationExplainer = true
                            }
                        }
                    )

                    if (showExactAlarmRow) {
                        SettingsItem(
                            title = "Exact Reminder Timing",
                            subtitle = if (exactAlarmsGranted) {
                                "Granted: reminders fire at the exact minute"
                            } else {
                                "Optional: standard Doze-safe delivery fires within minutes"
                            },
                            shape = shapeFor(1, itemCount),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = if (exactAlarmsGranted) "Exact" else "Standard",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        FollowUpAlarmScheduler.createExactAlarmSettingsIntent(context)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item {
                val weekStartOptions = WeekStartDay.entries
                SettingsSubsection(title = "Calendar") {
                    weekStartOptions.forEachIndexed { index, option ->
                        SelectableSettingItem(
                            title = option.title,
                            subtitle = option.subtitle,
                            selected = uiState.firstDayOfWeek == option,
                            onClick = { onEvent(SettingsUiEvent.OnSelectFirstDayOfWeek(option)) },
                            shape = shapeFor(index, weekStartOptions.size)
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

    if (showNotificationExplainer) {
        val openSettings = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            FollowUpNotificationHelper.hasNotificationPermission(context) ||
            hasRequestedRuntimePermission
        NotificationExplainerDialog(
            openSystemSettings = openSettings,
            onAllowClick = {
                showNotificationExplainer = false
                if (openSettings) {
                    val intent = if (!FollowUpNotificationHelper.hasNotificationPermission(context)) {
                        FollowUpNotificationHelper.createAppNotificationSettingsIntent(context)
                    } else {
                        FollowUpNotificationHelper.createChannelNotificationSettingsIntent(context)
                    }
                    runCatching { context.startActivity(intent) }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasRequestedRuntimePermission = true
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismissClick = { showNotificationExplainer = false }
        )
    }
}
