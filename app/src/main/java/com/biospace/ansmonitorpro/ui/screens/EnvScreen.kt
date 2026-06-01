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
import com.biospace.ansmonitorpro.data.EnvData
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors
import kotlin.math.abs

@Composable
fun EnvScreen(data: EnvData) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Location card ─────────────────────────────────────────────────
        SectionCard(borderColor = AppColors.Cyan.copy(alpha = 0.4f)) {
            SectionHeader("LOCAL ENVIRONMENT", AppColors.Cyan)
            SubLabel("LOCAL WEATHER · AUTONOMIC DYSFUNCTION RISK FACTORS")
            Spacer(Modifier.height(12.dp))

            // Location row
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg2).border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📍", fontSize = 16.sp)
                    Column {
                        Text(data.cityName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("${String.format("%.4f",data.lat)}°, ${String.format("%.4f",data.lon)}°",
                            fontSize = 10.sp, color = AppColors.TextDim)
                    }
                }
                Text("⟳", fontSize = 18.sp, color = AppColors.Cyan)
            }
        }

        // ── 4 metric cards ────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val tempColor = when { data.tempF > 95 -> AppColors.Red; data.tempF > 85 -> AppColors.Gold; data.tempF > 70 -> AppColors.Cyan; else -> AppColors.Green }
            val humColor  = when { data.humidity > 80 -> AppColors.Red; data.humidity > 65 -> AppColors.Gold; else -> AppColors.Green }
            val pressColor = when { abs(data.pressureDelta) > 3 -> AppColors.Red; abs(data.pressureDelta) > 1 -> AppColors.Gold; else -> AppColors.Cyan }
            val windColor  = when { data.windMph > 25 -> AppColors.Red; data.windMph > 15 -> AppColors.Gold; else -> AppColors.Green }

            EnvMetricCard("${data.tempF}", "°F", "TEMPERATURE", data.tempLabel, tempColor, Modifier.weight(1f))
            EnvMetricCard("${data.humidity}", "% RH", "HUMIDITY", data.humidLabel, humColor, Modifier.weight(1f))
            EnvMetricCard("${data.pressureHpa.toInt()}", "hPa", "PRESSURE", data.pressureLabel, pressColor, Modifier.weight(1f))
            EnvMetricCard("${data.windMph}", "mph", "WIND", data.windLabel, windColor, Modifier.weight(1f))
        }

        // ── Pressure trend chart ──────────────────────────────────────────
        SectionCard {
            val deltaSign = if (data.pressureDelta >= 0) "+" else ""
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SubLabel("BAROMETRIC PRESSURE · 24HR TREND (hPa)")
                Text("$deltaSign${String.format("%.1f",data.pressureDelta)} hPa",
                    fontSize = 11.sp, color = pressureDeltaColor(data.pressureDelta), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(80.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xFF060810))
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp)).padding(6.dp)
            ) {
                SparklineChart(data.pressureHistory, AppColors.Gold, Modifier.fillMaxSize(), filled = true)
            }
        }

        // ── Autonomic Dysfunction Trigger Assessment ───────────────────────
        SectionCard {
            SubLabel("AUTONOMIC DYSFUNCTION TRIGGER ASSESSMENT")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriggerCard("🌡", "HEAT LOAD", "${data.tempF}°F / HI ${data.heatIndex}°F", data.heatLoad, heatColor(data.heatLoad), Modifier.weight(1f))
                TriggerCard("💧", "HUMIDITY STRESS", "${data.humidity}% RH", data.humidStress, humStressColor(data.humidStress), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val pdLabel = when { data.pressureDelta < -3 -> "SEVERE DROP"; data.pressureDelta < -1 -> "MODERATE DROP"; data.pressureDelta > 3 -> "RAPID RISE"; abs(data.pressureDelta) < 1 -> "STABLE"; else -> "MILD CHANGE" }
                val pdSeverity = when { abs(data.pressureDelta) > 3 -> "HIGH"; abs(data.pressureDelta) > 1 -> "MODERATE"; else -> "LOW" }
                TriggerCard("🌀", "PRESSURE CHANGE", "${String.format("%.1f",data.pressureDelta)} hPa/hr", pdSeverity, pressureDeltaColor(data.pressureDelta), Modifier.weight(1f))
                TriggerCard("💨", "WIND STRESS", "${data.windMph} mph", data.windLabel, if (data.windMph > 20) AppColors.Gold else AppColors.Green, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun EnvMetricCard(value: String, unit: String, label: String, statusLabel: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        Text(unit, fontSize = 9.sp, color = AppColors.TextDim)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 7.sp, color = AppColors.TextDim, letterSpacing = 0.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider).padding(vertical = 3.dp))
        Spacer(Modifier.height(3.dp))
        Text(statusLabel, fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun TriggerCard(emoji: String, label: String, value: String, severity: String, color: Color, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Column {
            Text(label, fontSize = 8.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 11.sp, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(severity, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

fun pressureDeltaColor(d: Double): Color = when { d < -3 -> AppColors.Red; d < -1 -> AppColors.Gold; d > 3 -> AppColors.Orange; else -> AppColors.Green }
fun heatColor(s: String): Color = when { s == "SEVERE" -> AppColors.Red; s == "MODERATE" -> AppColors.Gold; s == "MILD" -> AppColors.Gold; else -> AppColors.Green }
fun humStressColor(s: String): Color = when { s == "HIGH" -> AppColors.Red; s == "MODERATE" -> AppColors.Gold; else -> AppColors.Green }
