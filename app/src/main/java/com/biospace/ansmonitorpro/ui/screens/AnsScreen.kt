package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.AnsData
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun AnsScreen(data: AnsData) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── ANS Load Engine card ──────────────────────────────────────────
        SectionCard(borderColor = AppColors.Magenta.copy(alpha = 0.3f)) {
            SectionHeader("ANS LOAD ENGINE", AppColors.Magenta)
            SubLabel("AUTONOMIC NERVOUS SYSTEM · REAL-TIME BURDEN ANALYSIS")
            Spacer(Modifier.height(12.dp))

            val loadColor = loadColor(data.loadIndex)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularGauge(data.loadIndex, 100, loadColor, 110.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(data.loadLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = loadColor)
                    Text("ANS LOAD INDEX", fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Coherence: ${data.coherencePct}%", fontSize = 13.sp, color = AppColors.TextSecondary)
                    Text("Sympathetic bias: ${data.sympatheticBias}%", fontSize = 13.sp, color = AppColors.TextSecondary)
                }
            }
        }

        // ── Field quality ─────────────────────────────────────────────────
        SectionCard {
            SubLabel("ENVIRONMENTAL COHERENCE FIELD QUALITY")
            Spacer(Modifier.height(8.dp))
            Text(data.fieldQuality, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fqColor(data.fieldQuality))
            SubLabel("FIELD QUALITY STATE")
            Spacer(Modifier.height(8.dp))
            DataBar(data.fieldQualityScore, fqColor(data.fieldQuality), height = 5.dp)
        }

        // ── ANS Balance ───────────────────────────────────────────────────
        SectionCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◀ PARA (REST)", fontSize = 9.sp, color = AppColors.Green, fontWeight = FontWeight.Bold)
                Text("ANS BALANCE", fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                Text("SYMPATHETIC ▶", fontSize = 9.sp, color = AppColors.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            // Gradient balance bar
            val animBalance by animateFloatAsState(data.ansBalance, tween(1000), label = "balance")
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFF00FF88), Color(0xFFFFFF00), Color(0xFFFF3333))
                    )
                )
                // Needle
                val nx = size.width * animBalance
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(nx, 0f), androidx.compose.ui.geometry.Offset(nx, size.height), strokeWidth = 3f)
            }
        }

        // ── HRV / Cortisol / Melatonin ────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AxisCard("↓", "HRV IMPACT", data.hrvImpact, Modifier.weight(1f))
            AxisCard("↑", "CORTISOL AXIS", data.cortisol, Modifier.weight(1f))
            AxisCard("↓", "MELATONIN", data.melatonin, Modifier.weight(1f))
        }

        // ── Probable symptoms ─────────────────────────────────────────────
        SectionCard {
            SubLabel("PROBABLE SYMPTOMS TODAY · RANKED BY DRIVER LOAD")
            Spacer(Modifier.height(10.dp))
            data.symptoms.forEach { s ->
                val pctColor = when { s.pct >= 60 -> AppColors.Red; s.pct >= 40 -> AppColors.Gold; s.pct >= 20 -> AppColors.Green; else -> AppColors.TextDim }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(s.emoji, fontSize = 18.sp, modifier = Modifier.width(26.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontSize = 12.sp, color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(s.driver, fontSize = 9.sp, color = AppColors.TextDim)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${s.pct}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = pctColor)
                        Text(s.level, fontSize = 8.sp, color = pctColor, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
            }
        }

        // ── Mitigation protocol ───────────────────────────────────────────
        SectionCard(borderColor = AppColors.Cyan.copy(alpha = 0.3f)) {
            SectionHeader("▸ MITIGATION PROTOCOL", AppColors.Cyan)
            Spacer(Modifier.height(10.dp))
            data.mitigationProtocol.forEach { line ->
                Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("▸", fontSize = 10.sp, color = AppColors.Cyan)
                    Text(line, fontSize = 11.sp, color = AppColors.TextSecondary, lineHeight = 16.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        // ── Narrative line ────────────────────────────────────────────────
        if (data.narrativeLine.isNotBlank()) {
            SectionCard(borderColor = AppColors.Divider) {
                SubLabel("SYSTEM NARRATIVE")
                Spacer(Modifier.height(6.dp))
                Text(data.narrativeLine, fontSize = 11.sp,
                    color = AppColors.TextSecondary, lineHeight = 16.sp)
            }
        }

        // ── Burden breakdown ──────────────────────────────────────────────
        if (data.breakdown.isNotEmpty()) {
            SectionCard {
                SectionHeader("BURDEN COMPONENT BREAKDOWN", AppColors.Magenta)
                SubLabel("MAGNITUDE · FLUCTUATION · COMBINED SCORE")
                Spacer(Modifier.height(10.dp))
                data.breakdown.entries
                    .sortedByDescending { it.value.combined }
                    .forEach { (_, c) ->
                        val combColor = when {
                            c.combined >= 60 -> AppColors.Red
                            c.combined >= 35 -> AppColors.Orange
                            c.combined >= 15 -> AppColors.Gold
                            else             -> AppColors.Green
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        c.name + if (c.value.isNotBlank()) "  (${c.value}${c.unit})" else "",
                                        fontSize = 10.sp, color = AppColors.TextPrimary
                                    )
                                    Text(
                                        "${c.combined.toInt()}%",
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = combColor
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                DataBar(c.combined / 100f, combColor, height = 4.dp)
                                Spacer(Modifier.height(1.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("M: ${c.magnitude.toInt()}%", fontSize = 8.sp, color = AppColors.TextDim)
                                    Text("F: ${c.fluctuation.toInt()}%", fontSize = 8.sp, color = AppColors.TextDim)
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
                    }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun AxisCard(arrow: String, label: String, value: String, modifier: Modifier) {
    val color = when { value == "ELEVATED" -> AppColors.Red; value == "SUPPRESSED" -> AppColors.Red; value == "REDUCED" -> AppColors.Gold; value == "VARIABLE" -> AppColors.Gold; else -> AppColors.Green }
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.CardBg)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(arrow, fontSize = 18.sp, color = color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 8.sp, color = AppColors.TextDim, letterSpacing = 1.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

fun loadColor(load: Int): Color = when { load >= 75 -> AppColors.Red; load >= 55 -> Color(0xFFFF6600); load >= 35 -> AppColors.Gold; load >= 20 -> AppColors.Green; else -> AppColors.Green }
fun fqColor(label: String): Color = when { label == "OPTIMAL" -> AppColors.Green; label == "ADEQUATE" -> AppColors.Green; label == "IMPAIRED" -> AppColors.Gold; else -> AppColors.Red }
