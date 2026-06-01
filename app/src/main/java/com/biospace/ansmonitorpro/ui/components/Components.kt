package com.biospace.ansmonitorpro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.ui.theme.AppColors
import kotlin.math.*

// ── Section card (dark navy rounded card) ────────────────────────────────────
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AppColors.Divider,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        content = content
    )
}

// ── Tab pill button (matches BioSpace Monitor style) ─────────────────────────
@Composable
fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg     = if (selected) AppColors.CardBg2 else AppColors.CardBg
    val border = if (selected) AppColors.Cyan    else AppColors.TabBorder
    val text   = if (selected) AppColors.Cyan    else AppColors.TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .then(Modifier.height(IntrinsicSize.Min))
    ) {
        Text(
            label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = text,
            letterSpacing = 1.sp
        )
    }
}

// ── Circular gauge (matches the /100 gauge in BioSpace Monitor) ──────────────
@Composable
fun CircularGauge(
    value: Int,
    maxValue: Int = 100,
    color: Color = AppColors.Green,
    size: Dp = 110.dp
) {
    val animPct by animateFloatAsState(
        targetValue  = value.toFloat() / maxValue.toFloat(),
        animationSpec = tween(1000, easing = EaseInOutCubic),
        label        = "gauge"
    )
    Box(
        modifier        = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = this.size.width * 0.10f
            val r  = (this.size.width - sw) / 2f
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            // Background track
            drawArc(Color(0xFF1A2030), -90f, 360f, false, Offset(cx-r,cy-r), Size(r*2,r*2), style = Stroke(sw, cap = StrokeCap.Round))
            // Filled arc
            if (animPct > 0.001f) {
                drawArc(color, -90f, animPct * 360f, false, Offset(cx-r,cy-r), Size(r*2,r*2), style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontSize = (size.value * 0.22).sp, fontWeight = FontWeight.Black, color = color)
            Text("/$maxValue", fontSize = (size.value * 0.10).sp, color = AppColors.TextDim)
        }
    }
}

// ── Horizontal bar ────────────────────────────────────────────────────────────
@Composable
fun DataBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val animFrac by animateFloatAsState(fraction.coerceIn(0f,1f), tween(800), label="bar")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(2.dp)).background(AppColors.Divider)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animFrac).clip(RoundedCornerShape(2.dp)).background(color))
    }
}

// ── Waveform / sparkline chart ────────────────────────────────────────────────
@Composable
fun SparklineChart(
    data: List<Double>,
    color: Color = AppColors.Gold,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    if (data.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val minV = data.min(); val maxV = data.max()
        val range = if (maxV > minV) maxV - minV else 1.0
        fun xOf(i: Int)  = i / (data.size - 1).toFloat() * w
        fun yOf(v: Double) = h - ((v - minV) / range * h * 0.85f + h * 0.05f).toFloat()

        // Filled area
        if (filled) {
            val path = Path()
            path.moveTo(0f, h)
            data.forEachIndexed { i, v -> path.lineTo(xOf(i), yOf(v)) }
            path.lineTo(w, h); path.close()
            drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha=0.35f), Color.Transparent)))
        }
        // Line
        val path = Path()
        data.forEachIndexed { i, v -> if (i == 0) path.moveTo(xOf(i), yOf(v)) else path.lineTo(xOf(i), yOf(v)) }
        drawPath(path, color, style = Stroke(2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ── Sine wave animation (SR tab) ──────────────────────────────────────────────
@Composable
fun SineWaveCanvas(
    hz: Double,
    amplitude: Double,
    color: Color = AppColors.Gold,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val cx = h / 2f
        val amp = (amplitude / 3.0 * cx * 0.7).toFloat().coerceIn(5f, cx * 0.85f)
        val path = Path()
        val steps = 200
        for (i in 0..steps) {
            val x = i / steps.toFloat() * w
            val angle = (i / steps.toFloat() * 4 * Math.PI + phase).toFloat()
            val y = cx - sin(angle) * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.5f, cap = StrokeCap.Round))
        // Second harmonic (dimmer)
        val path2 = Path()
        for (i in 0..steps) {
            val x = i / steps.toFloat() * w
            val angle = (i / steps.toFloat() * 8 * Math.PI + phase * 1.5f).toFloat()
            val y = cx - sin(angle) * amp * 0.4f
            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }
        drawPath(path2, color.copy(alpha = 0.3f), style = Stroke(1.5f))
    }
}

// ── KP bar chart ──────────────────────────────────────────────────────────────
@Composable
fun KpBarChart(history: List<Double>, currentKp: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        if (history.isEmpty()) return@Canvas
        val barW = (w / history.size * 0.7f).coerceAtLeast(4f)
        val gap  = w / history.size
        history.forEachIndexed { i, kp ->
            val barH = (kp / 9.0 * h).toFloat().coerceAtLeast(2f)
            val x    = i * gap
            val color = when { kp >= 5 -> Color(0xFFFF3333); kp >= 3 -> Color(0xFFD4A843); else -> Color(0xFF00CC6A) }
            drawRect(color.copy(alpha=0.85f), Offset(x, h - barH), Size(barW, barH))
        }
        // Current Kp line
        val lineY = h - (currentKp / 9.0 * h).toFloat()
        drawLine(AppColors.Cyan, Offset(0f, lineY), Offset(w, lineY), strokeWidth = 1.5f)
    }
}

// ── 3-column metric card row ──────────────────────────────────────────────────
@Composable
fun MetricCard(label: String, value: String, unit: String, statusLabel: String, statusColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = statusColor, textAlign = TextAlign.Center)
        if (unit.isNotEmpty()) Text(unit, fontSize = 9.sp, color = AppColors.TextDim)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
        Spacer(Modifier.height(4.dp))
        Text(statusLabel, fontSize = 9.sp, color = statusColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── Label / value row ─────────────────────────────────────────────────────────
@Composable
fun LabelValueRow(label: String, value: String, valueColor: Color = AppColors.Cyan) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = AppColors.TextSecondary)
        Text(value, fontSize = 11.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

// ── Section header (cyan, monospace, letter-spaced) ───────────────────────────
@Composable
fun SectionHeader(text: String, color: Color = AppColors.Cyan) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.5.sp)
}

// ── Sub-label ─────────────────────────────────────────────────────────────────
@Composable
fun SubLabel(text: String) {
    Text(text, fontSize = 9.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
}
