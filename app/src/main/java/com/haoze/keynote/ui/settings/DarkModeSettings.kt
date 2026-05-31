package com.haoze.keynote.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.haoze.keynote.ui.theme.DarkModePreference
import com.haoze.keynote.ui.theme.LocalAppColors

@Composable
fun DarkModeSettings(
    currentPreference: DarkModePreference,
    onPreferenceChange: (DarkModePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Column(modifier = modifier) {
        Text("主题切换", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkModeOption(
                icon = Icons.Default.SettingsBrightness,
                label = "跟随系统",
                selected = currentPreference == DarkModePreference.SYSTEM,
                onClick = { onPreferenceChange(DarkModePreference.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
            DarkModeOption(
                icon = Icons.Default.LightMode,
                label = "始终浅色",
                selected = currentPreference == DarkModePreference.LIGHT,
                onClick = { onPreferenceChange(DarkModePreference.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            DarkModeOption(
                icon = Icons.Default.DarkMode,
                label = "始终深色",
                selected = currentPreference == DarkModePreference.DARK,
                onClick = { onPreferenceChange(DarkModePreference.DARK) },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Composable
private fun DarkModeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Surface(
        onClick = onClick,
        modifier = modifier
            .border(
                1.dp,
                if (selected) colors.primary else colors.outlineVariant,
                MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface
            )
        }
    }
}
