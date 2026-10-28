package com.mm2vault.dynamicisland

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF08070D)
private val Purple = Color(0xFFB68CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Ink, surface = Color(0xFF17131F))) {
                val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("DYNAMIC ISLAND", color = Purple, fontSize = 13.sp, letterSpacing = 3.sp)
                        Text("Telefonuna yeni bir görünüm.", color = Color.White, fontSize = 28.sp, lineHeight = 34.sp)
                        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Row(Modifier.background(Color.Black, RoundedCornerShape(40.dp)).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(9.dp).background(Purple, RoundedCornerShape(50)))
                                Column {
                                    Text("Dynamic Island", color = Color.White, fontSize = 14.sp)
                                    Text("MOR NEON • LIVE", color = Purple, fontSize = 9.sp, letterSpacing = 1.5.sp)
                                }
                            }
                        }
                        Text("Kurulum", color = Color.White, fontSize = 20.sp)
                        PermissionRow("Diğer uygulamaların üzerinde göster", overlayGranted.value, "İzin ver") {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        }
                        Text("Bu ilk sürüm yalnızca kapsül ve pil/şarj bilgisini gösterir. Bildirim içeriğine erişmez ve internet izni istemez.", color = Color(0xFFAAA4B5), fontSize = 13.sp)
                        Button(onClick = {
                            if (!Settings.canDrawOverlays(this@MainActivity)) {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            } else {
                                val intent = Intent(this@MainActivity, IslandService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Island'ı başlat") }
                        OutlinedButton(onClick = { stopService(Intent(this@MainActivity, IslandService::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Island'ı durdur") }
                        Text("Android 8+ • Ücretsiz • Açık kaynak", color = Color(0xFF8E8799), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, enabled: Boolean, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF17131F), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(if (enabled) "Etkin" else "İzin gerekli", color = if (enabled) Color(0xFF8EE6B1) else Color(0xFFFFC477), fontSize = 12.sp)
        }
        TextButton(onClick = onClick) { Text(action) }
    }
}