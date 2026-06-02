package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.AppSettings
import com.biospace.ansmonitorpro.data.AutonomicProfile
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onGpsToggle: (Boolean) -> Unit,
    onManualLatLon: (Double, Double, String) -> Unit,
    onWatchMac: (String) -> Unit,
    onGeminiKey: (String) -> Unit,
    onUsername: (String) -> Unit,
    onRefresh: () -> Unit,
    onProfileChange: (AutonomicProfile) -> Unit
) {
    var latInput   by remember { mutableStateOf(settings.lat.toString()) }
    var lonInput   by remember { mutableStateOf(settings.lon.toString()) }
    var cityInput  by remember { mutableStateOf(settings.locationName) }
    var macInput   by remember { mutableStateOf(settings.watchMac) }
    var gemInput   by remember { mutableStateOf(settings.geminiKey) }
    var userInput  by remember { mutableStateOf(settings.username) }
    var gemVisible by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard(borderColor = AppColors.Cyan.copy(.3f)) {
            SectionHeader("SETTINGS", AppColors.Cyan)
            SubLabel("PREFERENCES · LOCATION · WATCH · API KEYS")
        }

        // ── Autonomic Profile ─────────────────────────────────────────────
        SectionCard {
            SectionHeader("AUTONOMIC PROFILE", AppColors.Magenta)
            SubLabel("AFFECTS ANS LOAD SCORING SENSITIVITY")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AutonomicProfile.STANDARD to "STANDARD", AutonomicProfile.DYSAUTONOMIA to "DYSAUTONOMIA")
                .forEach { (prof, label) ->
                    val selected = settings.autonomicProfile == prof
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AppColors.Magenta.copy(alpha=0.2f) else AppColors.CardBg)
                            .border(1.dp, if (selected) AppColors.Magenta else AppColors.Divider, RoundedCornerShape(8.dp))
                            .clickable { onProfileChange(prof) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) AppColors.Magenta else AppColors.TextDim,
                            letterSpacing = 1.sp)
                    }
                }
            }
        }

        // ── Location ──────────────────────────────────────────────────────
        SectionCard {
            SectionHeader("LOCATION", AppColors.Gold)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Use GPS", fontSize = 13.sp, color = AppColors.TextPrimary)
                    Text("Auto-detect location on launch", fontSize = 10.sp, color = AppColors.TextDim)
                }
                Switch(
                    checked = settings.useGps,
                    onCheckedChange = onGpsToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.Background,
                        checkedTrackColor = AppColors.Cyan,
                        uncheckedThumbColor = AppColors.TextDim,
                        uncheckedTrackColor = AppColors.CardBg2
                    )
                )
            }
            if (!settings.useGps) {
                Spacer(Modifier.height(10.dp))
                SubLabel("MANUAL COORDINATES")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BioField("LATITUDE", latInput, { latInput = it }, Modifier.weight(1f))
                    BioField("LONGITUDE", lonInput, { lonInput = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                BioField("CITY NAME", cityInput, { cityInput = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                SaveButton("APPLY LOCATION") {
                    val lat = latInput.toDoubleOrNull() ?: settings.lat
                    val lon = lonInput.toDoubleOrNull() ?: settings.lon
                    onManualLatLon(lat, lon, cityInput)
                }
            }
            if (settings.locationName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                LabelValueRow("Current location", settings.locationName)
                LabelValueRow("Coordinates", "${"%.4f".format(settings.lat)}, ${"%.4f".format(settings.lon)}")
            }
        }

        // ── Watch ─────────────────────────────────────────────────────────
        SectionCard {
            SectionHeader("Y007 BLE WATCH", AppColors.Cyan)
            Spacer(Modifier.height(10.dp))
            BioField("WATCH MAC ADDRESS", macInput, { macInput = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            SubLabel("Find MAC in nRF Connect · Format: XX:XX:XX:XX:XX:XX")
            Spacer(Modifier.height(8.dp))
            SaveButton("SAVE MAC") { onWatchMac(macInput) }
        }

        // ── Gemini API Key ────────────────────────────────────────────────
        SectionCard {
            SectionHeader("GEMINI API KEY", AppColors.Magenta)
            Spacer(Modifier.height(6.dp))
            Text(
                "Required for AI Report generation. Get a free key at aistudio.google.com.",
                fontSize = 11.sp, color = AppColors.TextSecondary, lineHeight = 16.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BioField(
                    if (gemVisible) "API KEY (VISIBLE)" else "API KEY (HIDDEN)",
                    if (gemVisible) gemInput else gemInput.take(4) + "•".repeat((gemInput.length - 4).coerceAtLeast(0)),
                    { gemInput = it },
                    Modifier.weight(1f)
                )
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(AppColors.CardBg2)
                        .border(1.dp, AppColors.Divider, RoundedCornerShape(6.dp))
                        .clickable { gemVisible = !gemVisible }
                        .padding(12.dp)
                ) {
                    Text(if (gemVisible) "HIDE" else "SHOW", fontSize = 9.sp,
                        color = AppColors.TextDim, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            SaveButton("SAVE KEY") { onGeminiKey(gemInput) }
        }

        // ── Username ──────────────────────────────────────────────────────
        SectionCard {
            SectionHeader("DISPLAY NAME", AppColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            BioField("YOUR NAME / CALLSIGN", userInput, {
                val filtered = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' }.take(12)
                userInput = filtered
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SaveButton("SAVE NAME") { onUsername(userInput) }
        }

        // ── Manual refresh ────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Green.copy(.12f))
                .border(1.dp, AppColors.Green.copy(.5f), RoundedCornerShape(10.dp))
                .clickable { onRefresh() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("↻  FORCE REFRESH ALL DATA", fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = AppColors.Green, letterSpacing = 1.sp)
        }

        // ── About ─────────────────────────────────────────────────────────
        SectionCard {
            SubLabel("ABOUT")
            Spacer(Modifier.height(8.dp))
            LabelValueRow("App", "ANS Monitor Pro v2.0")
            LabelValueRow("Space weather", "NOAA SWPC · NASA DONKI")
            LabelValueRow("Environment", "Open-Meteo API")
            LabelValueRow("Biometrics", "Y007 BP Doctor BLE")
            LabelValueRow("AI reports", "Google Gemini 2.5 Flash")
            LabelValueRow("Auto-refresh", "Every 5 minutes")
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SaveButton(label: String, onClick: () -> Unit) {
    var saved by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (saved) AppColors.Green.copy(.12f) else AppColors.CardBg2)
            .border(1.dp, if (saved) AppColors.Green.copy(.5f) else AppColors.Divider, RoundedCornerShape(8.dp))
            .clickable { onClick(); saved = true }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (saved) "✓  SAVED" else label,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (saved) AppColors.Green else AppColors.TextSecondary,
            letterSpacing = 1.sp
        )
    }
}
