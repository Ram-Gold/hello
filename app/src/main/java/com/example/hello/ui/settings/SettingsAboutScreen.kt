package com.example.hello.ui.settings

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsAboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PixelStyleTopBar(
                title = "About Hello",
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
                SettingsSubsection(title = "Application") {
                    SettingsItem(
                        title = "Hello Personal CRM",
                        subtitle = "v1.0.0-alpha • 100% Native Kotlin & Jetpack Compose",
                        shape = shapeFor(0, 1),
                        onClick = {}
                    )
                }
            }

            item {
                val totalInvariants = 4
                SettingsSubsection(title = "Architectural Invariants") {
                    SettingsItem(
                        title = "1. Zero Backend, Zero Accounts",
                        subtitle = "No remote servers, no Supabase, no Firebase, and zero telemetry. Your device is the sovereign home of all data.",
                        shape = shapeFor(0, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "2. SQLite Truth & Export Bundle (ADR-0006)",
                        subtitle = "Room DB (SQLite + FTS5) provides zero-latency queries. Export bundles (JSON, Markdown, CSV) saved via SAF.",
                        shape = shapeFor(1, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "3. Extreme Speed (<50ms Cold Start)",
                        subtitle = "Ultra-lean capture tree, zero disk I/O on main thread during startup, and Baseline Profiles.",
                        shape = shapeFor(2, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Outlined.Speed, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "4. Strict Permission Discipline",
                        subtitle = "Never requests READ_CONTACTS or WRITE_CONTACTS. Foreground-only location strictly at instant of capture.",
                        shape = shapeFor(3, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )
                }
            }
        }
    }
}
