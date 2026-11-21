package com.example.hello.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hello.ui.components.HelloHorizontalLogo

@Composable
fun SettingsAboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HelloHorizontalLogo(width = 180.dp)
                }
            }

            item {
                SettingsSubsection(title = "Application") {
                    SettingsItem(
                        title = "Hello Personal CRM",
                        subtitle = "v1.0.0-alpha • 100% Native Kotlin & Jetpack Compose",
                        shape = shapeFor(0, 2),
                        onClick = {}
                    )
                    SettingsItem(
                        title = "Open-Source Licenses",
                        subtitle = "Third-party libraries and bundled typefaces",
                        shape = shapeFor(1, 2),
                        leadingIcon = {
                            Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToLicenses
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
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "2. SQLite Truth & Export Bundle (ADR-0006)",
                        subtitle = "Room DB (SQLite + FTS5) provides zero-latency queries. Export bundles (JSON, Markdown, CSV) saved via SAF.",
                        shape = shapeFor(1, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "3. Extreme Speed (<50ms Cold Start)",
                        subtitle = "Ultra-lean capture tree, zero disk I/O on main thread during startup, and Baseline Profiles.",
                        shape = shapeFor(2, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "4. Strict Permission Discipline",
                        subtitle = "Never requests READ_CONTACTS or WRITE_CONTACTS. Foreground-only location strictly at instant of capture.",
                        shape = shapeFor(3, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )
                }
            }
        }
    }
}

private data class OpenSourceNotice(
    val name: String,
    val author: String,
    val licenseName: String,
    val licenseText: String
)

private const val APACHE_2_LICENSE = """Apache License, Version 2.0

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."""

private const val SIL_OFL_1_1_LICENSE = """SIL Open Font License, Version 1.1

Permission is hereby granted, free of charge, to any person obtaining a copy of the Font Software, to use, study, copy, merge, embed, modify, redistribute, and sell modified and unmodified copies of the Font Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components, in Original or Modified Versions, may be sold by itself.
2) Original or Modified Versions of the Font Software may be bundled, redistributed and/or sold with any software, provided that each copy contains the above copyright notice and this license.
3) No Modified Version of the Font Software may use the Reserved Font Name(s) unless explicit written permission is granted by the corresponding Copyright Holder.

THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED."""

private val RUNTIME_LIBRARY_NOTICES = listOf(
    OpenSourceNotice("Kotlin Standard Library & Coroutines", "JetBrains s.r.o.", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("Jetpack Compose & Material 3", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("AndroidX Navigation Compose", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("AndroidX Core KTX, Splashscreen & Lifecycle", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("AndroidX Room & Bundled SQLite", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("AndroidX DataStore Preferences", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("AndroidX Glance AppWidget", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE),
    OpenSourceNotice("AndroidX WorkManager", "The Android Open Source Project", "Apache 2.0", APACHE_2_LICENSE)
)

private val FONT_AND_ASSET_NOTICES = listOf(
    OpenSourceNotice("Nunito", "Vernon Adams, Cyreal, Jacques Le Bailly", "SIL OFL 1.1", SIL_OFL_1_1_LICENSE),
    OpenSourceNotice("Open Sans", "Steve Matteson", "SIL OFL 1.1", SIL_OFL_1_1_LICENSE),
    OpenSourceNotice(
        "Mallow & Vellum",
        "Bundled Typeface Assets",
        "Bundled Asset",
        "Bundled typeface assets used for editorial headings (Mallow) and body/caption typography (Vellum)."
    )
)

@Composable
fun SettingsLicensesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNotice by remember { mutableStateOf<OpenSourceNotice?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PixelStyleTopBar(
                title = "Open-Source Licenses",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSubsection(title = "Runtime Libraries") {
                    RUNTIME_LIBRARY_NOTICES.forEachIndexed { index, notice ->
                        SettingsItem(
                            title = notice.name,
                            subtitle = "${notice.author} • ${notice.licenseName}",
                            shape = shapeFor(index, RUNTIME_LIBRARY_NOTICES.size),
                            trailingContent = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { selectedNotice = notice }
                        )
                    }
                }
            }

            item {
                SettingsSubsection(title = "Fonts & Typefaces") {
                    FONT_AND_ASSET_NOTICES.forEachIndexed { index, notice ->
                        SettingsItem(
                            title = notice.name,
                            subtitle = "${notice.author} • ${notice.licenseName}",
                            shape = shapeFor(index, FONT_AND_ASSET_NOTICES.size),
                            trailingContent = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { selectedNotice = notice }
                        )
                    }
                }
            }
        }
    }

    selectedNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { selectedNotice = null },
            title = {
                Text(
                    text = notice.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${notice.author} • ${notice.licenseName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = notice.licenseText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedNotice = null }) {
                    Text("Close")
                }
            }
        )
    }
}
