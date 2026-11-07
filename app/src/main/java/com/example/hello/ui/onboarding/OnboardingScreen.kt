package com.example.hello.ui.onboarding

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.TagColorPalette
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.FeatherIconSize
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.theme.HelloTheme
import com.example.hello.ui.theme.MotionTokens

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept system Back when on Step 1 or 2 to retreat linearly
    BackHandler(enabled = uiState.currentStep > 0) {
        onEvent(OnboardingUiEvent.OnPreviousStep)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Step Progress Indicator (Dots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val isActive = uiState.currentStep == index
                    val isPast = uiState.currentStep > index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isActive) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> MaterialTheme.colorScheme.primary
                                    isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                    )
                }
            }

            // Step Content with directional slide transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 3 } + fadeOut()
                        } else {
                            slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 3 } + fadeOut()
                        }
                    },
                    label = "OnboardingStepContent"
                ) { step ->
                    when (step) {
                        0 -> OnboardingWelcomeStep(modifier = Modifier.fillMaxSize())
                        1 -> OnboardingTerminologyStep(
                            selected = uiState.selectedTerminology,
                            onSelect = { onEvent(OnboardingUiEvent.OnSelectTerminology(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                        2 -> OnboardingLocationStep(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.currentStep > 0) {
                    OutlinedButton(
                        onClick = { onEvent(OnboardingUiEvent.OnPreviousStep) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Back", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Button(
                    onClick = {
                        if (uiState.currentStep < 2) {
                            onEvent(OnboardingUiEvent.OnNextStep)
                        } else {
                            onEvent(OnboardingUiEvent.OnFinishOnboarding(onFinish))
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(if (uiState.currentStep > 0) 1.5f else 1f)
                        .height(52.dp)
                ) {
                    if (uiState.currentStep >= 2) {
                        CustomizableIcon(
                            iconKey = HelloIconKey.CHECK,
                            size = FeatherIconSize.INLINE,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (uiState.currentStep < 2) "Continue" else "Enter Hello",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Hero Greeting Doodle
        DoodleAvatar(
            initials = "",
            bgIndex = 0,
            size = 88.dp,
            shape = RoundedCornerShape(28.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Welcome to Hello",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your local-first personal relationship keeper.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Value Proposition Pillars
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ValuePropCard(
                icon = Icons.Rounded.FlashOn,
                title = "Extreme Capture Speed (<50ms)",
                description = "Record quick notes, encounters, and places before the context fades."
            )
            ValuePropCard(
                icon = Icons.Rounded.Lock,
                title = "100% Local-First Sovereignty",
                description = "No accounts, no cloud sync, no tracking. Your device is the single home of all data."
            )
            ValuePropCard(
                icon = Icons.Rounded.Shield,
                title = "Strict Permission Discipline",
                description = "We never harvest your system contacts and never track your location in the background."
            )
        }
    }
}

@Composable
private fun OnboardingTerminologyStep(
    selected: RelationshipTerminology,
    onSelect: (RelationshipTerminology) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "What should we call your directory?",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Choose a universal container for everyone in your life. You'll use flexible #tags (#friends, #work, #family) for how you know each person—so your labels never collide.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RelationshipTerminology.primaryDirectoryOptions.forEach { option ->
                val isSelected = selected == option
                val isRecommended = option == RelationshipTerminology.PEOPLE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelect(option) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = option.pluralTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isRecommended) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "Recommended",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = option.addActionTitle(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Two-Layer Taxonomy Preview Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "HOW YOUR DIRECTORY IS ORGANIZED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layer 1 • Directory Tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "All ${selected.pluralTitle}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layer 2 • Context Pills",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RelationshipTerminology.coreSphereTags.forEach { sphere ->
                            val color = TagColorPalette.getDeterministicColorHex(sphere)
                            TonalTagPill(
                                tag = Tag(id = "sphere-$sphere", name = sphere, colorHex = color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingLocationStep(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Foreground-Only Location",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Why Hello asks for location only when you capture an encounter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ValuePropCard(
                icon = Icons.Rounded.Shield,
                title = "Never Runs in Background",
                description = "Hello never tracks where you go or keeps running location services while your phone is asleep."
            )
            ValuePropCard(
                icon = Icons.Rounded.LocationOn,
                title = "Instant Snapshot Only",
                description = "GPS coordinates are requested strictly when you tap 'Near Current GPS' while recording an encounter."
            )
            ValuePropCard(
                icon = Icons.Rounded.Lock,
                title = "Local & Completely Optional",
                description = "Place names and coordinates never leave your device. Denying location never prevents logging encounters."
            )
        }
    }
}

@Composable
private fun ValuePropCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Previews
// -------------------------------------------------------------

@Preview(name = "Onboarding Welcome Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingWelcomeLightPreview() {
    HelloTheme(darkTheme = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 0),
            onEvent = {},
            onFinish = {}
        )
    }
}

@Preview(name = "Onboarding Terminology Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingTerminologyLightPreview() {
    HelloTheme(darkTheme = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 1, selectedTerminology = RelationshipTerminology.FRIENDS),
            onEvent = {},
            onFinish = {}
        )
    }
}

@Preview(name = "Onboarding Location Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingLocationLightPreview() {
    HelloTheme(darkTheme = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 2),
            onEvent = {},
            onFinish = {}
        )
    }
}
