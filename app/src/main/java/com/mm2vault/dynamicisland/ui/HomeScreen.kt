package com.mm2vault.dynamicisland.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mm2vault.dynamicisland.BuildConfig
import com.mm2vault.dynamicisland.R

@Composable
fun HomeScreen(
    running: Boolean,
    overlayGranted: Boolean,
    startError: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Palette.Screen)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Header(running, onOpenSettings)
            IslandPreview()
            Text(
                stringResource(R.string.preview_label) + " · " + stringResource(R.string.preview_hint),
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Features()
            PermissionRow(overlayGranted, onOpenPermission)
            if (startError) Text(stringResource(R.string.start_failed), color = Palette.Warning, style = MaterialTheme.typography.bodySmall)
            Controls(running, overlayGranted, onStart, onStop, onOpenPermission)
            Reliability(onOpenBattery, onOpenAppInfo)
            Text(stringResource(if (BuildConfig.FULL) R.string.privacy_note else R.string.privacy_note_lite), style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Header(running: Boolean, onOpenSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo()
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        StatusPill(running)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings), tint = Palette.Lavender)
        }
    }
}

@Composable
private fun Logo() {
    val cd = stringResource(R.string.logo_cd)
    Canvas(Modifier.size(40.dp).semantics { contentDescription = cd }.clip(RoundedCornerShape(12.dp)).background(Palette.Surface)) {
        val w = size.width * 0.66f; val h = size.height * 0.30f
        val tl = Offset((size.width - w) / 2f, (size.height - h) / 2f)
        drawRoundRect(Color.Black, tl, Size(w, h), CornerRadius(h / 2f))
        drawRoundRect(Palette.Neon, tl, Size(w, h), CornerRadius(h / 2f), style = Stroke(1.5.dp.toPx()))
        drawCircle(Palette.Lavender, h * 0.2f, Offset(tl.x + h * 0.55f, tl.y + h / 2f))
    }
}

@Composable
private fun StatusPill(running: Boolean) {
    val color = if (running) Palette.Success else Palette.TextDim
    Row(
        Modifier.clip(CircleShape).background(Palette.Surface2).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(if (running) R.string.status_active else R.string.status_inactive),
            style = MaterialTheme.typography.labelMedium, color = color
        )
    }
}

@Composable
private fun Features() {
    val items = listOf(
        R.string.feature_battery to R.string.feature_battery_desc,
        R.string.feature_charging to R.string.feature_charging_desc,
        R.string.feature_expand to R.string.feature_expand_desc,
        R.string.feature_overlay to R.string.feature_overlay_desc,
    )
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Palette.Surface)
            .border(1.dp, Palette.Hairline, MaterialTheme.shapes.medium).padding(vertical = 6.dp)
    ) {
        Text(stringResource(R.string.features_title), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
        items.forEach { (t, d) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(Palette.Neon))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(t), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(d), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(granted: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Palette.Surface)
            .border(1.dp, if (granted) Palette.Success.copy(alpha = 0.35f) else Palette.Warning.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(if (granted) R.string.permission_granted else R.string.permission_missing),
                style = MaterialTheme.typography.bodySmall, color = if (granted) Palette.Success else Palette.Warning
            )
        }
        if (!granted) {
            TextButton(onClick = onOpen, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.permission_open_settings), color = Palette.Lavender)
            }
        }
    }
}

@Composable
private fun Controls(running: Boolean, granted: Boolean, onStart: () -> Unit, onStop: () -> Unit, onOpenPermission: () -> Unit) {
    if (running) {
        OutlinedButton(
            onClick = onStop, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Palette.Neon.copy(alpha = 0.6f))
        ) { Text(stringResource(R.string.stop_island), color = Palette.Text, style = MaterialTheme.typography.titleMedium) }
    } else {
        Button(
            onClick = if (granted) onStart else onOpenPermission,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Neon, contentColor = Color(0xFF1A0B33))
        ) { Text(stringResource(R.string.start_island), style = MaterialTheme.typography.titleMedium, color = Color(0xFF1A0B33)) }
    }
}

@Composable
private fun Reliability(onBattery: () -> Unit, onAppInfo: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Palette.Surface)
            .border(1.dp, Palette.Hairline, MaterialTheme.shapes.medium).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.reliability_title), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.reliability_desc), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBattery, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Text(stringResource(R.string.reliability_battery), color = Palette.Text, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(onClick = onAppInfo, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Text(stringResource(R.string.reliability_app_info), color = Palette.Text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
