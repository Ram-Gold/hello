package com.example.hello.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello.R

/**
 * The Hello name-tag mark. Colour comes from the theme (`onSurface`),
 * so no raw hex lives in feature code.
 * Automatically switches to the small-size cut below 32.dp (minimum 16.dp).
 */
@Composable
fun HelloMark(modifier: Modifier = Modifier, width: Dp = 48.dp) {
    val clampedWidth = width.coerceAtLeast(16.dp)
    val drawableRes = if (clampedWidth < 32.dp) {
        R.drawable.ic_hello_mark_small
    } else {
        R.drawable.ic_hello_mark
    }
    Image(
        painter = painterResource(drawableRes),
        contentDescription = "Hello",
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        modifier = modifier.size(clampedWidth)
    )
}

/**
 * Horizontal Hello lockup (name-tag symbol beside wordmark).
 * Minimum width 96.dp per logo guidelines.
 */
@Composable
fun HelloHorizontalLogo(modifier: Modifier = Modifier, width: Dp = 180.dp) {
    val clampedWidth = width.coerceAtLeast(96.dp)
    Image(
        painter = painterResource(R.drawable.ic_hello_horizontal),
        contentDescription = "Hello",
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .width(clampedWidth)
            .aspectRatio(760f / 256f)
    )
}

/**
 * Stacked Hello lockup (name-tag symbol above wordmark) for square surfaces.
 * Minimum width 72.dp per logo guidelines.
 */
@Composable
fun HelloStackedLogo(modifier: Modifier = Modifier, width: Dp = 140.dp) {
    val clampedWidth = width.coerceAtLeast(72.dp)
    Image(
        painter = painterResource(R.drawable.ic_hello_stacked),
        contentDescription = "Hello",
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .width(clampedWidth)
            .aspectRatio(560f / 392f)
    )
}
