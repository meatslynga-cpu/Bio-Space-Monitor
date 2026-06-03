package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.ui.theme.AppColors
import com.biospace.ansmonitorpro.viewmodel.MainViewModel
import com.biospace.ansmonitorpro.data.SolarStormForecast

private data class Tab(val id: String, val label: String)
private val TABS = listOf(
    Tab("ans",      "ANS"),
    Tab("bio",      "BIO"),
    Tab("space",    "SPACE"),
    Tab("imf",      "IMF"),
    Tab("sr",       "SR"),
    Tab("env",      "ENV"),
    Tab("assess",   "ASSESS"),
    Tab("alerts",   "ALERTS"),
    Tab("symptoms", "LOG"),
    Tab("report",   "REPORT"),
    Tab("chat",     "CHAT"),
    Tab("storm",    "STORM"),
    Tab("settings", "⚙")
)

@Composable
fun AppNavigation(vm: MainViewModel) {
    val state  by vm.uiState.collectAsState()
    var tabId  by remember { mutableStateOf("ans") }

    Column(Modifier.fillMaxSize().background(AppColors.Background)) {

        // ── Top status bar ────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(AppColors.CardBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ANS MONITOR PRO", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = AppColors.Cyan, letterSpacing = 1.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(8.dp), color = AppColors.Cyan, strokeWidth = 1.5.dp)
                    } else {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                            .background(alertDot(state.ans.alertLevel.name)))
                    }
                    Text(
                        if (state.isLoading) "UPDATING…"
                        else "${state.env.cityName.take(22)} · Kp ${"%.1f".format(state.space.kp)} · ${state.ans.alertLevel.label}",
                        fontSize = 9.sp, color = AppColors.TextDim
                    )
                }
            }
            // Alert badge
            val badgeColor = alertDot(state.ans.alertLevel.name)
            Box(
                Modifier.clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(.15f))
                    .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(state.ans.alertLevel.name, fontSize = 9.sp,
                    color = badgeColor, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }

        // ── Error banner ──────────────────────────────────────────────────
        state.error?.let { err ->
            Row(
                Modifier.fillMaxWidth().background(AppColors.Red.copy(.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠", fontSize = 12.sp)
                Text(err, fontSize = 10.sp, color = AppColors.Red,
                    modifier = Modifier.weight(1f))
                Text("RETRY", fontSize = 9.sp, color = AppColors.Cyan,
                    fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.refresh() })
            }
        }

        // ── Alert instruction strip ───────────────────────────────────────
        val alertColor = alertDot(state.ans.alertLevel.name)
        Box(
            Modifier.fillMaxWidth()
                .background(alertColor.copy(.08f))
                .border(0.dp, Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(state.ans.alertLevel.instruction, fontSize = 10.sp,
                color = alertColor.copy(.9f), lineHeight = 14.sp)
        }

        // ── Tab bar ───────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(AppColors.CardBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TABS.forEach { tab ->
                val sel = tabId == tab.id
                Box(
                    Modifier.clip(RoundedCornerShape(7.dp))
                        .background(if (sel) AppColors.CardBg2 else Color.Transparent)
                        .border(1.dp, if (sel) AppColors.Cyan else AppColors.TabBorder, RoundedCornerShape(7.dp))
                        .clickable { tabId = tab.id }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (sel) AppColors.Cyan else AppColors.TextSecondary,
                        letterSpacing = 0.8.sp)
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tabId) {
                "ans"   -> AnsScreen(state.ans)
                "bio"   -> BioScreen(
                    bio            = state.bio,
                    watchConnected = state.bio.isWatchConnected,
                    watchMac       = state.settings.watchMac,
                    onConnect      = { mac -> vm.setWatchMac(mac); vm.watchRepo.connect(mac) },
                    onDisconnect   = { vm.watchRepo.disconnect() },
                    onManualUpdate = { vm.watchRepo.updateManual(it) },
                    onMacChange    = { vm.setWatchMac(it) }
                )
                "space"    -> SpaceScreen(state.space)
                "imf"      -> ImfScreen(state.space)
                "sr"       -> SrScreen(state.schumann)
                "env"      -> EnvScreen(state.env)
                "assess"   -> AssessScreen(state.assess)
                "alerts"   -> AlertsScreen(state.alerts)
                "symptoms" -> SymptomsScreen(
                    logs      = state.symptomLogs,
                    space     = state.space,
                    schumann  = state.schumann,
                    env       = state.env,
                    ans       = state.ans,
                    onLog     = { vm.logSymptom(it) }
                )
                "report"   -> ReportScreen(
                    reportOutput  = state.reportOutput,
                    reportLoading = state.reportLoading,
                    hasGeminiKey  = state.settings.geminiKey.isNotBlank(),
                    onGenerate    = { clinical -> vm.generateReport(clinical) }
                )
                "chat"     -> ChatScreen(
                    kp          = state.space.kp,
                    lastUpdated = state.lastUpdated
                )
                "storm"    -> StormForecastScreen(state.stormForecast)
                "settings" -> SettingsScreen(
                    settings      = state.settings,
                    onGpsToggle   = { vm.setUseGps(it) },
                    onManualLatLon= { lat, lon, city -> vm.setLocation(lat, lon, city) },
                    onWatchMac    = { vm.setWatchMac(it) },
                    onGeminiKey   = { vm.setGeminiKey(it) },
                    onUsername    = { vm.setUsername(it) },
                    onRefresh     = { vm.refresh() },
                    onProfileChange = { vm.onProfileChange(it) }
                )
            }
        }
    }
}

fun alertDot(levelName: String): Color = when (levelName) {
    "GREEN" -> AppColors.Green
    "YELLOW"-> AppColors.Gold
    "RED"   -> AppColors.Red
    "BLUE"  -> Color(0xFF1A8FFF)
    else    -> AppColors.TextDim
}
