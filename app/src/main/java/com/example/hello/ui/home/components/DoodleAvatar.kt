package com.example.hello.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.HelloIconKey
import com.example.hello.platform.avatar.AvatarBitmapCache
import com.example.hello.ui.components.CustomizableIcon
import com.example.hello.ui.theme.AvatarAmber
import com.example.hello.ui.theme.AvatarLavender
import com.example.hello.ui.theme.AvatarPeach
import com.example.hello.ui.theme.AvatarRose
import com.example.hello.ui.theme.AvatarSage
import com.example.hello.ui.theme.AvatarSky

private val AvatarColors = listOf(
    AvatarPeach,
    AvatarAmber,
    AvatarSage,
    AvatarSky,
    AvatarRose,
    AvatarLavender
)

@Composable
fun DoodleAvatar(
    initials: String,
    bgIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    photoUri: String? = null
) {
    val context = LocalContext.current
    var loadedBitmap by remember(photoUri) {
        mutableStateOf(AvatarBitmapCache.get(photoUri))
    }

    LaunchedEffect(photoUri) {
        if (!photoUri.isNullOrBlank() && loadedBitmap == null) {
            loadedBitmap = AvatarBitmapCache.loadOrDecode(context, photoUri)
        } else if (photoUri.isNullOrBlank()) {
            loadedBitmap = null
        }
    }

    val bgColor = if (loadedBitmap != null) {
        Color.White
    } else {
        AvatarColors.getOrElse(bgIndex % AvatarColors.size) { AvatarPeach }
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
        } else if (initials.isBlank()) {
            CustomizableIcon(
                iconKey = HelloIconKey.USER,
                size = size * 0.48f,
                tint = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
