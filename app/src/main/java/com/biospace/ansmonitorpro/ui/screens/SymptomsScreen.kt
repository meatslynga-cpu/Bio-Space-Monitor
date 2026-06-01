package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.SymptomLog
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

private data class SymptomField(val key: String, val label: String, val emoji: String)

private val FIELDS = listOf(
    SymptomField("lightheadedness", "Lightheadedness",  "💫"),
    SymptomField("heartPounding",   "Heart Pounding",   "💓"),
    SymptomField("fatigue",         "Fatigue",          "😴"),
    SymptomField("brainFog",        "Brain Fog",        "🌫"),
    SymptomField("chestPain",       "Chest Discomfort", "🫀"),
    SymptomField("nausea",          "Nausea",           "🤢"),
    SymptomField("shortBreath",     "Short of Breath",  "🫁"),
    SymptomField("tremors",         "Tremors",          "🫨"),
    SymptomField("blurredVision",   "Blurred Vision",   "👁"),
    SymptomField("headache",        "Headache",         "🤕")
)

@Composable
fun SymptomsScreen(
    logs: List<SymptomLog>,
    currentKp: Double,
    currentBurden: Int,
    onLog: (SymptomLog) -> Unit
) {
    val scores = remember { mutableStateMapOf<String, Int>().apply { FIELDS.forEach { put(it.key, 0) } } }
    var notes  by remember { mutableStateOf("") }
    var saved  by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        SectionCard(borderColor = AppColors.Magenta.copy(.3f)) {
            SectionHeader("SYMPTOM LOG", AppColors.Magenta)
            SubLabel("RATE EACH SYMPTOM 0–10 · CORRELATED WITH LIVE KP & ANS BURDEN")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("Kp", "${"%.1f".format(currentKp)}", AppColors.Cyan, Modifier.weight(1f))
                InfoChip("BURDEN", "${currentBurden}%", loadColor(currentBurden), Modifier.weight(1f))
                InfoChip("TIME", SimpleDateFormat("HH:mm", Locale.US).format(Date()), AppColors.TextSecondary, Modifier.weight(1f))
            }
        }

        // ── Symptom sliders ───────────────────────────────────────────────
        SectionCard {
            SubLabel("SYMPTOM SEVERITY")
            Spacer(Modifier.height(10.dp))
            FIELDS.forEach { field ->
                val v = scores[field.key] ?: 0
                val color = symptomColor(v)
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(field.emoji, fontSize = 16.sp)
                            Text(field.label, fontSize = 12.sp, color = AppColors.TextPrimary)
                        }
                        Text(
                            if (v == 0) "—" else "$v",
                            fontSize = 16.sp, fontWeight = FontWeight.Black, color = color
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    // 0–10 tap bar
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        (0..10).forEach { n ->
                            val bg = if (n <= v && v > 0) color.copy(alpha = .85f) else AppColors.CardBg2
                            Box(
                                Modifier.weight(1f).height(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(bg)
                                    .border(1.dp, if (n == v) color else AppColors.Divider, RoundedCornerShape(3.dp))
                                    .clickable { scores[field.key] = n; saved = false },
                                contentAlignment = Alignment.Center
                            ) {
                                if (n == v && v > 0)
                                    Text("$n", fontSize = 7.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("None", fontSize = 8.sp, color = AppColors.TextDim)
                        Text("Severe", fontSize = 8.sp, color = AppColors.TextDim)
                    }
                }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
                Spacer(Modifier.height(2.dp))
            }
        }

        // ── Notes ─────────────────────────────────────────────────────────
        SectionCard {
            SubLabel("NOTES (OPTIONAL)")
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg2)
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = notes,
                    onValueChange = { notes = it; saved = false },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = AppColors.TextPrimary, fontSize = 13.sp, lineHeight = 18.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Cyan),
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) Text("Describe symptoms, triggers, context…",
                            fontSize = 12.sp, color = AppColors.TextDim)
                        inner()
                    }
                )
            }
        }

        // ── Save button ───────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (saved) AppColors.Green.copy(.15f) else AppColors.Magenta.copy(.15f))
                .border(1.dp, if (saved) AppColors.Green else AppColors.Magenta, RoundedCornerShape(10.dp))
                .clickable {
                    if (!saved) {
                        onLog(SymptomLog(
                            lightheadedness = scores["lightheadedness"] ?: 0,
                            heartPounding   = scores["heartPounding"] ?: 0,
                            fatigue         = scores["fatigue"] ?: 0,
                            brainFog        = scores["brainFog"] ?: 0,
                            chestPain       = scores["chestPain"] ?: 0,
                            nausea          = scores["nausea"] ?: 0,
                            shortBreath     = scores["shortBreath"] ?: 0,
                            tremors         = scores["tremors"] ?: 0,
                            blurredVision   = scores["blurredVision"] ?: 0,
                            headache        = scores["headache"] ?: 0,
                            notes           = notes,
                            kpAtLog         = currentKp,
                            burdenAtLog     = currentBurden
                        ))
                        saved = true
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (saved) "✓  LOGGED" else "▸  SAVE LOG ENTRY",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (saved) AppColors.Green else AppColors.Magenta,
                letterSpacing = 1.sp
            )
        }

        // ── Log history ───────────────────────────────────────────────────
        if (logs.isNotEmpty()) {
            SectionCard {
                SubLabel("RECENT LOG HISTORY")
                Spacer(Modifier.height(8.dp))
                logs.take(10).forEach { log ->
                    val dt = SimpleDateFormat("MMM d · HH:mm", Locale.US).format(Date(log.timestamp))
                    val total = log.lightheadedness + log.heartPounding + log.fatigue +
                        log.brainFog + log.chestPain + log.nausea + log.shortBreath +
                        log.tremors + log.blurredVision + log.headache
                    val avg = if (total > 0) total / 10 else 0
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dt, fontSize = 11.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("Kp ${"%.1f".format(log.kpAtLog)} · Burden ${log.burdenAtLog}%",
                                fontSize = 9.sp, color = AppColors.TextDim)
                            if (log.notes.isNotBlank())
                                Text(log.notes.take(50) + if (log.notes.length > 50) "…" else "",
                                    fontSize = 9.sp, color = AppColors.TextDim)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("$avg/10", fontSize = 18.sp, fontWeight = FontWeight.Black,
                                color = symptomColor(avg))
                            Text("AVG", fontSize = 8.sp, color = AppColors.TextDim)
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
fun InfoChip(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 8.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
    }
}

fun symptomColor(v: Int): Color = when {
    v >= 7  -> AppColors.Red
    v >= 4  -> AppColors.Orange
    v >= 2  -> AppColors.Gold
    v >= 1  -> AppColors.Green
    else    -> AppColors.TextDim
}
