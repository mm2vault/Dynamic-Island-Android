package com.mm2vault.dynamicisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mm2vault.dynamicisland.R
import com.mm2vault.dynamicisland.data.IslandSettings

@Composable
fun SettingsScreen(
    settings: IslandSettings,
    onChange: (IslandSettings) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    listenerGranted: Boolean,
    onOpenAccess: () -> Unit,
    update: @Composable () -> Unit,
) {
    // 0 = none, 1 = notifications, 2 = media: which feature is waiting for the consent dialog.
    var consentFor by remember { mutableStateOf(0) }
    fun toggleLive(kind: Int, on: Boolean) {
        fun apply(v: Boolean) = onChange(if (kind == 1) settings.copy(notificationsEnabled = v) else settings.copy(mediaEnabled = v))
        if (on && !listenerGranted) consentFor = kind else apply(on)
    }
    if (consentFor != 0) {
        AlertDialog(
            onDismissRequest = { consentFor = 0 },
            title = { Text(stringResource(R.string.consent_title)) },
            text = { Text(stringResource(R.string.consent_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val k = consentFor; consentFor = 0
                    onChange(if (k == 1) settings.copy(notificationsEnabled = true) else settings.copy(mediaEnabled = true))
                    onOpenAccess()
                }) { Text(stringResource(R.string.consent_continue), color = Palette.Lavender) }
            },
            dismissButton = {
                TextButton(onClick = { consentFor = 0 }) { Text(stringResource(R.string.consent_cancel), color = Palette.TextDim) }
            },
            containerColor = Palette.Surface2, titleContentColor = Palette.Text, textContentColor = Palette.TextDim,
        )
    }
    Box(Modifier.fillMaxSize().background(Palette.Screen)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = Palette.Lavender)
                }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium)
            }
            IslandPreview()
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Palette.Surface)
                    .border(1.dp, Palette.Hairline, MaterialTheme.shapes.medium).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SliderRow(R.string.setting_width, settings.widthDp.toFloat(), IslandSettings.WIDTH_RANGE, "${settings.widthDp} dp") {
                    onChange(settings.copy(widthDp = it.toInt()))
                }
                SliderRow(R.string.setting_height, settings.heightDp.toFloat(), IslandSettings.HEIGHT_RANGE, "${settings.heightDp} dp") {
                    onChange(settings.copy(heightDp = it.toInt()))
                }
                SliderRow(R.string.setting_offset, settings.offsetDp.toFloat(), IslandSettings.OFFSET_RANGE, "${settings.offsetDp} dp") {
                    onChange(settings.copy(offsetDp = it.toInt()))
                }
                PercentRow(R.string.setting_animation, settings.animation) { onChange(settings.copy(animation = it)) }
                PercentRow(R.string.setting_glow, settings.glow) { onChange(settings.copy(glow = it)) }
                SwitchRow(R.string.setting_haptics, R.string.setting_haptics_desc, settings.haptics) { onChange(settings.copy(haptics = it)) }
                SwitchRow(R.string.setting_autostart, R.string.setting_autostart_desc, settings.autostart) { onChange(settings.copy(autostart = it)) }
            }
            Text(stringResource(R.string.settings_apply_hint), style = MaterialTheme.typography.bodySmall)
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Palette.Surface)
                    .border(1.dp, Palette.Hairline, MaterialTheme.shapes.medium).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(stringResource(R.string.live_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.live_desc), style = MaterialTheme.typography.bodySmall)
                SwitchRow(R.string.live_notifications, R.string.live_notifications_desc, settings.notificationsEnabled) { toggleLive(1, it) }
                SwitchRow(R.string.live_media, R.string.live_media_desc, settings.mediaEnabled) { toggleLive(2, it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (listenerGranted) R.string.live_access_granted else R.string.live_access_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (listenerGranted) Palette.Success else Palette.Warning,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onOpenAccess, modifier = Modifier.height(48.dp)) {
                        Text(stringResource(R.string.live_open_access), color = Palette.Lavender)
                    }
                }
                Text(stringResource(R.string.live_privacy), style = MaterialTheme.typography.bodySmall)
            }
            update()
            OutlinedButton(
                onClick = onReset, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)
            ) { Text(stringResource(R.string.settings_reset), color = Palette.Text) }
        }
    }
}

@Composable
private fun SliderRow(label: Int, value: Float, range: IntRange, shown: String, onValue: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(shown, style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value, onValueChange = onValue,
            valueRange = range.first.toFloat()..range.last.toFloat(), steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = sliderColors()
        )
    }
}

@Composable
private fun PercentRow(label: Int, value: Float, onValue: (Float) -> Unit) {
    SliderRowFloat(label, value, "${(value * 100).toInt()}%", onValue)
}

@Composable
private fun SliderRowFloat(label: Int, value: Float, shown: String, onValue: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(shown, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onValue, valueRange = 0f..1f, colors = sliderColors())
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Palette.Lavender, activeTrackColor = Palette.Neon,
    inactiveTrackColor = Palette.Surface2, activeTickColor = Palette.Neon.copy(alpha = 0f), inactiveTickColor = Palette.Neon.copy(alpha = 0f)
)

@Composable
private fun SwitchRow(title: Int, desc: Int, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.Background, checkedTrackColor = Palette.Neon,
                uncheckedThumbColor = Palette.TextDim, uncheckedTrackColor = Palette.Surface2
            )
        )
    }
}
