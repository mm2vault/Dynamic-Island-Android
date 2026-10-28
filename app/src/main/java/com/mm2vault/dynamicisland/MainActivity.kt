package com.mm2vault.dynamicisland

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF090711)
private val Panel = Color(0xFF171323)
private val Purple = Color(0xFFB98AFF)
private val Pink = Color(0xFFEC8CFF)
private val Muted = Color(0xFFA9A2B9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Purple, secondary = Pink, background = Background,
                surface = Panel, onSurface = Color.White
            )) {
                var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(this@MainActivity)) }
                Surface(Modifier.fillMaxSize(), color = Background) {
                    Box(Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xFF211333), Background, Color(0xFF08070D)))
                    )) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier.size(42.dp).clip(RoundedCornerShape(15.dp))
                                        .background(Brush.linearGradient(listOf(Purple, Color(0xFF7650D8)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("D", color = Color(0xFF10091B), fontSize = 24.sp, fontWeight = FontWeight.Black)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("DYNAMIC ISLAND", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                    Text("PURPLE GLASS EDITION", color = Purple, fontSize = 10.sp, letterSpacing = 1.6.sp)
                                }
                                Box(Modifier.clip(CircleShape).background(Color(0xFF252035)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text("BETA", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("Android’ına yeni
bir hava kat.", color = Color.White, fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
                                Text("Minimal kapsül. Yumuşak animasyonlar. Mor neon detaylar.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
                            }

                            // Large, decorative live-style preview
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF2B1D42), Color(0xFF171323), Color(0xFF111019))))
                                    .border(1.dp, Color(0xFF4D3869), RoundedCornerShape(28.dp))
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(17.dp)) {
                                    Text("CANLI ÖNİZLEME", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        Modifier.clip(RoundedCornerShape(40.dp))
                                            .background(Color(0xFF050507))
                                            .border(1.dp, Color(0xFF39313F), RoundedCornerShape(40.dp))
                                            .padding(start = 15.dp, end = 17.dp, top = 11.dp, bottom = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(Modifier.size(9.dp).clip(CircleShape).background(Purple))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("DYNAMIC ISLAND", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            Text("Pil ve şarj durumu", color = Color(0xFFB9B2C6), fontSize = 10.sp)
                                        }
                                        Box(Modifier.size(1.dp, 27.dp).background(Color(0xFF39313F)))
                                        Text("100%", color = Color(0xFFBDA0FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        repeat(3) { index ->
                                            Box(Modifier.size(if (index == 1) 6.dp else 4.dp).clip(CircleShape).background(if (index == 1) Purple else Color(0xFF6A587F)))
                                        }
                                        Text("TASARIM ÖNİZLEMESİ", color = Muted, fontSize = 9.sp, letterSpacing = 1.1.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FeatureTile("01", "Mor neon", "Cam görünümlü koyu tema", Modifier.weight(1f))
                                FeatureTile("02", "Pil & şarj", "Durum kapsülde görünür", Modifier.weight(1f))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("KURULUM", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                        .background(Panel).border(1.dp, Color(0xFF332843), RoundedCornerShape(20.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2C2040)), contentAlignment = Alignment.Center) {
                                        Text("01", color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Diğer uygulamaların üzerinde göster", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(if (overlayGranted) "İzin hazır" else "Kapsül için izin gerekiyor", color = if (overlayGranted) Color(0xFF8EE6B1) else Color(0xFFFFC477), fontSize = 12.sp)
                                    }
                                    TextButton(onClick = {
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                                    }) { Text(if (overlayGranted) "Ayarlar" else "İzin ver") }
                                }
                                Text("Gizlilik önceliğimiz: bu sürüm bildirim içeriklerini okumaz ve internet izni istemez.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                            }

                            Button(
                                onClick = {
                                    overlayGranted = Settings.canDrawOverlays(this@MainActivity)
                                    if (!overlayGranted) {
                                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                                    } else {
                                        val intent = Intent(this@MainActivity, IslandService::class.java)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color(0xFF170D24))
                            ) { Text("Island’ı başlat", fontSize = 15.sp, fontWeight = FontWeight.Bold) }

                            OutlinedButton(
                                onClick = { stopService(Intent(this@MainActivity, IslandService::class.java)) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4B3A60))
                            ) { Text("Island’ı durdur", color = Color.White, fontWeight = FontWeight.SemiBold) }

                            Text("Android 8+  •  Ücretsiz  •  Açık kaynak", modifier = Modifier.fillMaxWidth(), color = Color(0xFF777083), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureTile(number: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF171323))
            .border(1.dp, Color(0xFF332843), RoundedCornerShape(20.dp)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(number, color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
