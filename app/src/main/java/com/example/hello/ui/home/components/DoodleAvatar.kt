package com.example.hello.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.platform.avatar.AvatarBitmapCache
import com.example.hello.ui.components.CustomizableIcon

/**
 * Contact Avatar badge rendered across all surfaces (ADR-0022, ADR-0035).
 * Displays the Contact's local portrait photo or opt-in Notionist Avatar when [photoUri] is set,
 * and falls back to the uniform neutral minimalist user icon otherwise.
 */
@Composable
fun DoodleAvatar(
    initials: String = "",
    bgIndex: Int = 0,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    photoUri: String? = null
) {
    val context = LocalContext.current
    val targetMaxDimension = if (size <= 64.dp) {
        AvatarBitmapCache.THUMBNAIL_MAX_DIMENSION
    } else {
        AvatarBitmapCache.FULL_MAX_DIMENSION
    }
    var loadedBitmap by remember(photoUri, targetMaxDimension) {
        mutableStateOf(AvatarBitmapCache.get(photoUri, targetMaxDimension))
    }

    LaunchedEffect(photoUri, targetMaxDimension) {
        if (!photoUri.isNullOrBlank() && loadedBitmap == null) {
            loadedBitmap = AvatarBitmapCache.loadOrDecode(context, photoUri, targetMaxDimension)
        } else if (photoUri.isNullOrBlank()) {
            loadedBitmap = null
        }
    }

    val bgColor = if (loadedBitmap != null) {
        Color.White
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = loadedBitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CustomizableIcon(
                iconKey = HelloIconKey.USER,
                size = size * 0.48f,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
