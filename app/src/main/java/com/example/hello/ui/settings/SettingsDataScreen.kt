package com.example.hello.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDataScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onEvent(SettingsUiEvent.OnDismissUserMessage)
        }
    }

    LaunchedEffect(uiState.exportSharePayload) {
        uiState.exportSharePayload?.let { payload ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, payload)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Hello Data")
            context.startActivity(shareIntent)
            onEvent(SettingsUiEvent.OnClearExportPayload)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            onEvent(SettingsUiEvent.OnStartExportBundle(uri.toString()))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PixelStyleTopBar(
                title = "Data & Sovereign Storage",
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
                SettingsSubsection(title = "Storage Invariant (ADR-0006)") {
                    Surface(
                        shape = shapeFor(0, 1),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SQLite Truth + Multi-Format Portability",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Room DB (SQLite + FTS5) provides zero-latency local queries with sub-2ms mutations. An on-demand WorkManager pipeline packages all data into JSON, Markdown notes, and CSV tabular sheets saved via Storage Access Framework.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            item {
                val totalExportItems = 3
                SettingsSubsection(title = "Export & Backup Operations") {
                    SettingsItem(
                        title = "Export Multi-Format Bundle (.zip)",
                        subtitle = "Packages JSON schemas, Obsidian notes, and CSV sheets",
                        shape = shapeFor(0, totalExportItems),
                        leadingIcon = {
                            Icon(Icons.Outlined.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = { exportLauncher.launch("hello-export-${System.currentTimeMillis()}.zip") }
                    )

                    SettingsItem(
                        title = "Export Contacts as Markdown (.zip / text)",
                        subtitle = "Clean Markdown notes with frontmatter for Obsidian/Logseq",
                        shape = shapeFor(1, totalExportItems),
                        leadingIcon = {
                            Icon(Icons.Outlined.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = { onEvent(SettingsUiEvent.OnExportMarkdownZip) }
                    )

                    SettingsItem(
                        title = "Export Backup (.json)",
                        subtitle = "Single JSON snapshot for automated or external backup",
                        shape = shapeFor(2, totalExportItems),
                        leadingIcon = {
                            Icon(Icons.Outlined.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = { onEvent(SettingsUiEvent.OnExportBackupJson) }
                    )
                }
            }

            item {
                SettingsSubsection(title = "Local Cache Maintenance") {
                    SettingsItem(
                        title = "Rebuild Cache from Markdown (<2s)",
                        subtitle = "Reindexes FTS5 SQLite tables from disk state if needed",
                        shape = shapeFor(0, 1),
                        leadingIcon = {
                            Icon(Icons.Outlined.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = { onEvent(SettingsUiEvent.OnRebuildCache) }
                    )
                }
            }
        }
    }
}
