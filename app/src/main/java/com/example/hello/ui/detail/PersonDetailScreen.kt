package com.example.hello.ui.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.detail.components.DetailCollapsingHeader
import com.example.hello.ui.detail.components.LivingTimeline
import com.example.hello.ui.detail.components.PropertiesPanel
import com.example.hello.ui.theme.HelloTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    uiState: PersonDetailUiState,
    onEvent: (PersonDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onEvent(PersonDetailUiEvent.OnDismissUserMessage)
        }
    }

    val contactName = uiState.contact?.name ?: uiState.contactSummary?.name ?: "Contact"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = contactName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(PersonDetailUiEvent.OnBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(PersonDetailUiEvent.OnEditClick) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Contact",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item(key = "header") {
                    DetailCollapsingHeader(
                        contact = uiState.contact,
                        summary = uiState.contactSummary,
                        onPhoneClick = { onEvent(PersonDetailUiEvent.OnPhoneClick) },
                        onMessageClick = { onEvent(PersonDetailUiEvent.OnMessageClick) },
                        onEmailClick = { onEvent(PersonDetailUiEvent.OnEmailClick) }
                    )
                }

                item(key = "properties") {
                    PropertiesPanel(
                        contact = uiState.contact,
                        cadenceStatus = uiState.cadenceStatus,
                        allTags = uiState.tagsList
                    )
                }

                item(key = "timeline") {
                    LivingTimeline(
                        encounters = uiState.encounters,
                        placesMap = uiState.placesMap,
                        onLogEncounterClick = { onEvent(PersonDetailUiEvent.OnLogEncounterClick) }
                    )
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Preview(name = "Person Detail Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PersonDetailScreenPreview() {
    HelloTheme(darkTheme = false) {
        val sample = MockDataProvider.contactSarah
        PersonDetailScreen(
            uiState = PersonDetailUiState(
                contact = sample,
                contactSummary = ContactSummary(
                    id = sample.id,
                    name = sample.name,
                    contextSubtitle = "Staff Engineer",
                    circles = sample.circles,
                    avatarInitials = "SL",
                    avatarBgIndex = 1
                ),
                encounters = MockDataProvider.encounters.filter { it.contactId == sample.id },
                placesMap = MockDataProvider.places.associateBy { it.id },
                tagsList = MockDataProvider.tags,
                cadenceStatus = CadenceStatusInfo(
                    health = CadenceHealth.ON_TRACK,
                    displayText = "On track • Due in 29 days",
                    subText = "Cadence: Every 30 days • Last met 1 day ago"
                ),
                isLoading = false
            ),
            onEvent = {}
        )
    }
}
