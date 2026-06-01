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
import com.biospace.ansmonitorpro.data.AssessData
import com.biospace.ansmonitorpro.data.AlertEntry
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

// ── ASSESS Screen ─────────────────────────────────────────────────────────────

@Composable
fun AssessScreen(data: AssessData) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header card
        SectionCard(borderColor = AppColors.Magenta.copy(alpha = 0.5f)) {
            SectionHeader("INTEGRATED ASSESSMENT", AppColors.Magenta)
            SubLabel("SYNTHESIZED BODY BURDEN ANALYSIS · ALL DATA SOURCES")
            Spacer(Modifier.height(12.dp))

            // Big score card
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(AppColors.CardBg2).border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val scoreColor = assessColor(data.totalScore)
                    Text("${data.totalScore}", fontSize = 52.sp, fontWeight = FontWeight.Black, color = scoreColor)
                    Column {
                        Text(data.totalLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                        Text("INTEGRATED BODY BURDEN INDEX", fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                        Text("Space + SR + Environment", fontSize = 11.sp, color = AppColors.TextSecondary)
                    }
                }
            }
        }

        // Load drivers
        SectionCard {
            SubLabel("LOAD DRIVERS")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriverCard("SPACE\nWEATHER",   data.spaceScore, data.spaceMax, Modifier.weight(1f))
                DriverCard("SCHUMANN\nRESONANCE", data.srScore, data.srMax,   Modifier.weight(1f))
                DriverCard("LOCAL\nENVIRONMENT", data.envScore, data.envMax,  Modifier.weight(1f))
            }
        }

        // Clinical narrative
        SectionCard(borderColor = AppColors.Cyan.copy(alpha = 0.3f)) {
            SectionHeader("▸ CLINICAL NARRATIVE", AppColors.Cyan)
            Spacer(Modifier.height(10.dp))
            Text(data.clinicalNarrative, fontSize = 13.sp, color = AppColors.TextSecondary, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun DriverCard(label: String, score: Int, max: Int, modifier: Modifier) {
    val color = assessColor((score.toDouble() / max * 100).toInt())
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 8.sp, color = AppColors.TextDim, letterSpacing = 1.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("$score", fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        Text("/$max", fontSize = 10.sp, color = AppColors.TextDim)
        Spacer(Modifier.height(6.dp))
        DataBar(score.toFloat() / max.toFloat(), color, height = 3.dp)
    }
}

fun assessColor(score: Int): Color = when { score >= 75 -> AppColors.Red; score >= 55 -> Color(0xFFFF6600); score >= 35 -> AppColors.Gold; score >= 20 -> AppColors.Green; else -> AppColors.Green }

// ── ALERTS Screen ─────────────────────────────────────────────────────────────

@Composable
fun AlertsScreen(alerts: List<AlertEntry>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SubLabel("NOAA SPACE WEATHER ALERTS")
        Spacer(Modifier.height(4.dp))

        if (alerts.isEmpty()) {
            SectionCard {
                Text("No active alerts", fontSize = 13.sp, color = AppColors.TextDim)
            }
        } else {
            alerts.forEach { alert ->
                AlertCard(alert)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun AlertCard(alert: AlertEntry) {
    val codeColor = when {
        alert.code.startsWith("W") || alert.code.startsWith("A") -> AppColors.Gold
        alert.code.startsWith("X") -> AppColors.Red
        else -> AppColors.Cyan
    }
    SectionCard {
        Text(alert.messageCode.ifEmpty { alert.code }, fontSize = 14.sp,
            fontWeight = FontWeight.Black, color = codeColor)
        Spacer(Modifier.height(8.dp))
        if (alert.messageCode.isNotEmpty()) {
            Text("Space Weather Message Code: ${alert.messageCode}", fontSize = 11.sp, color = AppColors.TextSecondary)
        }
        if (alert.serial.isNotEmpty()) {
            Text("Serial Number: ${alert.serial}", fontSize = 11.sp, color = AppColors.TextSecondary)
        }
        if (alert.issueTime.isNotEmpty()) {
            Text("Issue Time: ${alert.issueTime}", fontSize = 11.sp, color = AppColors.TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(alert.body.lines().drop(4).take(10).joinToString("\n"),
            fontSize = 11.sp, color = AppColors.TextDim, lineHeight = 15.sp)
    }
}

// ── CME Screen ────────────────────────────────────────────────────────────────

@Composable
fun CmeScreen(kp: Double) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard(borderColor = AppColors.Orange.copy(alpha = 0.4f)) {
            SectionHeader("CORONAL MASS EJECTIONS", Color(0xFFFF6600))
            SubLabel("NOAA SWPC · DONKI DATABASE")
            Spacer(Modifier.height(12.dp))
            Text("CME tracking uses the DONKI API.", fontSize = 12.sp, color = AppColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("Current Kp ${String.format("%.1f",kp)} — ${if (kp >= 5) "Active storm conditions suggest recent CME impact." else "Quiet conditions. No significant CME impact currently indicated."}",
                fontSize = 12.sp, color = AppColors.TextSecondary, lineHeight = 17.sp)
            Spacer(Modifier.height(12.dp))
            SubLabel("CURRENT STORM INDICATORS")
            Spacer(Modifier.height(8.dp))
            listOf(
                "Kp Index" to "${String.format("%.1f",kp)} / 9.0",
                "G-Scale"  to if (kp >= 5) "G${((kp-4).toInt()).coerceIn(1,5)}" else "G0 — None",
                "CME Arrival" to "See NOAA SWPC for active watches",
                "Impact Risk" to if (kp >= 5) "ELEVATED" else "BASELINE"
            ).forEach { (label, value) ->
                LabelValueRow(label, value, if (kp >= 5) AppColors.Gold else AppColors.Green)
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

