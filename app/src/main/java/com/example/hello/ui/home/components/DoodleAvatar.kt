package com.example.hello.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val bgColor = AvatarColors.getOrElse(bgIndex % AvatarColors.size) { AvatarPeach }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center
    ) {
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
