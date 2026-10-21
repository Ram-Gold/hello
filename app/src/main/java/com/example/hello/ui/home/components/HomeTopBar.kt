package com.example.hello.ui.home.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.ui.theme.HelloTheme

@Composable
fun HomeTopBar(
    onSettingsClick: () -> Unit,
    onDrawerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Hamburger / Drawer button
        IconButton(
            onClick = onDrawerClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Open Drawer",
                modifier = Modifier.size(22.dp)
            )
        }

        // Right: Settings button
        IconButton(
            onClick = onSettingsClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(name = "HomeTopBar Light", showBackground = true)
@Composable
fun HomeTopBarLightPreview() {
    HelloTheme(darkTheme = false) {
        HomeTopBar(
            onSettingsClick = {},
            onDrawerClick = {}
        )
    }
}

@Preview(name = "HomeTopBar Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeTopBarDarkPreview() {
    HelloTheme(darkTheme = true) {
        HomeTopBar(
            onSettingsClick = {},
            onDrawerClick = {}
        )
    }
}
