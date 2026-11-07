package com.example.hello.ui.edit.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hello.platform.avatar.AvatarBitmapCache
import com.example.hello.platform.avatar.AvatarCropGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
fun AvatarCropDialog(
    sourceUri: String,
    onConfirmCrop: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onDecodeFailed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var isDecoding by remember(sourceUri) { mutableStateOf(true) }
    var isProcessingCrop by remember { mutableStateOf(false) }

    var zoom by remember(sourceUri) { mutableFloatStateOf(1f) }
    var offsetX by remember(sourceUri) { mutableFloatStateOf(0f) }
    var offsetY by remember(sourceUri) { mutableFloatStateOf(0f) }
    var fitWholeImage by remember(sourceUri) { mutableStateOf(false) }
    var currentViewportPx by remember { mutableFloatStateOf(512f) }

    LaunchedEffect(sourceUri) {
        isDecoding = true
        val decoded = withContext(Dispatchers.IO) {
            AvatarBitmapCache.decodeSampledBitmap(context, sourceUri, maxDimension = 2048)
        }
        isDecoding = false
        if (decoded == null) {
            onDecodeFailed()
        } else {
            sourceBitmap = decoded
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessingCrop) onCancel() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isProcessingCrop,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1A1817)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCancel,
                        enabled = !isProcessingCrop
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel crop",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Crop Photo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            if (isProcessingCrop) return@Button
                            isProcessingCrop = true
                            scope.launch {
                                val pngBytes = withContext(Dispatchers.Default) {
                                    AvatarCropGeometry.renderCroppedAvatarPng(
                                        source = bmp,
                                        viewportSizePx = currentViewportPx,
                                        zoom = zoom,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        fitWholeImage = fitWholeImage
                                    )
                                }
                                isProcessingCrop = false
                                onConfirmCrop(pngBytes)
                            }
                        },
                        enabled = sourceBitmap != null && !isProcessingCrop,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center Interactive 1:1 Circular Crop Viewport
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val maxDimPx = with(density) {
                        min(maxWidth.toPx(), maxHeight.toPx()) * 0.82f
                    }
                    currentViewportPx = maxDimPx.coerceAtLeast(200f)

                    val bmp = sourceBitmap
                    if (isDecoding || bmp == null) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                        val borderColor = MaterialTheme.colorScheme.primary

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(bmp, fitWholeImage, currentViewportPx) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        val newZoom = (zoom * gestureZoom).coerceIn(
                                            AvatarCropGeometry.MIN_ZOOM,
                                            AvatarCropGeometry.MAX_ZOOM
                                        )
                                        val (clampedX, clampedY) = AvatarCropGeometry.clampPanOffset(
                                            offsetX = offsetX + pan.x,
                                            offsetY = offsetY + pan.y,
                                            imageWidth = bmp.width.toFloat(),
                                            imageHeight = bmp.height.toFloat(),
                                            viewportSize = currentViewportPx,
                                            zoom = newZoom,
                                            fitWholeImage = fitWholeImage
                                        )
                                        zoom = newZoom
                                        offsetX = clampedX
                                        offsetY = clampedY
                                    }
                                }
                        ) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = currentViewportPx / 2f

                            // 1. Draw Warm White (#FFFFFF) circle backing inside the crop aperture
                            drawCircle(
                                color = Color.White,
                                radius = radius,
                                center = center
                            )

                            // 2. Draw transformed source image
                            val baseScale = AvatarCropGeometry.calculateBaseScale(
                                imageWidth = bmp.width.toFloat(),
                                imageHeight = bmp.height.toFloat(),
                                viewportSize = currentViewportPx,
                                fitWholeImage = fitWholeImage
                            )
                            val totalScale = baseScale * zoom

                            withTransform({
                                translate(
                                    left = center.x + offsetX,
                                    top = center.y + offsetY
                                )
                                scale(
                                    scaleX = totalScale,
                                    scaleY = totalScale,
                                    pivot = Offset.Zero
                                )
                                translate(
                                    left = -bmp.width / 2f,
                                    top = -bmp.height / 2f
                                )
                            }) {
                                drawImage(image = imageBitmap)
                            }

                            // 3. Draw darkened outer scrim with circular 1:1 cutout
                            val scrimPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addOval(
                                    Rect(
                                        center.x - radius,
                                        center.y - radius,
                                        center.x + radius,
                                        center.y + radius
                                    )
                                )
                            }
                            drawPath(
                                path = scrimPath,
                                color = Color(0xCC1A1817)
                            )

                            // 4. Draw crisp circular crop guide ring
                            drawCircle(
                                color = borderColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Bottom Controls: Fit Whole Image Toggle & Reset Zoom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pinch to zoom • Drag to position",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (fitWholeImage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            },
                            modifier = Modifier.clickable {
                                val nextFit = !fitWholeImage
                                fitWholeImage = nextFit
                                val bmp = sourceBitmap
                                if (bmp != null) {
                                    val (cx, cy) = AvatarCropGeometry.clampPanOffset(
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        imageWidth = bmp.width.toFloat(),
                                        imageHeight = bmp.height.toFloat(),
                                        viewportSize = currentViewportPx,
                                        zoom = zoom,
                                        fitWholeImage = nextFit
                                    )
                                    offsetX = cx
                                    offsetY = cy
                                }
                            }
                        ) {
                            Text(
                                text = "Fit whole image",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (fitWholeImage) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (zoom > 1.01f || offsetX != 0f || offsetY != 0f) {
                            TextButton(
                                onClick = {
                                    zoom = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            ) {
                                Text(
                                    text = "Reset",
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

