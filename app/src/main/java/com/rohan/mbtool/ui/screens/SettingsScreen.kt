package com.rohan.mbtool.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohan.mbtool.ui.theme.*
import com.rohan.mbtool.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel = viewModel()) {
    val cs        = MaterialTheme.colorScheme
    val themeMode by vm.themeMode.collectAsState()

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surf),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Primary.copy(0.12f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Settings, null, tint = Primary, modifier = Modifier.size(17.dp)) }
                        Text("Settings", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            // ── Appearance ────────────────────────────────────────────────────
            item { SectionHeader("Appearance") }
            item {
                // Theme row
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Surface(
                        shape         = RoundedCornerShape(14.dp),
                        color         = Surf,
                        border        = BorderStroke(1.dp, Border),
                        tonalElevation = 0.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PrimaryBg),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Palette, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                                Column(Modifier.weight(1f)) {
                                    Text("Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("App color scheme", fontSize = 12.sp, color = Muted)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ThemeChip(
                                    label    = "Light",
                                    icon     = Icons.Rounded.WbSunny,
                                    selected = themeMode == com.rohan.mbtool.ui.theme.ThemeMode.LIGHT,
                                    onClick  = { vm.setTheme(com.rohan.mbtool.ui.theme.ThemeMode.LIGHT) },
                                    modifier = Modifier.weight(1f),
                                )
                                ThemeChip(
                                    label    = "Dark",
                                    icon     = Icons.Rounded.DarkMode,
                                    selected = themeMode == com.rohan.mbtool.ui.theme.ThemeMode.DARK,
                                    onClick  = { vm.setTheme(com.rohan.mbtool.ui.theme.ThemeMode.DARK) },
                                    modifier = Modifier.weight(1f),
                                )
                                ThemeChip(
                                    label    = "System",
                                    icon     = Icons.Rounded.SettingsBrightness,
                                    selected = themeMode == com.rohan.mbtool.ui.theme.ThemeMode.SYSTEM,
                                    onClick  = { vm.setTheme(com.rohan.mbtool.ui.theme.ThemeMode.SYSTEM) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item { SectionHeader("About") }
            item {
                Surface(
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape         = RoundedCornerShape(14.dp),
                    color         = Surf,
                    border        = BorderStroke(1.dp, Border),
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AboutRow(Icons.Rounded.Apps, "App Version", "1.0")
                        HorizontalDivider(color = Border)
                        AboutRow(Icons.Rounded.Person, "Developer", "Rohan Dora (RD7890)")
                        HorizontalDivider(color = Border)
                        AboutRow(Icons.Rounded.Code, "Engine", "MaterialBinTool port (MIT)")
                        HorizontalDivider(color = Border)
                        AboutRow(Icons.Rounded.PhoneAndroid, "Package", "com.rohan.mbtool")
                    }
                }
            }

            // ── Legal ─────────────────────────────────────────────────────────
            item { SectionHeader("Legal") }
            item {
                Surface(
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape         = RoundedCornerShape(14.dp),
                    color         = Surf,
                    border        = BorderStroke(1.dp, Border),
                    tonalElevation = 0.dp,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "This app uses a Kotlin port of MaterialBinTool by ddf8196 (MIT License). " +
                            "RenderDragon is a trademark of Mojang Studios / Microsoft. " +
                            "This tool is not affiliated with or endorsed by Mojang Studios.",
                            fontSize = 12.sp,
                            color    = Muted,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize   = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color      = Muted,
        letterSpacing = 0.8.sp,
        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick       = onClick,
        modifier      = modifier,
        shape         = RoundedCornerShape(10.dp),
        color         = if (selected) PrimaryBg else SurfVar,
        border        = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Primary else Border),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier              = Modifier.padding(vertical = 10.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = if (selected) Primary else Muted, modifier = Modifier.size(16.dp))
            Text(
                label,
                fontSize   = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = if (selected) Primary else Muted,
            )
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        Text(label, fontSize = 13.sp, color = Muted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
