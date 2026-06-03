package com.biospace.ansmonitorpro.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.*
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private data class SymptomField(val key: String, val label: String, val emoji: String)

private val FIELDS = listOf(
    SymptomField("lightheadedness", "Lightheadedness", "💫"),
    SymptomField("heartPounding",   "Heart Pounding",  "💓"),
    SymptomField("fatigue",         "Fatigue",         "😴"),
    SymptomField("brainFog",        "Brain Fog",       "🌫"),
    SymptomField("chestPain",       "Chest Discomfort","🫀"),
    SymptomField("nausea",          "Nausea",          "🤢"),
    SymptomField("shortBreath",     "Short of Breath", "🫁"),
    SymptomField("tremors",         "Tremors",         "🫨"),
    SymptomField("blurredVision",   "Blurred Vision",  "👁"),
    SymptomField("headache",        "Headache",        "🤕")
)

private fun exportSymptomLogs(context: Context, logs: List<SymptomLog>): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
    val paint = android.graphics.Paint().apply { textSize = 11f; isAntiAlias = true }
    val titlePaint = android.graphics.Paint().apply { textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
    val headPaint = android.graphics.Paint().apply { textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
    val pageWidth = 595; val pageHeight = 842; val margin = 40f
    val pdf = android.graphics.pdf.PdfDocument()
    var pageNum = 1
    var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
    var page = pdf.startPage(pageInfo)
    var canvas = page.canvas
    var y = margin

    fun newPage() {
        pdf.finishPage(page)
        pageNum++
        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        page = pdf.startPage(pageInfo)
        canvas = page.canvas
        y = margin
    }
    fun checkY(needed: Float) { if (y + needed > pageHeight - margin) newPage() }
    fun drawLine(text: String, p: android.graphics.Paint = paint, indent: Float = 0f) {
        checkY(p.textSize + 4f)
        canvas.drawText(text, margin + indent, y, p)
        y += p.textSize + 4f
    }
    fun drawRule() {
        checkY(8f)
        canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        y += 8f
    }

    drawLine("ANS TRIGGER PRO — SYMPTOM LOG", titlePaint)
    drawLine("Generated: ${sdf.format(java.util.Date())}   Entries: ${logs.size}", paint)
    drawRule()

    logs.forEach { log ->
        checkY(20f)
        drawLine("${sdf.format(java.util.Date(log.timestamp))}  |  Burden: ${log.burdenAtLog}%  |  Alert: ${log.alertLevelAtLog}", headPaint)
        drawLine("SYMPTOMS", headPaint, 8f)
        listOf(
            "Lightheadedness: ${log.lightheadedness}/10",
            "Heart Pounding: ${log.heartPounding}/10",
            "Fatigue: ${log.fatigue}/10",
            "Brain Fog: ${log.brainFog}/10",
            "Chest Discomfort: ${log.chestPain}/10",
            "Nausea: ${log.nausea}/10",
            "Short of Breath: ${log.shortBreath}/10",
            "Tremors: ${log.tremors}/10",
            "Blurred Vision: ${log.blurredVision}/10",
            "Headache: ${log.headache}/10"
        ).forEach { drawLine(it, paint, 16f) }
        val total = log.lightheadedness + log.heartPounding + log.fatigue + log.brainFog +
            log.chestPain + log.nausea + log.shortBreath + log.tremors + log.blurredVision + log.headache
        drawLine("Avg Severity: ${"%.1f".format(total / 10.0)}/10", headPaint, 16f)
        if (log.notes.isNotBlank()) drawLine("Notes: ${log.notes}", paint, 16f)
        drawLine("SPACE WEATHER", headPaint, 8f)
        drawLine("Kp: ${"%.1f".format(log.kpAtLog)}  Bz: ${"%.1f".format(log.bzAtLog)} nT  Wind: ${log.solarWindAtLog.toInt()} km/s  Storm: ${log.stormLevelAtLog}", paint, 16f)
        drawLine("HSS: ${if (log.hssAtLog) "YES" else "No"}  SEP: ${if (log.sepAtLog) "YES" else "No"}  GST: ${if (log.gstAtLog) "YES" else "No"}", paint, 16f)
        drawLine("SCHUMANN", headPaint, 8f)
        drawLine("Freq: ${"%.2f".format(log.schumannHzAtLog)} Hz  Q: ${"%.1f".format(log.schumannQAtLog)}  Amp: ${"%.2f".format(log.schumannAmpAtLog)} pT", paint, 16f)
        drawLine("ENVIRONMENT", headPaint, 8f)
        drawLine("Temp: ${log.tempAtLog}°F  HI: ${log.heatIndexAtLog}°F  Humidity: ${log.humidAtLog}%  Pressure: ${"%.1f".format(log.pressureAtLog)} hPa  ΔP: ${"%.1f".format(log.pressureDeltaAtLog)} hPa/hr", paint, 16f)
        drawRule()
    }
    pdf.finishPage(page)

    val filename = "ANSTriggerPro_SymptomLog_${java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())}.pdf"
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> pdf.writeTo(os) } }
        } else {
            val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), filename)
            java.io.FileOutputStream(file).use { pdf.writeTo(it) }
        }
        pdf.close()
        "Saved to Downloads/$filename"
    } catch (e: Exception) {
        pdf.close()
        "Error: ${e.message}"
    }
}

