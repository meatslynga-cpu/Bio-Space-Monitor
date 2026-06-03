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
import com.biospace.ansmonitorpro.data.SolarStormForecast
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun StormForecastScreen(forecast: SolarStormForecast) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Severity Card ─────────────────────────────────────────────────────
        SectionCard {
            SubLabel("SOLAR STORM FORECAST // ANS MONITOR PRO")
            Spacer(Modifier.height(10.dp))
            val sevColor = stormColor(forecast.severityLabel)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background(sevColor.copy(.15f))
                        .border(1.dp, sevColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(forecast.severityLabel, fontSize = 22.sp, fontWeight = FontWeight.Black,
                        color = sevColor, letterSpacing = 2.sp)
                }
                Column {
                    Text("Score: ${forecast.severityScore}/100", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text("G-Storm: ${forecast.gStormLevel}  Kp max: ${"%.1f".format(forecast.expectedKpMax)}",
                        fontSize = 11.sp, color = AppColors.TextSecondary)
                    Text("Updated: ${forecast.timestamp}", fontSize = 9.sp, color = AppColors.TextDim)
                }
            }
            Spacer(Modifier.height(10.dp))
            val scoreFrac = (forecast.severityScore / 100f).coerceIn(0f, 1f)
            DataBar(scoreFrac, sevColor, Modifier.fillMaxWidth(), height = 5.dp)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("NONE","MINOR","MODERATE","STRONG","SEVERE","EXTREME").forEach {
                    Text(it, fontSize = 7.sp, color = AppColors.TextDim)
                }
            }
        }

        // ── Arrival Card ──────────────────────────────────────────────────────
        SectionCard {
            SubLabel("STORM TIMING")
            Spacer(Modifier.height(8.dp))
            LabelValueRow("Arrival", forecast.arrivalLabel)
            LabelValueRow("Peak Duration", if (forecast.peakDurationHrs > 0) "~${forecast.peakDurationHrs}h" else "N/A")
            LabelValueRow("Full Dissipation", if (forecast.dissipationHrs > 0) "~${forecast.dissipationHrs}h after onset" else "N/A")
        }

        // ── Drivers Card ──────────────────────────────────────────────────────
        SectionCard {
            SubLabel("ACTIVE DRIVERS")
            Spacer(Modifier.height(8.dp))
            if (forecast.drivers.isEmpty()) {
                Text("No significant drivers detected.", fontSize = 11.sp, color = AppColors.TextDim)
            } else {
                forecast.drivers.forEach { d ->
                    Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("▸", fontSize = 10.sp, color = AppColors.Cyan)
                        Text(d, fontSize = 11.sp, color = AppColors.TextPrimary)
                    }
                }
            }
        }

        // ── Source Summary Card ───────────────────────────────────────────────
        SectionCard {
            SubLabel("SOURCE SUMMARY")
            Spacer(Modifier.height(8.dp))
            LabelValueRow("CME Count", forecast.cmeCount.toString())
            LabelValueRow("CME Max Speed", if (forecast.cmeMaxSpeed > 0) "${forecast.cmeMaxSpeed.toInt()} km/s" else "N/A")
            LabelValueRow("Max Flare Class", forecast.flareMaxClass)
            LabelValueRow("Coronal Hole Active", if (forecast.coronalHoleActive) "YES" else "NO")
            LabelValueRow("HSS Contributing", if (forecast.hssContributing) "YES" else "NO")
        }

        // ── Narrative Card ────────────────────────────────────────────────────
        SectionCard {
            SubLabel("FORECAST NARRATIVE")
            Spacer(Modifier.height(8.dp))
            Text(forecast.narrative, fontSize = 11.sp, color = AppColors.TextPrimary, lineHeight = 17.sp)
        }
    }
}

fun stormColor(label: String): Color = when (label) {
    "EXTREME"  -> Color(0xFFFF2222)
    "SEVERE"   -> Color(0xFFFF5500)
    "STRONG"   -> Color(0xFFFF9900)
    "MODERATE" -> Color(0xFFFFCC00)
    "MINOR"    -> Color(0xFF88FF44)
    else       -> Color(0xFF448844)
}
