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
                        subtitle = "v1.0.0-alpha, Native Kotlin & Jetpack Compose",
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
                SettingsSubsection(title = "Principles") {
                    SettingsItem(
                        title = "1. Zero Backend, Zero Accounts",
                        subtitle = "No remote servers, no cloud accounts, and no telemetry. Everything stays on your device.",
                        shape = shapeFor(0, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "2. Local SQLite & Portable Exports",
                        subtitle = "Stored in local SQLite (Room + FTS5). Export your data anytime as JSON, Markdown, or CSV.",
                        shape = shapeFor(1, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "3. Fast Startup (<50ms Cold Start)",
                        subtitle = "Lean capture screen with no main-thread disk I/O during launch.",
                        shape = shapeFor(2, totalInvariants),
                        leadingIcon = {
                            Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {}
                    )

                    SettingsItem(
                        title = "4. Strict Permission Discipline",
                        subtitle = "Never reads your phone contacts. Location is foreground-only when logging a meeting.",
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
    val description: String,
    val licenseText: String
)

private const val APACHE_2_LICENSE = """Apache License, Version 2.0

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."""

private const val MIT_LICENSE = """MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT."""

private const val CC0_AND_MIT_LICENSE = """CC0 1.0 Universal (Public Domain Dedication) & MIT License

Original Notionists artwork by Zoish is dedicated to the public domain under CC0 1.0 Universal (no rights reserved).

DiceBear avatar style remix and HTTP API tooling are licensed under the MIT License (https://www.dicebear.com/styles/notionists/)."""

private const val KOBOYO_ICONS_LICENSE = """Koboyo Icons License (https://koboyo.com/icons/license)

Free for personal and commercial use in websites, apps, documents, presentations, and products, including recoloring, resizing, and modification. Attribution is not required (link to https://koboyo.com/icons appreciated).

Redistribution or resale as a standalone icon collection, or bundling into a competing icon library, canvas, or drawing tool, is not permitted."""

private const val SQLITE_PUBLIC_DOMAIN_AND_APACHE = """Apache License, Version 2.0 (AndroidX Room & SQLite wrappers) & Public Domain (SQLite)

The AndroidX Room and androidx.sqlite wrappers are licensed under the Apache License, Version 2.0.

The underlying SQLite C library (by D. Richard Hipp and SQLite contributors) has been dedicated to the public domain by its authors."""

private const val SIL_OFL_1_1_LICENSE = """SIL Open Font License, Version 1.1

Permission is hereby granted, free of charge, to any person obtaining a copy of the Font Software, to use, study, copy, merge, embed, modify, redistribute, and sell modified and unmodified copies of the Font Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components, in Original or Modified Versions, may be sold by itself.
2) Original or Modified Versions of the Font Software may be bundled, redistributed and/or sold with any software, provided that each copy contains the above copyright notice and this license.
3) No Modified Version of the Font Software may use the Reserved Font Name(s) unless explicit written permission is granted by the corresponding Copyright Holder.

THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED."""

private val ARTWORK_AND_ICON_NOTICES = listOf(
    OpenSourceNotice(
        name = "Notionists (via DiceBear)",
        author = "Zoish & DiceBear",
        licenseName = "CC0 1.0 & MIT",
        description = "Hand-drawn monochrome character illustrations used for opt-in contact avatars in the Avatar Playground.",
        licenseText = CC0_AND_MIT_LICENSE
    ),
    OpenSourceNotice(
        name = "Koboyo Icons",
        author = "Koboyo (koboyo.com/icons)",
        licenseName = "Koboyo Icons License",
        description = "Monochrome editorial vector illustrations used for empty states across the app.",
        licenseText = KOBOYO_ICONS_LICENSE
    ),
    OpenSourceNotice(
        name = "Feather Icons",
        author = "Cole Bemis & Contributors",
        licenseName = "MIT",
        description = "Minimalist 24x24 stroke icons bundled for core UI navigation, actions, and the default contact avatar icon.",
        licenseText = MIT_LICENSE
    )
)

private val FONT_AND_ASSET_NOTICES = listOf(
    OpenSourceNotice(
        name = "Nunito (Mallow alias)",
        author = "The Nunito Project Authors (Vernon Adams)",
        licenseName = "SIL OFL 1.1",
        description = "Rounded sans-serif typeface bundled for display, heading, and title typography (nunito_*.ttf and mallow_*.ttf).",
        licenseText = SIL_OFL_1_1_LICENSE
    ),
    OpenSourceNotice(
        name = "Open Sans (Vellum alias)",
        author = "The Open Sans Project Authors (Monotype Design Team)",
        licenseName = "SIL OFL 1.1",
        description = "Humanist sans-serif typeface bundled for body copy, captions, and UI labels (opensans_*.ttf and vellum_*.ttf).",
        licenseText = SIL_OFL_1_1_LICENSE
    )
)

private val RUNTIME_LIBRARY_NOTICES = listOf(
    OpenSourceNotice(
        name = "Kotlin Standard Library & Coroutines",
        author = "JetBrains s.r.o.",
        licenseName = "Apache 2.0",
        description = "Language runtime and structured concurrency (Flow, StateFlow, and background dispatchers).",
        licenseText = APACHE_2_LICENSE
    ),
    OpenSourceNotice(
        name = "Jetpack Compose & Material 3",
        author = "The Android Open Source Project",
        licenseName = "Apache 2.0",
        description = "Declarative UI toolkit, Material Design 3 components, Material Icons, and Activity Compose integration.",
        licenseText = APACHE_2_LICENSE
    ),
    OpenSourceNotice(
        name = "AndroidX Navigation Compose",
        author = "The Android Open Source Project",
        licenseName = "Apache 2.0",
        description = "In-app screen routing and back-stack management.",
        licenseText = APACHE_2_LICENSE
    ),
    OpenSourceNotice(
        name = "AndroidX Core, Splashscreen & Lifecycle",
        author = "The Android Open Source Project",
        licenseName = "Apache 2.0",
        description = "Kotlin Android extensions (core-ktx), system launch splash screen API, and lifecycle runtime.",
        licenseText = APACHE_2_LICENSE
    ),
    OpenSourceNotice(
        name = "AndroidX Room & Bundled SQLite",
        author = "The Android Open Source Project & SQLite Authors",
        licenseName = "Apache 2.0 & Public Domain",
        description = "Local SQLite + FTS5 persistence layer and bundled native SQLite database engine.",
        licenseText = SQLITE_PUBLIC_DOMAIN_AND_APACHE
    ),
    OpenSourceNotice(
        name = "AndroidX DataStore Preferences",
        author = "The Android Open Source Project",
        licenseName = "Apache 2.0",
        description = "Asynchronous local key-value storage for theme, terminology, and app preferences.",
        licenseText = APACHE_2_LICENSE
    ),
    OpenSourceNotice(
        name = "AndroidX Glance AppWidget",
        author = "The Android Open Source Project",
        licenseName = "Apache 2.0",
        description = "Compose-based home screen widget runtime powering the Quick Add widget.",
        licenseText = APACHE_2_LICENSE
    ),
    OpenSourceNotice(
        name = "AndroidX WorkManager",
        author = "The Android Open Source Project",
        licenseName = "Apache 2.0",
        description = "Background job execution for on-demand ZIP export bundle generation.",
        licenseText = APACHE_2_LICENSE
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
                SettingsSubsection(title = "Artwork & Icons") {
                    ARTWORK_AND_ICON_NOTICES.forEachIndexed { index, notice ->
                        SettingsItem(
                            title = notice.name,
                            subtitle = "${notice.author} (${notice.licenseName})",
                            shape = shapeFor(index, ARTWORK_AND_ICON_NOTICES.size),
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
                            subtitle = "${notice.author} (${notice.licenseName})",
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

            item {
                SettingsSubsection(title = "Runtime Libraries") {
                    RUNTIME_LIBRARY_NOTICES.forEachIndexed { index, notice ->
                        SettingsItem(
                            title = notice.name,
                            subtitle = "${notice.author} (${notice.licenseName})",
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
                        text = "${notice.author} (${notice.licenseName})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = notice.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
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
