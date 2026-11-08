package com.example.hello.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.hello.ui.components.HelloToastHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.RelationshipTerminology

@Composable
fun SettingsTerminologyScreen(
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            HelloToastHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
            )
        },
        topBar = {
            PixelStyleTopBar(
                title = "Display Terminology",
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
                SettingsSubsection(title = "Layer 1 • Universal Directory Containers (Recommended)") {
                    val primaryEntries = RelationshipTerminology.primaryDirectoryOptions
                    primaryEntries.forEachIndexed { index, option ->
                        val titleText = if (option == RelationshipTerminology.PEOPLE) {
                            "${option.pluralTitle} (Recommended)"
                        } else {
                            option.pluralTitle
                        }
                        SelectableSettingItem(
                            title = titleText,
                            subtitle = option.description,
                            selected = uiState.terminology == option,
                            onClick = { onEvent(SettingsUiEvent.OnSelectTerminology(option)) },
                            shape = shapeFor(index, primaryEntries.size)
                        )
                    }
                }
            }

            item {
                SettingsSubsection(title = "Specialized Labels") {
                    val secondaryEntries = RelationshipTerminology.entries.filterNot { it.isPrimaryDirectoryOption }
                    secondaryEntries.forEachIndexed { index, option ->
                        SelectableSettingItem(
                            title = option.pluralTitle,
                            subtitle = option.description,
                            selected = uiState.terminology == option,
                            onClick = { onEvent(SettingsUiEvent.OnSelectTerminology(option)) },
                            shape = shapeFor(index, secondaryEntries.size)
                        )
                    }
                }
            }

            item {
                SettingsSubsection(title = "Two-Layer Taxonomy Architecture") {
                    Surface(
                        shape = shapeFor(0, 1),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Why Two Layers? (ADR-0005)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Layer 1 names your global directory container (${uiState.terminology.pluralTitle}), while Layer 2 uses flat #tags (#friends, #work, #family, #uni) for life spheres. This prevents overlapping labels and keeps your SQLite & Markdown schemas 100% portable.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}
