package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.SpaceWeatherData
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun SpaceScreen(data: SpaceWeatherData) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Kp Card ──────────────────────────────────────────────────────
        SectionCard {
            SubLabel("PLANETARY K-INDEX // NOAA SWPC")
            Spacer(Modifier.height(8.dp))
            // Scale bar
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val kpFrac = (data.kp / 9.0).toFloat().coerceIn(0f, 1f)
                DataBar(kpFrac, kpColor(data.kp), Modifier.fillMaxWidth(), height = 5.dp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0 QUIET","3","5 ACTIVE","7","9 G5").forEach {
                    Text(it, fontSize = 8.sp, color = AppColors.TextDim)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(String.format("%.1f", data.kp), fontSize = 52.sp, fontWeight = FontWeight.Black, color = kpColor(data.kp))
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(data.kpLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = kpColor(data.kp))
                }
            }
            Spacer(Modifier.height(8.dp))
            KpBarChart(data.kpHistory, data.kp, Modifier.fillMaxWidth().height(60.dp))
            Spacer(Modifier.height(4.dp))
            SubLabel("KP HISTORY · 1HR")
        }

        // ── Storm Scales ─────────────────────────────────────────────────
        SectionCard {
            SubLabel("NOAA STORM SCALES")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StormCard("G", data.stormG, Modifier.weight(1f))
                StormCard("S", data.stormS, Modifier.weight(1f))
                StormCard("R", data.stormR, Modifier.weight(1f))
            }
        }

        // ── Solar Wind ───────────────────────────────────────────────────
        SectionCard {
            SubLabel("SOLAR WIND // DSCOVR / ACE")
            Spacer(Modifier.height(12.dp))

            // Speed
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SPEED", fontSize = 10.sp, color = AppColors.TextSecondary, letterSpacing = 1.sp)
                Text("${data.solarWindSpeed.toInt()} km/s", fontSize = 11.sp, color = AppColors.Cyan, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            DataBar((data.solarWindSpeed / 800f).toFloat().coerceIn(0f,1f), AppColors.Cyan)
            Spacer(Modifier.height(10.dp))

            // Density
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("DENSITY", fontSize = 10.sp, color = AppColors.TextSecondary, letterSpacing = 1.sp)
                Text("${String.format("%.1f",data.solarWindDensity)} p/cm³", fontSize = 11.sp, color = AppColors.Green, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            DataBar((data.solarWindDensity / 30f).toFloat().coerceIn(0f,1f), AppColors.Green)
            Spacer(Modifier.height(10.dp))

            // Temperature
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TEMPERATURE", fontSize = 10.sp, color = AppColors.TextSecondary, letterSpacing = 1.sp)
                Text("${data.solarWindTemp.toInt()} kK", fontSize = 11.sp, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))

            // Bz inline
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("IMF Bz DIRECTION", fontSize = 10.sp, color = AppColors.TextSecondary, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg2).border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                val bzSign = if (data.bz >= 0) "+" else ""
                Text(
                    "$bzSign${String.format("%.1f",data.bz)} nT  ▲ ${data.bzLabel}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = bzColor(data.bz)
                )
            }
            Spacer(Modifier.height(10.dp))

            // Mini sparklines row
            Row(Modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SparklineChart(data.speedHistory, AppColors.Cyan, Modifier.weight(1f).fillMaxHeight())
                SparklineChart(data.densityHistory, AppColors.Green, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SubLabel("SPEED 1HR")
                SubLabel("DENSITY 1HR")
            }
        }
        // ── DONKI event flags ─────────────────────────────────────────
        SectionCard {
            SectionHeader("DONKI EVENT FLAGS", AppColors.Gold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventFlag("GST", data.gstActive, Modifier.weight(1f))
                EventFlag("HSS", data.hssActive, Modifier.weight(1f))
                EventFlag("SEP", data.sepActive, Modifier.weight(1f))
                EventFlagCount("IPS", data.ipsCount, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            LabelValueRow("Hemi Power N/S", "${data.hemisphericPowerNorth.toInt()} GW / ${data.hemisphericPowerSouth.toInt()} GW  (${data.fountainDumping})")
        }

        // ── CME arrival ───────────────────────────────────────────────
        SectionCard {
            SectionHeader("CME ANALYSIS", AppColors.Orange)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("SPEED", "${data.cmeSpeed.toInt()}", "km/s",
                    when { data.cmeSpeed > 1500 -> "EXTREME"; data.cmeSpeed > 900 -> "HIGH"; data.cmeSpeed > 500 -> "MODERATE"; else -> "SLOW" },
                    if (data.cmeSpeed > 900) AppColors.Red else AppColors.Gold, Modifier.weight(1f))
                MetricCard("ARRIVAL", if (data.cmeArrivalHrs < 999) "${data.cmeArrivalHrs}h" else "N/A",
                    "", data.cmeDirection,
                    if (data.cmeArrivalHrs < 24) AppColors.Red else if (data.cmeArrivalHrs < 72) AppColors.Gold else AppColors.Green,
                    Modifier.weight(1f))
            }
        }

        // ── Solar flares ──────────────────────────────────────────────
        if (data.flares.isNotEmpty()) {
            SectionCard {
                SectionHeader("RECENT SOLAR FLARES", AppColors.Red)
                SubLabel("LAST 7 DAYS · EARTH-DIRECTED EVENTS FLAGGED")
                Spacer(Modifier.height(8.dp))
                data.flares.forEach { f ->
                    val cls = f.flareClass
                    val clsColor = when {
                        cls.startsWith("X") -> AppColors.Red
                        cls.startsWith("M") -> AppColors.Orange
                        cls.startsWith("C") -> AppColors.Gold
                        else -> AppColors.TextDim
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cls, fontSize = 16.sp, fontWeight = FontWeight.Black,
                            color = clsColor, modifier = Modifier.width(48.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.startTime, fontSize = 11.sp, color = AppColors.TextSecondary)
                            Text(f.direction, fontSize = 9.sp, color = if (f.direction == "Earth-directed") AppColors.Red else AppColors.TextDim)
                        }
                        if (f.hasCme) Box(
                            Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .background(AppColors.Red.copy(.2f))
                                .border(1.dp, AppColors.Red, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("CME", fontSize = 8.sp, color = AppColors.Red, fontWeight = FontWeight.Bold) }
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun EventFlag(label: String, active: Boolean, modifier: Modifier) {
    val color = if (active) AppColors.Red else AppColors.TextDim
    Column(
        modifier.clip(RoundedCornerShape(8.dp))
            .background(if (active) AppColors.Red.copy(.12f) else AppColors.CardBg2)
            .border(1.dp, if (active) AppColors.Red.copy(.5f) else AppColors.Divider, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (active) "●" else "○", fontSize = 10.sp, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun EventFlagCount(label: String, count: Int, modifier: Modifier) {
    val color = if (count > 0) AppColors.Gold else AppColors.TextDim
    Column(
        modifier.clip(RoundedCornerShape(8.dp))
            .background(if (count > 0) AppColors.Gold.copy(.1f) else AppColors.CardBg2)
            .border(1.dp, if (count > 0) AppColors.Gold.copy(.4f) else AppColors.Divider, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun StormCard(prefix: String, value: String, modifier: Modifier) {
    val level = value.filter { it.isDigit() }.toIntOrNull() ?: 0
    val color = when { level >= 4 -> AppColors.Red; level >= 2 -> AppColors.Gold; level == 1 -> AppColors.Gold; else -> AppColors.TextSecondary }
    val label = when(prefix) { "G" -> "STORM INDEX"; "S" -> "SOLAR RAD"; else -> "RADIO" }
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 8.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
    }
}

fun kpColor(kp: Double): Color = when {
    kp >= 7 -> AppColors.Red
    kp >= 5 -> AppColors.Orange
    kp >= 3 -> AppColors.Gold
    else    -> AppColors.Green
}

fun bzColor(bz: Double): Color = when {
    bz < -10 -> AppColors.Red
    bz < -3  -> AppColors.Orange
    bz < 0   -> AppColors.Gold
    else     -> AppColors.Green
}
