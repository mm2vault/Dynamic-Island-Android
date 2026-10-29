package com.mm2vault.dynamicisland

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mm2vault.dynamicisland.data.IslandSettings
import com.mm2vault.dynamicisland.data.SettingsRepository
import com.mm2vault.dynamicisland.ui.HomeScreen
import com.mm2vault.dynamicisland.ui.IslandTheme
import com.mm2vault.dynamicisland.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = SettingsRepository(applicationContext)
        setContent {
            IslandTheme {
                val scope = rememberCoroutineScope()
                var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
                var startError by remember { mutableStateOf(false) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                val running by IslandService.running.collectAsState()
                val settings by repo.flow.collectAsState(initial = IslandSettings())

                // Re-check the permission whenever the user returns from Android settings.
                val owner = LocalLifecycleOwner.current
                DisposableEffect(owner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) overlayGranted = Settings.canDrawOverlays(this@MainActivity)
                    }
                    owner.lifecycle.addObserver(observer)
                    onDispose { owner.lifecycle.removeObserver(observer) }
                }

                fun startIsland() {
                    startError = false
                    if (!Settings.canDrawOverlays(this@MainActivity)) { overlayGranted = false; return }
                    try { IslandService.start(this@MainActivity) } catch (e: Exception) { startError = true }
                }

                val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    // The island works with or without the notification permission.
                    startIsland()
                }

                BackHandler(enabled = showSettings) { showSettings = false }

                if (showSettings) {
                    SettingsScreen(
                        settings = settings,
                        onChange = { n -> scope.launch { repo.update { n } } },
                        onReset = { scope.launch { repo.reset() } },
                        onBack = { showSettings = false },
                    )
                } else {
                    HomeScreen(
                        running = running,
                        overlayGranted = overlayGranted,
                        startError = startError,
                        onStart = {
                            val needsNotif = Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            if (needsNotif) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else startIsland()
                        },
                        onStop = { IslandService.stop(this@MainActivity) },
                        onOpenPermission = {
                            try {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            } catch (e: Exception) {
                                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                            }
                        },
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }
}
