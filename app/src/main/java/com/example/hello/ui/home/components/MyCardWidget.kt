package com.example.hello.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.CardType
import com.example.hello.domain.model.MyCardData

@Composable
fun MyCardWidget(
    cardData: MyCardData,
    activeCardType: CardType,
    onCardTypeSelect: (CardType) -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(22.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header: Card Type Switcher & Expand Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardTypeSelector(
                        selectedType = activeCardType,
                        onSelect = onCardTypeSelect
                    )

                    IconButton(
                        onClick = onExpandClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fullscreen,
                            contentDescription = "Expand QR Code",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Middle: User Info & Simulated QR Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = cardData.userName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = cardData.headline,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = cardData.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = cardData.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Simulated QR Matrix Container
                    SimulatedQrContainer(
                        onClick = onExpandClick,
                        modifier = Modifier.size(86.dp)
                    )
                }

                // Bottom Tag Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = cardData.circleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTypeSelector(
    selectedType: CardType,
    onSelect: (CardType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CardType.entries.forEach { type ->
            val isSelected = type == selectedType
            val bg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(200),
                label = "cardTypeBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(200),
                label = "cardTypeText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${type.iconEmoji} ${type.title}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun SimulatedQrContainer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matrixColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(68.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val dotCount = 7
            val cellSize = canvasWidth / dotCount

            // Finder patterns (top-left, top-right, bottom-left)
            val finderSize = cellSize * 2.2f
            drawRoundRect(
                color = matrixColor,
                topLeft = Offset(0f, 0f),
                size = Size(finderSize, finderSize),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cellSize * 0.4f, cellSize * 0.4f),
                size = Size(finderSize - cellSize * 0.8f, finderSize - cellSize * 0.8f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawCircle(
                color = matrixColor,
                radius = cellSize * 0.4f,
                center = Offset(finderSize / 2f, finderSize / 2f)
            )

            // Top-right finder
            val trX = canvasWidth - finderSize
            drawRoundRect(
                color = matrixColor,
                topLeft = Offset(trX, 0f),
                size = Size(finderSize, finderSize),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(trX + cellSize * 0.4f, cellSize * 0.4f),
                size = Size(finderSize - cellSize * 0.8f, finderSize - cellSize * 0.8f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawCircle(
                color = matrixColor,
                radius = cellSize * 0.4f,
                center = Offset(trX + finderSize / 2f, finderSize / 2f)
            )

            // Bottom-left finder
            val blY = canvasHeight - finderSize
            drawRoundRect(
                color = matrixColor,
                topLeft = Offset(0f, blY),
                size = Size(finderSize, finderSize),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cellSize * 0.4f, blY + cellSize * 0.4f),
                size = Size(finderSize - cellSize * 0.8f, finderSize - cellSize * 0.8f),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawCircle(
                color = matrixColor,
                radius = cellSize * 0.4f,
                center = Offset(finderSize / 2f, blY + finderSize / 2f)
            )

            // Decorative QR dots in the remaining matrix
            val samplePoints = listOf(
                Pair(3, 1), Pair(4, 2), Pair(3, 3), Pair(4, 4),
                Pair(1, 4), Pair(2, 4), Pair(5, 3), Pair(3, 5),
                Pair(4, 5), Pair(5, 5), Pair(6, 4), Pair(5, 6)
            )

            samplePoints.forEach { (col, row) ->
                drawRoundRect(
                    color = matrixColor,
                    topLeft = Offset(col * cellSize + 2f, row * cellSize + 2f),
                    size = Size(cellSize - 4f, cellSize - 4f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}