@Composable
fun SymptomsScreen(
    logs: List<SymptomLog>,
    space: SpaceWeatherData,
    schumann: SchumannData,
    env: EnvData,
    ans: AnsData,
    onLog: (SymptomLog) -> Unit
) {
    val scores = remember { mutableStateMapOf<String, Int>().apply { FIELDS.forEach { put(it.key, 0) } } }
    var notes  by remember { mutableStateOf("") }
    var saved  by remember { mutableStateOf(false) }
    var exportMsg by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("SEDENTARY") }
    var activeDuration by remember { mutableStateOf("<2hrs") }
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        SectionCard(borderColor = AppColors.Magenta.copy(.3f)) {
            SectionHeader("SYMPTOM TRACKER", AppColors.Magenta)
            SubLabel("RATE SYMPTOMS · SPACE/ENV CONDITIONS AUTO-CAPTURED AT LOG TIME")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("Kp", "${"%.1f".format(space.kp)}", AppColors.Cyan, Modifier.weight(1f))
                InfoChip("BURDEN", "${ans.loadIndex}%", loadColor(ans.loadIndex), Modifier.weight(1f))
                InfoChip("Bz", "${"%.1f".format(space.bz)}", if (space.bz < -3) AppColors.Red else AppColors.TextSecondary, Modifier.weight(1f))
                InfoChip("TIME", SimpleDateFormat("HH:mm", Locale.US).format(Date()), AppColors.TextSecondary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("TEMP", "${env.tempF}°F", AppColors.TextSecondary, Modifier.weight(1f))
                InfoChip("HI", "${env.heatIndex}°F", if (env.heatIndex > 90) AppColors.Orange else AppColors.TextSecondary, Modifier.weight(1f))
                InfoChip("HUMID", "${env.humidity}%", if (env.humidity > 75) AppColors.Gold else AppColors.TextSecondary, Modifier.weight(1f))
                InfoChip("SR", "${"%.2f".format(schumann.fundamentalHz)}Hz", AppColors.TextSecondary, Modifier.weight(1f))
            }
        }

        // ── Activity Context ──────────────────────────────────────────────
        SectionCard(borderColor = AppColors.Cyan.copy(.3f)) {
            SectionHeader("ACTIVITY CONTEXT", AppColors.Cyan)
            SubLabel("TODAY'S PHYSICAL ACTIVITY LEVEL")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("SEDENTARY","LIGHT","NORMAL","EXERTIONAL").forEach { level ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (activityLevel == level) AppColors.Cyan.copy(.25f) else AppColors.Surface)
                            .border(1.dp, if (activityLevel == level) AppColors.Cyan else AppColors.TextDim.copy(.3f), RoundedCornerShape(8.dp))
                            .clickable { activityLevel = level }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(level, fontSize = 9.sp, color = if (activityLevel == level) AppColors.Cyan else AppColors.TextDim, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SubLabel("ACTIVE DURATION TODAY")
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("<2hrs","2-4hrs","4-6hrs","Most of day").forEach { dur ->
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (activeDuration == dur) AppColors.Gold.copy(.25f) else AppColors.Surface)
                            .border(1.dp, if (activeDuration == dur) AppColors.Gold else AppColors.TextDim.copy(.3f), RoundedCornerShape(8.dp))
                            .clickable { activeDuration = dur }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(dur, fontSize = 9.sp, color = if (activeDuration == dur) AppColors.Gold else AppColors.TextDim, fontWeight = FontWeight.Bold) }
                }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(field.emoji, fontSize = 16.sp)
                            Text(field.label, fontSize = 12.sp, color = AppColors.TextPrimary)
                        }
                        Text(if (v == 0) "—" else "$v", fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
                    }
                    Spacer(Modifier.height(4.dp))
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
            Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.CardBg2).border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))) {
                androidx.compose.foundation.text.BasicTextField(
                    value = notes,
                    onValueChange = { notes = it; saved = false },
                    textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.TextPrimary, fontSize = 13.sp, lineHeight = 18.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Cyan),
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) Text("Describe symptoms, triggers, context…", fontSize = 12.sp, color = AppColors.TextDim)
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
                            lightheadedness  = scores["lightheadedness"] ?: 0,
                            heartPounding    = scores["heartPounding"] ?: 0,
                            fatigue          = scores["fatigue"] ?: 0,
                            brainFog         = scores["brainFog"] ?: 0,
                            chestPain        = scores["chestPain"] ?: 0,
                            nausea           = scores["nausea"] ?: 0,
                            shortBreath      = scores["shortBreath"] ?: 0,
                            tremors          = scores["tremors"] ?: 0,
                            blurredVision    = scores["blurredVision"] ?: 0,
                            headache         = scores["headache"] ?: 0,
                            notes            = notes,
                            kpAtLog          = space.kp,
                            bzAtLog          = space.bz,
                            solarWindAtLog   = space.solarWindSpeed,
                            hssAtLog         = space.hssActive,
                            sepAtLog         = space.sepActive,
                            gstAtLog         = space.gstActive,
                            stormLevelAtLog  = space.stormG,
                            schumannHzAtLog  = schumann.fundamentalHz,
                            schumannQAtLog   = schumann.qFactor,
                            schumannAmpAtLog = schumann.amplitudePt,
                            tempAtLog        = env.tempF,
                            humidAtLog       = env.humidity,
                            pressureAtLog    = env.pressureHpa,
                            pressureDeltaAtLog = env.pressureDelta,
                            heatIndexAtLog   = env.heatIndex,
                            burdenAtLog      = ans.loadIndex,
                            alertLevelAtLog  = ans.alertLevel.name,
                            activityLevel    = activityLevel,
                            activeDuration   = activeDuration
                        ))
                        saved = true
                        exportMsg = ""
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

        // ── Download button ───────────────────────────────────────────────
        if (logs.isNotEmpty()) {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.Cyan.copy(.1f))
                    .border(1.dp, AppColors.Cyan, RoundedCornerShape(10.dp))
                    .clickable { exportMsg = exportSymptomLogs(context, logs) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⬇  DOWNLOAD SYMPTOM LOG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.Cyan, letterSpacing = 1.sp)
            }
            if (exportMsg.isNotBlank()) {
                Text(exportMsg, fontSize = 10.sp, color = if (exportMsg.startsWith("Error")) AppColors.Red else AppColors.Green, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
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
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(dt, fontSize = 11.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text("Kp ${"%.1f".format(log.kpAtLog)} · Bz ${"%.1f".format(log.bzAtLog)}nT · Burden ${log.burdenAtLog}% · ${log.alertLevelAtLog}", fontSize = 9.sp, color = AppColors.TextDim)
                                Text("${log.tempAtLog}°F · HI ${log.heatIndexAtLog}°F · ${log.humidAtLog}% humid · ΔP ${"%.1f".format(log.pressureDeltaAtLog)}hPa · SR ${"%.2f".format(log.schumannHzAtLog)}Hz", fontSize = 9.sp, color = AppColors.TextDim)
                                if (log.hssAtLog || log.sepAtLog || log.gstAtLog)
                                    Text("${if (log.hssAtLog) "HSS " else ""}${if (log.sepAtLog) "SEP " else ""}${if (log.gstAtLog) "GST" else ""}".trim(), fontSize = 9.sp, color = AppColors.Orange)
                                if (log.notes.isNotBlank())
                                    Text(log.notes.take(60) + if (log.notes.length > 60) "…" else "", fontSize = 9.sp, color = AppColors.TextDim)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$avg/10", fontSize = 18.sp, fontWeight = FontWeight.Black, color = symptomColor(avg))
                                Text("AVG", fontSize = 8.sp, color = AppColors.TextDim)
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
                    }
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
    v >= 7 -> AppColors.Red
    v >= 4 -> AppColors.Orange
    v >= 2 -> AppColors.Gold
    v >= 1 -> AppColors.Green
    else   -> AppColors.TextDim
}
