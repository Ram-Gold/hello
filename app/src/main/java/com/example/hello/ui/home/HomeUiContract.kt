package com.example.hello.ui.home

import com.example.hello.domain.model.CardType
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.CircleGroupConfig
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.MyCardData
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.domain.model.UpcomingAlert

data class HomeUiState(
    val userName: String = "Alex",
    val activeCardType: CardType = CardType.WORK,
    val myCards: Map<CardType, MyCardData> = emptyMap(),
    val isQrExpanded: Boolean = false,
    val upcomingAlerts: List<UpcomingAlert> = emptyList(),
    val recentContacts: List<ContactSummary> = emptyList(),
    val isSpeedDialOpen: Boolean = false,
    val selectedNavTab: HomeNavTab = HomeNavTab.HOME,
    val userMessage: String? = null,
    val isDrawerOpen: Boolean = false,
    val selectedCircle: Circle? = null,
    val circleConfigs: Map<Circle, CircleGroupConfig> = emptyMap()
)

sealed interface HomeUiEvent {
    data class OnCardTypeSelect(val cardType: CardType) : HomeUiEvent
    data class OnExpandQr(val expanded: Boolean) : HomeUiEvent
    data class OnReminderDone(val alertId: String) : HomeUiEvent
    data class OnReminderSnooze(val alertId: String) : HomeUiEvent
    data class OnQuickAddContact(val name: String, val circles: Set<Circle>, val initialNote: String) : HomeUiEvent
    data class OnQuickUpdateContact(val contactId: String, val note: String) : HomeUiEvent
    data class OnContactClick(val contactId: String) : HomeUiEvent
    data object OnToggleSpeedDial : HomeUiEvent
    data object OnCloseSpeedDial : HomeUiEvent
    data class OnSpeedDialAction(val action: SpeedDialAction) : HomeUiEvent
    data class OnNavTabSelect(val tab: HomeNavTab) : HomeUiEvent
    data object OnSettingsClick : HomeUiEvent
    data object OnDismissUserMessage : HomeUiEvent
    data object OnOpenDrawer : HomeUiEvent
    data object OnCloseDrawer : HomeUiEvent
    data class OnSelectCircle(val circle: Circle?) : HomeUiEvent
    data class OnUpdateCircleConfig(val config: CircleGroupConfig) : HomeUiEvent
}
