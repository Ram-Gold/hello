package com.example.hello.ui.quickaction.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.example.hello.ui.components.BirthdayInputUtils
import com.example.hello.ui.components.HelloDatePickerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.components.TonalFilterPill
import com.example.hello.ui.components.TonalTagPill
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FlowAAddContactCard(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    predictiveBackProgress: Float = 0f
) {
    val isReducedMotion = LocalReducedMotion.current
    val focusRequester = remember { FocusRequester() }
    val activeComponents = uiState.activeFlowPreset?.activeComponents.orEmpty()
    val currentStep = uiState.currentStepIndex.coerceIn(0, (activeComponents.size - 1).coerceAtLeast(0))
    val currentComp = activeComponents.getOrNull(currentStep)
    val prevComp = if (currentStep > 0) activeComponents.getOrNull(currentStep - 1) else null
    val isLastStep = currentStep >= activeComponents.size - 1

    val stepTransitionState = remember { SeekableTransitionState(currentComp) }
    LaunchedEffect(currentComp, predictiveBackProgress, isReducedMotion) {
        if (predictiveBackProgress > 0f && prevComp != null && !isReducedMotion) {
            stepTransitionState.seekTo(fraction = predictiveBackProgress.coerceIn(0f, 1f), targetState = prevComp)
        } else if (stepTransitionState.currentState != currentComp || stepTransitionState.targetState != currentComp) {
            if (isReducedMotion) {
                stepTransitionState.snapTo(currentComp)
            } else {
                stepTransitionState.animateTo(currentComp)
            }
        }
    }
    val stepTransition = rememberTransition(stepTransitionState, label = "modularStepTransition")

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        HelloDatePickerDialog(
            initialDate = uiState.birthday,
            onConfirm = { picked ->
                onEvent(QuickActionUiEvent.OnBirthdayChange(picked))
            },
            onDismissRequest = { showDatePicker = false },
            restrictToPastDates = true
        )
    }

    // Sequence focus request after the frame settles to avoid IME flicker
    LaunchedEffect(currentStep, currentComp?.type) {
        val type = currentComp?.type
        if (type != CaptureComponentType.CADENCE && type != CaptureComponentType.BIRTHDAY) {
            withFrameNanos { }
            if (type == CaptureComponentType.NAME) {
                delay(45)
            }
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // Ignore if not focusable in test
            }
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val onAdvanceOrSave = {
        if (isLastStep) {
            onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
        } else {
            onEvent(QuickActionUiEvent.OnNextStep)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header: [ Back / Close ] + [ Dots Progression ] + [ Escape Hatch / Next ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Dismiss at step 0, Back otherwise
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (currentStep == 0) {
                            onEvent(QuickActionUiEvent.OnDismiss)
                        } else {
                            onEvent(QuickActionUiEvent.OnPreviousStep)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (currentStep == 0) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = if (currentStep == 0) "Close" else "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Centered Dynamic Dots Progression
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeComponents.forEachIndexed { index, _ ->
                    val isCurrent = index == currentStep
                    val isCompleted = index < currentStep
                    val dotWidth by animateDpAsState(
                        targetValue = if (isCurrent) 18.dp else 6.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
                        label = "stepDotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                        animationSpec = tween(150),
                        label = "stepDotColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable { onEvent(QuickActionUiEvent.OnStepIndexChange(index)) }
                    )
                }
            }

            // Right Button: Checkmark on final step, Forward arrow otherwise
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onAdvanceOrSave),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLastStep) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = if (isLastStep) "Save" else "Next",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Modular Step Card
        stepTransition.AnimatedContent(
            transitionSpec = {
                if (isReducedMotion) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    fadeIn(tween(110)) togetherWith fadeOut(tween(80))
                }
            }
        ) { comp ->
            if (comp != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = comp.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    when (comp.type) {
                        CaptureComponentType.NAME -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MinimalFilledTextField(
                                    value = uiState.newContactName,
                                    onValueChange = { onEvent(QuickActionUiEvent.OnNameChange(it)) },
                                    placeholder = "Type a name...",
                                    focusRequester = focusRequester,
                                    imeAction = if (isLastStep) ImeAction.Done else ImeAction.Next,
                                    capitalization = KeyboardCapitalization.Words,
                                    onImeAction = onAdvanceOrSave
                                )
                                if (uiState.nameError) {
                                    Text(
                                        text = "Name is required to save",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        CaptureComponentType.ROLE -> {
                            MinimalFilledTextField(
                                value = uiState.role,
                                onValueChange = { onEvent(QuickActionUiEvent.OnRoleChange(it)) },
                                placeholder = "e.g. Lead Designer, Founder...",
                                focusRequester = focusRequester,
                                imeAction = if (isLastStep) ImeAction.Done else ImeAction.Next,
                                capitalization = KeyboardCapitalization.Words,
                                onImeAction = onAdvanceOrSave
                            )
                        }

                        CaptureComponentType.COMPANY -> {
                            MinimalFilledTextField(
                                value = uiState.company,
                                onValueChange = { onEvent(QuickActionUiEvent.OnCompanyChange(it)) },
                                placeholder = "e.g. Acme Labs, Stanford...",
                                focusRequester = focusRequester,
                                imeAction = if (isLastStep) ImeAction.Done else ImeAction.Next,
                                capitalization = KeyboardCapitalization.Words,
                                onImeAction = onAdvanceOrSave
                            )
                        }

                        CaptureComponentType.PLACE -> {
                            MinimalFilledTextField(
                                value = uiState.customPlaceName,
                                onValueChange = { onEvent(QuickActionUiEvent.OnCustomPlaceNameChange(it)) },
                                placeholder = "Type a place...",
                                focusRequester = focusRequester,
                                imeAction = if (isLastStep) ImeAction.Done else ImeAction.Next,
                                capitalization = KeyboardCapitalization.Words,
                                onImeAction = onAdvanceOrSave
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MinimalSelectableChip(
                                    label = when {
                                        uiState.isLocatingGps -> "Locating..."
                                        uiState.capturedLocationLabel != null -> uiState.capturedLocationLabel
                                        else -> "Current GPS"
                                    },
                                    leadingIconKey = HelloIconKey.MAP_PIN,
                                    selected = uiState.isLocatingGps || (uiState.capturedLatitude != null && uiState.capturedLongitude != null),
                                    onClick = { onEvent(QuickActionUiEvent.OnRequestLocation) }
                                )

                                uiState.suggestedPlaces.forEach { place ->
                                    val isSelected = uiState.selectedPlaceId == place.id
                                    MinimalSelectableChip(
                                        label = place.label,
                                        selected = isSelected,
                                        onClick = { onEvent(QuickActionUiEvent.OnSelectPlaceChip(place.id)) }
                                    )
                                }
                            }
                        }

                        CaptureComponentType.TAGS -> {
                            MinimalFilledTextField(
                                value = uiState.customTagInput,
                                onValueChange = { onEvent(QuickActionUiEvent.OnCustomTagInputChange(it)) },
                                placeholder = "Type a tag...",
                                focusRequester = focusRequester,
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Words,
                                onImeAction = { onEvent(QuickActionUiEvent.OnAddCustomTag) }
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.suggestedTags.forEach { tag ->
                                    val isSelected = tag.id in uiState.selectedTagIds
                                    TonalTagPill(
                                        tag = tag,
                                        isSelected = isSelected,
                                        onClick = { onEvent(QuickActionUiEvent.OnToggleTag(tag.id)) }
                                    )
                                }
                            }
                        }

                        CaptureComponentType.EMAIL -> {
                            MinimalFilledTextField(
                                value = uiState.email,
                                onValueChange = { onEvent(QuickActionUiEvent.OnEmailChange(it)) },
                                placeholder = "name@example.com",
                                focusRequester = focusRequester,
                                imeAction = if (isLastStep) ImeAction.Done else ImeAction.Next,
                                keyboardType = KeyboardType.Email,
                                onImeAction = onAdvanceOrSave
                            )
                        }

                        CaptureComponentType.PHONE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MinimalFilledTextField(
                                    value = uiState.phone,
                                    onValueChange = { onEvent(QuickActionUiEvent.OnPhoneChange(it)) },
                                    placeholder = "+1 555 123 4567",
                                    focusRequester = focusRequester,
                                    imeAction = if (isLastStep) ImeAction.Done else ImeAction.Next,
                                    keyboardType = KeyboardType.Phone,
                                    onImeAction = onAdvanceOrSave
                                )
                                uiState.phoneError?.let { errorText ->
                                    Text(
                                        text = errorText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        CaptureComponentType.BIRTHDAY -> {
                            val displayDate = BirthdayInputUtils.formatHumanReadable(uiState.birthday)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (uiState.birthday != null) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                            showDatePicker = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Cake,
                                                contentDescription = null,
                                                tint = if (uiState.birthday != null) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = if (displayDate.isNotBlank()) displayDate else "Select Birthday from Calendar",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = if (displayDate.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (uiState.birthday != null) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        if (uiState.birthday != null) {
                                            IconButton(
                                                onClick = { onEvent(QuickActionUiEvent.OnBirthdayChange(null)) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Clear birthday",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "Tap to choose a birth date from calendar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        CaptureComponentType.CADENCE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val followUpOptions = listOf(
                                    3 to "In 3 days",
                                    7 to "Next week",
                                    30 to "In 1 month",
                                    90 to "In 3 months"
                                )
                                followUpOptions.forEach { (days, label) ->
                                    val isSelected = uiState.selectedFollowUpDays == days
                                    MinimalSelectableChip(
                                        label = label,
                                        selected = isSelected,
                                        onClick = { onEvent(QuickActionUiEvent.OnSelectFollowUp(days)) }
                                    )
                                }
                            }
                        }

                        CaptureComponentType.CUSTOM -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        MinimalFilledTextField(
                                            value = uiState.customPropertyKey,
                                            onValueChange = { onEvent(QuickActionUiEvent.OnCustomPropertyKeyChange(it)) },
                                            placeholder = "Property (e.g. Coffee)",
                                            focusRequester = focusRequester,
                                            imeAction = ImeAction.Next
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        MinimalFilledTextField(
                                            value = uiState.customPropertyValue,
                                            onValueChange = { onEvent(QuickActionUiEvent.OnCustomPropertyValueChange(it)) },
                                            placeholder = "Value (e.g. Flat White)",
                                            imeAction = ImeAction.Done,
                                            onImeAction = { onEvent(QuickActionUiEvent.OnAddCustomProperty) }
                                        )
                                    }
                                }

                                if (uiState.customProperties.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        uiState.customProperties.forEach { (key, value) ->
                                            MinimalSelectableChip(
                                                label = "$key: $value ×",
                                                selected = true,
                                                onClick = { onEvent(QuickActionUiEvent.OnRemoveCustomProperty(key)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        CaptureComponentType.NOTE -> {
                            MinimalFilledTextField(
                                value = uiState.encounterNote,
                                onValueChange = { onEvent(QuickActionUiEvent.OnNoteChange(it)) },
                                placeholder = "Write a quick note...",
                                focusRequester = focusRequester,
                                singleLine = false,
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences,
                                onImeAction = onAdvanceOrSave
                            )
                        }
                    }
                }
            }
        }

        // Pinned Tag Chip Strip: Top Tags + "+ tag" inline custom entry
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentComp?.type != CaptureComponentType.TAGS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.suggestedTags.forEach { tag ->
                        val isSelected = tag.id in uiState.selectedTagIds
                        TonalTagPill(
                            tag = tag,
                            isSelected = isSelected,
                            onClick = { onEvent(QuickActionUiEvent.OnToggleTag(tag.id)) }
                        )
                    }
                    MinimalSelectableChip(
                        label = if (uiState.isCustomTagInputExpanded) "× tag" else "+ tag",
                        selected = uiState.isCustomTagInputExpanded,
                        onClick = { onEvent(QuickActionUiEvent.OnToggleCustomTagInput) }
                    )
                }

                if (uiState.isCustomTagInputExpanded) {
                    MinimalFilledTextField(
                        value = uiState.customTagInput,
                        onValueChange = { onEvent(QuickActionUiEvent.OnCustomTagInputChange(it)) },
                        placeholder = "Add custom tag...",
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.None,
                        onImeAction = { onEvent(QuickActionUiEvent.OnAddCustomTag) }
                    )
                }
            }

            // Instant Save Escape Hatch when Name is entered (allows <3s 1-step save)
            if (uiState.newContactName.isNotBlank() && !isLastStep) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onEvent(QuickActionUiEvent.OnQuickSaveFlowA) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save ${uiState.newContactName.trim()} Now",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
internal fun MinimalFilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
    onImeAction: () -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
            onSearch = { onImeAction() }
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
internal fun MinimalSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIconKey: HelloIconKey? = null
) {
    TonalFilterPill(
        label = label,
        isSelected = selected,
        unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = if (leadingIconKey != null) {
            { tintColor ->
                CustomizableIcon(
                    iconKey = leadingIconKey,
                    size = 14.dp,
                    tint = tintColor
                )
            }
        } else {
            null
        }
    )
}
