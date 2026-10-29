package com.mm2vault.dynamicisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mm2vault.dynamicisland.R
import com.mm2vault.dynamicisland.update.Reason
import com.mm2vault.dynamicisland.update.UpdateState

@Composable
fun UpdateCard(
    state: UpdateState,
    versionName: String,
    build: Long,
    needsInstallPermission: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Palette.Surface)
            .border(1.dp, Palette.Hairline, MaterialTheme.shapes.medium).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.update_version, versionName, build.toInt()), style = MaterialTheme.typography.bodySmall)
        when (state) {
            UpdateState.Idle -> {}
            UpdateState.Checking -> Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodySmall)
            UpdateState.UpToDate -> Text(stringResource(R.string.update_uptodate), color = Palette.Success, style = MaterialTheme.typography.bodySmall)
            is UpdateState.Available -> Text(stringResource(R.string.update_available, state.release.build), color = Palette.Lavender, style = MaterialTheme.typography.bodyMedium)
            is UpdateState.Downloading -> {
                Text(stringResource(R.string.update_downloading, state.percent), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth(), color = Palette.Neon, trackColor = Palette.Surface2)
            }
            is UpdateState.Ready -> {
                Text(stringResource(R.string.update_ready), color = Palette.Success, style = MaterialTheme.typography.bodySmall)
                if (needsInstallPermission) Text(stringResource(R.string.update_need_permission), color = Palette.Warning, style = MaterialTheme.typography.bodySmall)
            }
            is UpdateState.Failed -> Text(
                stringResource(
                    when (state.reason) {
                        Reason.NETWORK -> R.string.update_err_network
                        Reason.NO_APK -> R.string.update_err_noapk
                        Reason.CHECKSUM -> R.string.update_err_checksum
                        Reason.STORAGE -> R.string.update_err_storage
                    }
                ), color = Palette.Warning, style = MaterialTheme.typography.bodySmall
            )
        }
        val busy = state is UpdateState.Checking || state is UpdateState.Downloading
        when (state) {
            is UpdateState.Available -> PrimaryButton(R.string.update_download, onDownload)
            is UpdateState.Ready -> PrimaryButton(R.string.update_install, onInstall)
            else -> OutlinedButton(
                onClick = onCheck, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)
            ) { Text(stringResource(R.string.update_check), color = Palette.Text) }
        }
        Text(stringResource(R.string.update_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PrimaryButton(label: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.Neon, contentColor = Color(0xFF1A0B33))
    ) { Text(stringResource(label), color = Color(0xFF1A0B33), style = MaterialTheme.typography.titleMedium) }
}
