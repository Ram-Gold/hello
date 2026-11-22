package com.example.hello.ui.onboarding

import android.content.res.Configuration
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag
import com.example.hello.domain.model.TagColorPalette
import com.example.hello.domain.model.extractAlphanumericInitials
import com.example.hello.ui.components.HelloMark
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.navigation.normalizePredictiveBackProgress
import com.example.hello.ui.theme.HelloTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val EditorialCardShape = RoundedCornerShape(8.dp)
private val EditorialButtonShape = RoundedCornerShape(12.dp)
private val StarterFirstConnectionTags = listOf("friends", "coffee", "work", "design")

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    val stepTransitionState = remember { SeekableTransitionState(uiState.currentStep) }

    LaunchedEffect(uiState.currentStep, isReducedMotion) {
        if (stepTransitionState.currentState != uiState.currentStep) {
            if (isReducedMotion) {
                stepTransitionState.snapTo(uiState.currentStep)
            } else {
                stepTransitionState.animateTo(uiState.currentStep)
            }
        }
    }

    val stepTransition = rememberTransition(stepTransitionState, label = "OnboardingStepTransition")

    PredictiveBackHandler(enabled = uiState.currentStep > 0) { progressFlow ->
        val prevStep = (uiState.currentStep - 1).coerceAtLeast(0)
        try {
            progressFlow.collect { backEvent ->
                val p = normalizePredictiveBackProgress(backEvent.progress, isReducedMotion)
                if (p > 0f) {
                    stepTransitionState.seekTo(fraction = p, targetState = prevStep)
                }
            }
            onEvent(OnboardingUiEvent.OnPreviousStep)
        } catch (e: CancellationException) {
            scope.launch {
                if (isReducedMotion) {
                    stepTransitionState.snapTo(uiState.currentStep)
                } else {
                    stepTransitionState.animateTo(uiState.currentStep)
                }
            }
            throw e
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OnboardingTopBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                onSkip = { onEvent(OnboardingUiEvent.OnSkipOnboarding(onFinish)) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                stepTransition.AnimatedContent(
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else if (targetState > initialState) {
                            slideInHorizontally { it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 3 } + fadeOut()
                        } else {
                            slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 3 } + fadeOut()
                        }
                    }
                ) { step ->
                    when (step) {
                        0 -> OnboardingWelcomeStep(modifier = Modifier.fillMaxSize())
                        1 -> OnboardingProfileStep(
                            userNameInput = uiState.userNameInput,
                            onNameChange = { onEvent(OnboardingUiEvent.OnUserNameChange(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                        2 -> OnboardingAppearanceStep(
                            selectedMode = uiState.selectedThemeMode,
                            onSelectMode = { onEvent(OnboardingUiEvent.OnSelectThemeMode(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                        3 -> OnboardingWeekStartStep(
                            selectedWeekStart = uiState.selectedFirstDayOfWeek,
                            onSelectWeekStart = { onEvent(OnboardingUiEvent.OnSelectFirstDayOfWeek(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                        4 -> OnboardingTerminologyStep(
                            selected = uiState.selectedTerminology,
                            onSelect = { onEvent(OnboardingUiEvent.OnSelectTerminology(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> OnboardingFirstConnectionStep(
                            name = uiState.firstConnectionName,
                            note = uiState.firstConnectionNote,
                            selectedTagNames = uiState.firstConnectionTagNames,
                            onNameChange = { onEvent(OnboardingUiEvent.OnFirstConnectionNameChange(it)) },
                            onNoteChange = { onEvent(OnboardingUiEvent.OnFirstConnectionNoteChange(it)) },
                            onToggleTag = { onEvent(OnboardingUiEvent.OnToggleFirstConnectionTag(it)) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            OnboardingBottomBar(
                uiState = uiState,
                onPrevious = { onEvent(OnboardingUiEvent.OnPreviousStep) },
                onNext = { onEvent(OnboardingUiEvent.OnNextStep) },
                onSkip = { onEvent(OnboardingUiEvent.OnSkipOnboarding(onFinish)) },
                onFinish = { onEvent(OnboardingUiEvent.OnFinishOnboarding(createFirstConnection = true, onFinished = onFinish)) }
            )
        }
    }
}

@Composable
private fun OnboardingTopBar(
    currentStep: Int,
    totalSteps: Int,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentStep) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        TextButton(onClick = onSkip) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    uiState: OnboardingUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            uiState.currentStep == 0 -> {
                Button(
                    onClick = onNext,
                    shape = EditorialButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            !uiState.isLastStep -> {
                OutlinedButton(
                    onClick = onPrevious,
                    shape = EditorialButtonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onNext,
                    shape = EditorialButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            else -> {
                OutlinedButton(
                    onClick = onSkip,
                    shape = EditorialButtonShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "Skip for now",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                val finishLabel = when {
                    uiState.firstConnectionName.isNotBlank() -> "Save & Start"
                    uiState.isReplay -> "Done"
                    else -> "Enter Hello"
                }

                Button(
                    onClick = onFinish,
                    shape = EditorialButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp)
                ) {
                    Text(
                        text = finishLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    centered: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
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

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            HelloMark(width = 64.dp)
        }

        StepHeader(
            eyebrow = "STEP 1 OF 6",
            title = "Welcome to Hello",
            subtitle = "A quiet, local-first home for the people you meet.",
            centered = true
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EditorialValueCard(
                icon = Icons.Rounded.Lock,
                title = "100% On-Device Storage",
                description = "No accounts, no cloud sync, and no tracking. Your device holds everything."
            )
            EditorialValueCard(
                icon = Icons.Rounded.Shield,
                title = "Private Permissions",
                description = "Never reads your phone contacts. Location is foreground-only when you choose to log it."
            )
        }
    }
}

@Composable
private fun EditorialValueCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        shape = EditorialCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, EditorialCardShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(EditorialCardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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

@Composable
private fun OnboardingProfileStep(
    userNameInput: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val greetingPrefix = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val previewName = userNameInput.trim().ifEmpty { "friend" }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        StepHeader(
            eyebrow = "STEP 2 OF 6",
            title = "What should we call you?",
            subtitle = "Used only for your home greeting on this device. Leave blank to skip."
        )

        OutlinedTextField(
            value = userNameInput,
            onValueChange = onNameChange,
            singleLine = true,
            shape = EditorialCardShape,
            label = { Text("Your name") },
            placeholder = { Text("e.g. Ram") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            shape = EditorialCardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, EditorialCardShape)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "HOME PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$greetingPrefix, $previewName",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OnboardingAppearanceStep(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val modes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        StepHeader(
            eyebrow = "STEP 3 OF 6",
            title = "Choose your look",
            subtitle = "Switch light, dark, or match your device. Preview updates immediately."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            modes.forEach { mode ->
                val isSelected = selectedMode == mode
                val cardShape = EditorialCardShape
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(cardShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = cardShape
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeArtworkSlot(
                        mode = mode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated visual preview slot for Light, Dark, and System theme cards.
 * Replace the placeholder monochrome frames here when custom artwork drawables are added.
 */
@Composable
private fun ThemeArtworkSlot(
    mode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val slotShape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(slotShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, slotShape)
    ) {
        when (mode) {
            ThemeMode.LIGHT -> MiniUiSurfacePreview(
                canvasColor = Color(0xFFFFFFFF),
                cardColor = Color(0xFFF3F3F5),
                inkColor = Color(0xFF121212),
                mutedInkColor = Color(0xFF8A8A8F),
                modifier = Modifier.fillMaxSize()
            )

            ThemeMode.DARK -> MiniUiSurfacePreview(
                canvasColor = Color(0xFF121212),
                cardColor = Color(0xFF222225),
                inkColor = Color(0xFFE4E4E7),
                mutedInkColor = Color(0xFF71717A),
                modifier = Modifier.fillMaxSize()
            )

            ThemeMode.SYSTEM -> Row(modifier = Modifier.fillMaxSize()) {
                MiniUiSurfacePreview(
                    canvasColor = Color(0xFFFFFFFF),
                    cardColor = Color(0xFFF3F3F5),
                    inkColor = Color(0xFF121212),
                    mutedInkColor = Color(0xFF8A8A8F),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                MiniUiSurfacePreview(
                    canvasColor = Color(0xFF121212),
                    cardColor = Color(0xFF222225),
                    inkColor = Color(0xFFE4E4E7),
                    mutedInkColor = Color(0xFF71717A),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun MiniUiSurfacePreview(
    canvasColor: Color,
    cardColor: Color,
    inkColor: Color,
    mutedInkColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(canvasColor)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(6.dp)
                .clip(CircleShape)
                .background(inkColor)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(cardColor)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(inkColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(inkColor.copy(alpha = 0.8f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(mutedInkColor)
            )
        }
    }
}

@Composable
private fun OnboardingWeekStartStep(
    selectedWeekStart: WeekStartDay,
    onSelectWeekStart: (WeekStartDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        StepHeader(
            eyebrow = "STEP 4 OF 6",
            title = "When does your week start?",
            subtitle = "Sets how calendars and weekly timelines align across Hello."
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WeekStartDay.entries.forEach { option ->
                OnboardingSelectableOptionCard(
                    title = option.title,
                    description = option.subtitle,
                    selected = selectedWeekStart == option,
                    onClick = { onSelectWeekStart(option) }
                )
            }
        }

        MiniMonthCalendarPreview(firstDayOfWeek = selectedWeekStart)
    }
}

@Composable
private fun MiniMonthCalendarPreview(
    firstDayOfWeek: WeekStartDay
) {
    val today = remember { LocalDate.now() }
    val month = remember(today) { YearMonth.from(today) }
    val monthLabel = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }
    val dayHeaders = if (firstDayOfWeek == WeekStartDay.MONDAY) {
        listOf("M", "T", "W", "T", "F", "S", "S")
    } else {
        listOf("S", "M", "T", "W", "T", "F", "S")
    }
    val rows = remember(month, firstDayOfWeek) {
        val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.dayOfWeek.value + 7) % 7
        val rawCells = List<Int?>(leadingBlanks) { null } + (1..month.lengthOfMonth()).toList()
        val trailingBlanks = (7 - (rawCells.size % 7)) % 7
        (rawCells + List<Int?>(trailingBlanks) { null }).chunked(7)
    }

    Surface(
        shape = EditorialCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, EditorialCardShape)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Starts ${firstDayOfWeek.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { initial ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            rows.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayNumber ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber != null) {
                                val isToday = dayNumber == today.dayOfMonth
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isToday) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
    val options = listOf(
        RelationshipTerminology.PEOPLE,
        RelationshipTerminology.FRIENDS,
        RelationshipTerminology.CONNECTIONS,
        RelationshipTerminology.CONTACTS
    )

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        StepHeader(
            eyebrow = "STEP 5 OF 6",
            title = "What do you call your directory?",
            subtitle = "Choose the label for your main tab. You can change this anytime in Settings."
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                OnboardingSelectableOptionCard(
                    title = option.pluralTitle,
                    description = option.description,
                    badgeText = if (option == RelationshipTerminology.PEOPLE) "Recommended" else null,
                    selected = selected == option,
                    onClick = { onSelect(option) }
                )
            }
        }

        MiniDockPreview(selected = selected)
    }
}

@Composable
private fun MiniDockPreview(selected: RelationshipTerminology) {
    Surface(
        shape = EditorialCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, EditorialCardShape)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "BOTTOM DOCK PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniDockTabItem(
                    icon = Icons.Rounded.Home,
                    label = "Home",
                    active = false
                )
                MiniDockTabItem(
                    icon = Icons.Rounded.People,
                    label = selected.pluralTitle,
                    active = true
                )
                MiniDockTabItem(
                    icon = Icons.Rounded.CalendarToday,
                    label = "Timeline",
                    active = false
                )
            }
        }
    }
}

@Composable
private fun MiniDockTabItem(
    icon: ImageVector,
    label: String,
    active: Boolean
) {
    val pillShape = CircleShape
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (active) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}

@Composable
private fun OnboardingSelectableOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    badgeText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EditorialCardShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = EditorialCardShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingFirstConnectionStep(
    name: String,
    note: String,
    selectedTagNames: Set<String>,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val previewName = name.trim().ifEmpty { "Your first person" }
    val previewSubtitle = note.trim().ifEmpty { "Met today" }

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        StepHeader(
            eyebrow = "STEP 6 OF 6",
            title = "Add your first connection",
            subtitle = "Start with someone you met recently, or skip and add later."
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            shape = EditorialCardShape,
            label = { Text("Name") },
            placeholder = { Text("e.g. Maya Lin") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            maxLines = 2,
            shape = EditorialCardShape,
            label = { Text("Where did you meet? (optional)") },
            placeholder = { Text("Coffee near the studio") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StarterFirstConnectionTags.forEach { tagName ->
                TonalTagPill(
                    tag = Tag(
                        id = "onboarding-$tagName",
                        name = tagName,
                        colorHex = TagColorPalette.getDeterministicColorHex(tagName)
                    ),
                    isSelected = tagName in selectedTagNames,
                    onClick = { onToggleTag(tagName) }
                )
            }
        }

        Surface(
            shape = EditorialCardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, EditorialCardShape)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CONTACT CARD PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DoodleAvatar(
                        initials = extractAlphanumericInitials(previewName),
                        bgIndex = 0,
                        size = 40.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = previewName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = previewSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (selectedTagNames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StarterFirstConnectionTags
                            .filter { it in selectedTagNames }
                            .forEach { tagName ->
                                TonalTagPill(
                                    tag = Tag(
                                        id = "preview-$tagName",
                                        name = tagName,
                                        colorHex = TagColorPalette.getDeterministicColorHex(tagName)
                                    )
                                )
                            }
                    }
                }
            }
        }
    }
}

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

@Preview(
    name = "Onboarding Appearance Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun OnboardingAppearanceDarkPreview() {
    HelloTheme(darkTheme = true) {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 2, selectedThemeMode = ThemeMode.DARK),
            onEvent = {},
            onFinish = {}
        )
    }
}

@Preview(name = "Onboarding Week Start Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingWeekStartLightPreview() {
    HelloTheme(darkTheme = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 3, selectedFirstDayOfWeek = WeekStartDay.SUNDAY),
            onEvent = {},
            onFinish = {}
        )
    }
}

@Preview(name = "Onboarding First Connection Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun OnboardingFirstConnectionLightPreview() {
    HelloTheme(darkTheme = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentStep = 5,
                firstConnectionName = "Maya Lin",
                firstConnectionNote = "Met at Kissa Coffee",
                firstConnectionTagNames = setOf("friends", "coffee")
            ),
            onEvent = {},
            onFinish = {}
        )
    }
}
