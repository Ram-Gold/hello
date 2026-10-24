package com.example.hello.ui.quickaction.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.Circle
import com.example.hello.ui.quickaction.FlowAStep
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowAAddContactCard(
    uiState: QuickActionUiState,
    onEvent: (QuickActionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Sequence focus request after the sheet frame settles so the input renders before the keyboard rises
    LaunchedEffect(uiState.flowAStep) {
        if (uiState.flowAStep != FlowAStep.TAGS_AND_CIRCLES) {
            withFrameNanos { }
            if (uiState.flowAStep == FlowAStep.NAME) {
                delay(45)
            }
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // Ignore if composable is not focusable in preview/test
            }
        }
    }

    val onAdvanceOrSave = {
        if (uiState.flowAStep == FlowAStep.NOTE_AND_FOLLOW_UP) {
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
        // Minimal Top Header: [ Circular 'x' / '<-' ] + [ Centered Minimal Dots ] + [ Circular '->' ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Circular Close / Back Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (uiState.flowAStep == FlowAStep.NAME) {
                            onEvent(QuickActionUiEvent.OnDismiss)
                        } else {
                            onEvent(QuickActionUiEvent.OnPreviousStep)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.flowAStep == FlowAStep.NAME) {
                        Icons.Outlined.Close
                    } else {
                        Icons.AutoMirrored.Outlined.ArrowBack
                    },
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Centered Minimal Dots Progression
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowAStep.entries.forEach { step ->
                    val isCurrent = step == uiState.flowAStep
                    val isCompleted = step.stepNumber < uiState.flowAStep.stepNumber
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
                            .clickable { onEvent(QuickActionUiEvent.OnJumpToStep(step)) }
                    )
                }
            }

            // Right Circular '->' Arrow Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onAdvanceOrSave),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Minimal Step Prompt + Filled Input + Bordered/Selected Chips
        AnimatedContent(
            targetState = uiState.flowAStep,
            transitionSpec = {
                fadeIn(tween(110)) togetherWith fadeOut(tween(80))
            },
            label = "flowAStepTransition"
        ) { step ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                when (step) {
                    FlowAStep.NAME -> {
                        MinimalFilledTextField(
                            value = uiState.newContactName,
                            onValueChange = { onEvent(QuickActionUiEvent.OnNameChange(it)) },
                            placeholder = "Type a name...",
                            focusRequester = focusRequester,
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Words,
                            onImeAction = { onEvent(QuickActionUiEvent.OnNextStep) }
                        )
                    }

                    FlowAStep.PLACE -> {
                        MinimalFilledTextField(
                            value = uiState.customPlaceName,
                            onValueChange = { onEvent(QuickActionUiEvent.OnCustomPlaceNameChange(it)) },
                            placeholder = "Type a place...",
                            focusRequester = focusRequester,
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Words,
                            onImeAction = { onEvent(QuickActionUiEvent.OnNextStep) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinimalSelectableChip(
                                label = "📍 Current GPS",
                                selected = uiState.isLocationGranted && uiState.selectedPlaceId != null,
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

                    FlowAStep.TAGS_AND_CIRCLES -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Circle.entries.forEach { circle ->
                                val isSelected = circle in uiState.selectedCircles
                                MinimalSelectableChip(
                                    label = circle.displayName,
                                    selected = isSelected,
                                    onClick = { onEvent(QuickActionUiEvent.OnToggleCircle(circle)) }
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.suggestedTags.forEach { tag ->
                                val isSelected = tag.id in uiState.selectedTagIds
                                MinimalSelectableChip(
                                    label = "#${tag.name.lowercase()}",
                                    selected = isSelected,
                                    onClick = { onEvent(QuickActionUiEvent.OnToggleTag(tag.id)) }
                                )
                            }
                        }
                    }

                    FlowAStep.NOTE_AND_FOLLOW_UP -> {
                        MinimalFilledTextField(
                            value = uiState.encounterNote,
                            onValueChange = { onEvent(QuickActionUiEvent.OnNoteChange(it)) },
                            placeholder = "Write a quick note...",
                            focusRequester = focusRequester,
                            singleLine = false,
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences,
                            onImeAction = { onEvent(QuickActionUiEvent.OnQuickSaveFlowA) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val followUpOptions = listOf(
                                3 to "In 3 days",
                                7 to "Next week",
                                30 to "In 1 month"
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
    modifier: Modifier = Modifier
) {
    // MD3 Tonal Chip: Selected state uses matching containerColor and borderColor
    val selectedFillAndBorder = MaterialTheme.colorScheme.primaryContainer
    val containerColor = if (selected) {
        selectedFillAndBorder
    } else {
        Color.Transparent
    }
    val borderColor = if (selected) {
        selectedFillAndBorder
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = contentColor
        )
    }
}
