package com.example.hello.ui.home.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hello.ui.theme.HelloTheme

@Composable
fun HomeTopBar(
    userName: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clean, bold headline matching People and Timeline screens
        Text(
            text = "Hello, $userName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false)
        )

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
                imageVector = Icons.Rounded.Settings,
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
            userName = "Alex",
            onSettingsClick = {}
        )
    }
}

@Preview(name = "HomeTopBar Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeTopBarDarkPreview() {
    HelloTheme(darkTheme = true) {
        HomeTopBar(
            userName = "Alex",
            onSettingsClick = {}
        )
    }
}
