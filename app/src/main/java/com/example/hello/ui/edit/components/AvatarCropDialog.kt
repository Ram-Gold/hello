package com.example.hello.ui.edit.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hello.platform.avatar.AvatarBitmapCache
import com.example.hello.platform.avatar.AvatarCropGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private val DialogDarkSurface = Color(0xFF1A1817)

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

    val isResetActive = zoom > 1.01f || offsetX != 0f || offsetY != 0f || fitWholeImage

    fun resetTransform() {
        zoom = 1f
        offsetX = 0f
        offsetY = 0f
        fitWholeImage = false
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DialogDarkSurface)
        ) {
            // 1. Top Action Header (anchored at top with solid background above canvas)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DialogDarkSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
            }

            // 2. Center Interactive 1:1 Circular Crop Viewport (contained & clipped)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val maxDimPx = with(density) {
                    min(maxWidth.toPx(), maxHeight.toPx()) * 0.85f
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

                        val circlePath = Path().apply {
                            addOval(
                                Rect(
                                    center.x - radius,
                                    center.y - radius,
                                    center.x + radius,
                                    center.y + radius
                                )
                            )
                        }

                        // Strictly clip drawing to circular aperture so photo never renders outside circle
                        clipPath(circlePath) {
                            // 1. Draw solid Warm White (#FFFFFF) circular backing
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
                        }

                        // 3. Draw circular crop guide ring
                        drawCircle(
                            color = borderColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // 3. Bottom Controls Bar (anchored at bottom with solid background above canvas)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DialogDarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pinch to zoom • Drag to position",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Fit whole image" pill toggle
                        CropPillButton(
                            text = "Fit whole image",
                            active = fitWholeImage,
                            enabled = true,
                            onClick = {
                                fitWholeImage = !fitWholeImage
                                zoom = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )

                        // "Reset" pill button (permanent, active vs inactive states)
                        CropPillButton(
                            text = "Reset",
                            active = false,
                            enabled = isResetActive,
                            onClick = { resetTransform() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CropPillButton(
    text: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        active -> MaterialTheme.colorScheme.primary
        enabled -> Color.White.copy(alpha = 0.18f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val contentColor = when {
        active -> MaterialTheme.colorScheme.onPrimary
        enabled -> Color.White
        else -> Color.White.copy(alpha = 0.35f)
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1817)
@Composable
private fun CropPillButtonPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CropPillButton(text = "Fit whole image", active = true, enabled = true, onClick = {})
        CropPillButton(text = "Reset", active = false, enabled = true, onClick = {})
        CropPillButton(text = "Reset", active = false, enabled = false, onClick = {})
    }
}


