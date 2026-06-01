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
import com.biospace.ansmonitorpro.data.SchumannData
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun SrScreen(data: SchumannData) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Main SR card ──────────────────────────────────────────────────
        SectionCard(borderColor = AppColors.Gold.copy(alpha = 0.3f)) {
            SectionHeader("SCHUMANN RESONANCE", AppColors.Gold)
            SubLabel("EARTH–IONOSPHERE CAVITY · GLOBAL ELF MONITOR")
            Spacer(Modifier.height(16.dp))

            // Big frequency display
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text(String.format("%.2f", data.fundamentalHz), fontSize = 44.sp,
                        fontWeight = FontWeight.Black, color = AppColors.Gold)
                    Text("Hz", fontSize = 13.sp, color = AppColors.TextDim)
                    Text("FUNDAMENTAL (f₁)", fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                }
                // Divider
                Box(Modifier.width(1.dp).height(60.dp).background(AppColors.Divider))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val driftSign = if (data.freqDrift >= 0) "+" else ""
                    Text("$driftSign${String.format("%.3f", data.freqDrift)}", fontSize = 22.sp,
                        fontWeight = FontWeight.Black, color = AppColors.Cyan)
                    Text("Hz DRIFT", fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                    Text(data.freqDriftLabel, fontSize = 9.sp, color = AppColors.Cyan, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.width(1.dp).height(60.dp).background(AppColors.Divider))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(String.format("%.2f", data.amplitudePt), fontSize = 22.sp,
                        fontWeight = FontWeight.Black, color = AppColors.Green)
                    Text("pT AMPLITUDE", fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                    Text(data.ampLabel, fontSize = 9.sp, color = AppColors.Green, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))

            // Sine wave animation
            SineWaveCanvas(
                hz = data.fundamentalHz,
                amplitude = data.amplitudePt,
                color = AppColors.Gold,
                modifier = Modifier.fillMaxWidth().height(70.dp)
            )
            Spacer(Modifier.height(4.dp))
            SubLabel("AMPLITUDE HISTORY · 3HR · SIMULATED CAVITY SIGNAL")
            Spacer(Modifier.height(14.dp))

            // Core metrics
            SectionHeader("CORE METRICS · ANS IMPACT QUANTIFICATION")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("INTENSITY", String.format("%.2f", data.amplitudePt), "pT²/Hz",
                    data.intensityLabel, ampColor(data.amplitudePt), Modifier.weight(1f))
                val driftSign = if (data.freqDrift >= 0) "+" else ""
                MetricCard("FREQ DRIFT", "$driftSign${String.format("%.3f", data.freqDrift)}", "Hz ABOVE 7.83",
                    data.freqDriftLabel, AppColors.Cyan, Modifier.weight(1f))
                MetricCard("Q-FACTOR", String.format("%.1f", data.qFactor), "COHERENCE",
                    if (data.qFactor >= 4.5) "HIGH Q" else if (data.qFactor >= 3.0) "▲ BROADBAND" else "LOW Q",
                    qColor(data.qFactor), Modifier.weight(1f))
            }
        }

        // ── Coherence / compensation threshold card ───────────────────────
        SectionCard {
            SubLabel("COMPENSATION THRESHOLD")
            Spacer(Modifier.height(8.dp))
            Text(data.coherenceLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Green)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularGauge(data.coherenceScore, 100, AppColors.Green, 100.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabelValueRow("Intensity:", "${String.format("%.2f",data.amplitudePt)} pT²/Hz")
                    val driftSign = if (data.freqDrift >= 0) "+" else ""
                    LabelValueRow("Freq drift:", "$driftSign${String.format("%.3f",data.freqDrift)} Hz")
                    LabelValueRow("Q-factor:", String.format("%.1f", data.qFactor))
                    LabelValueRow("Bz stability:", data.bzStability,
                        if (data.bzStability.contains("STABLE")) AppColors.Green else AppColors.Gold)
                }
            }
        }

        // ── TEC coupling card ─────────────────────────────────────────────
        SectionCard {
            val tecSign = if (data.tecDelta >= 0) "+" else ""
            SectionHeader("▸ TEC → CAVITY COUPLING", AppColors.Cyan)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LOCAL TEC", fontSize = 10.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${String.format("%.1f",data.tecLocal)} TECU", fontSize = 12.sp,
                        color = AppColors.Cyan, fontWeight = FontWeight.Bold)
                    Text("$tecSign${String.format("%.1f",data.tecDelta)} FROM MEDIAN →",
                        fontSize = 10.sp, color = AppColors.TextDim)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CAVITY HEIGHT", fontSize = 10.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                Text(data.cavityHeight, fontSize = 11.sp, color = cavityColor(data.cavityHeight), fontWeight = FontWeight.Bold)
            }
        }

        // ── Harmonic ANS pathway table ────────────────────────────────────
        SectionCard {
            SectionHeader("HARMONIC → ANS PATHWAY TABLE")
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("f (Hz)", fontSize = 8.sp, color = AppColors.TextDim, modifier = Modifier.width(42.dp))
                Text("RANGE", fontSize = 8.sp, color = AppColors.TextDim, modifier = Modifier.width(58.dp))
                Text("BRAIN", fontSize = 8.sp, color = AppColors.TextDim, modifier = Modifier.width(70.dp))
                Text("PRIMARY ANS EFFECT", fontSize = 8.sp, color = AppColors.TextDim)
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
            Spacer(Modifier.height(4.dp))
            val harmonics = listOf(
                HarmonicRow("7.83",  "7.0–8.5",  "Theta/α",      AppColors.Gold,    "HRV coherence · Sleep architecture · Parasympathetic baseline · Circadian rhythm anchor"),
                HarmonicRow("14.3",  "13–15",    "Low Beta/SMR", AppColors.Magenta,  "Motor cortex excitability · Cortisol axis sensitization · Jaw clenching · Muscle tension"),
                HarmonicRow("20.8",  "19–22",    "Mid Beta",     AppColors.Cyan,     "Hypervigilance · Thought loop amplification · Sensory filter degradation · Anxiety tone"),
                HarmonicRow("26.4",  "25–28",    "High Beta",    AppColors.Orange,   "Adrenaline axis · Tachycardia · Startle amplification · Vagal tone suppression"),
                HarmonicRow("33.0",  "32–35",    "Gamma boundary",AppColors.Red,     "Sensory binding disruption · Tinnitus · Visual processing artifacts · Temporal disorientation"),
                HarmonicRow("39.5",  "38–41",    "Gamma",        AppColors.MagentaDim,"Pineal axis · Melatonin suppression · Circadian phase shift · Compounded sleep debt"),
            )
            harmonics.forEach { h ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(h.freq, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = h.color,
                        modifier = Modifier.width(42.dp))
                    Text(h.range, fontSize = 10.sp, color = AppColors.TextSecondary,
                        modifier = Modifier.width(58.dp))
                    Text(h.brain, fontSize = 10.sp, color = AppColors.TextSecondary,
                        modifier = Modifier.width(70.dp))
                    Text(h.effect, fontSize = 10.sp, color = AppColors.TextDim, lineHeight = 14.sp,
                        modifier = Modifier.weight(1f))
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

data class HarmonicRow(val freq: String, val range: String, val brain: String, val color: Color, val effect: String)

fun ampColor(amp: Double): Color = when { amp > 2.0 -> AppColors.Orange; amp > 1.5 -> AppColors.Green; amp > 0.8 -> AppColors.Cyan; else -> AppColors.Red }
fun qColor(q: Double): Color = when { q >= 5.0 -> AppColors.Green; q >= 3.5 -> AppColors.Cyan; q >= 2.5 -> AppColors.Gold; else -> AppColors.Red }
fun cavityColor(s: String): Color = when { s.contains("NOMINAL") -> AppColors.Green; s.contains("SLIGHTLY") -> AppColors.Gold; else -> AppColors.Orange }
