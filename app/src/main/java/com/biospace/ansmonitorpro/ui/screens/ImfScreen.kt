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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.SpaceWeatherData
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun ImfScreen(data: SpaceWeatherData) {
    var subTab by remember { mutableStateOf("BZ") }
    val subTabs = listOf("BZ", "BT", "COMPS", "ALL")

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard {
            // Header
            SubLabel("// INTERPLANETARY MAGNETIC FIELD")
            Text("IMF", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppColors.TextPrimary)
            Text("· 7-DAY", fontSize = 10.sp, color = AppColors.Cyan, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            // Sub-tabs
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                subTabs.forEach { t ->
                    val sel = subTab == t
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) AppColors.Cyan.copy(alpha=0.15f) else AppColors.CardBg2)
                            .border(1.dp, if (sel) AppColors.Cyan else AppColors.TabBorder, RoundedCornerShape(8.dp))
                            .clickable { subTab = t }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (sel) AppColors.Cyan else AppColors.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Main value
            when (subTab) {
                "BZ" -> ImfBzContent(data)
                "BT" -> ImfBtContent(data)
                "COMPS" -> ImfCompsContent(data)
                "ALL" -> { ImfBzContent(data); Spacer(Modifier.height(12.dp)); ImfBtContent(data) }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ImfBzContent(data: SpaceWeatherData) {
    val sign = if (data.bz >= 0) "+" else ""
    val color = bzColor(data.bz)

    Text("$sign${String.format("%.2f",data.bz)} nT", fontSize = 36.sp, fontWeight = FontWeight.Black, color = color)
    Spacer(Modifier.height(6.dp))

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("▲", fontSize = 12.sp, color = color)
        Text(data.bzLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
    }
    Spacer(Modifier.height(12.dp))

    // Waveform
    Box(
        Modifier.fillMaxWidth().height(90.dp)
            .clip(RoundedCornerShape(8.dp)).background(Color(0xFF060810))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        SparklineChart(data.bzHistory, color, Modifier.fillMaxSize(), filled = true)
    }
    Spacer(Modifier.height(4.dp))
    SubLabel("Bz · 7-DAY HISTORY · SOUTHWARD = GEOEFFECTIVE")
    Spacer(Modifier.height(10.dp))

    // Interpretation
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(AppColors.CardBg2).border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        val narrative = when {
            data.bz < -10 -> "STRONGLY SOUTHWARD: Maximum geoeffective state. Magnetopause is being driven earthward. Severe geomagnetic coupling. Maximum autonomic stress expected. Horizontal rest and full management protocols indicated."
            data.bz < -5  -> "SOUTHWARD: Active geoeffective state. Sustained southward Bz drives geomagnetic storms. HRV suppression and vagal withdrawal are likely. Reduce orthostatic challenge."
            data.bz < -1  -> "SLIGHTLY SOUTHWARD: Mild geoeffective coupling. Geosensitive individuals may notice fatigue, headache, or HRV changes. Monitor for further southward excursion."
            data.bz < 1   -> "UNSTABLE: Near-zero Bz is the most unpredictable state. Any southward excursion can rapidly become geoeffective. Monitor closely. Diaphragmatic 0.1Hz breathing recommended for HRV stabilization."
            data.bz < 5   -> "SLIGHTLY NORTHWARD: Low geoeffective state. Magnetosphere partially sealed. Minor ANS burden from this component."
            else          -> "NORTHWARD: Magnetosphere sealed. This is the most favorable IMF orientation. Geomagnetic coupling is minimal. ANS recovery window from IMF perspective."
        }
        Text(narrative, fontSize = 12.sp, color = AppColors.TextSecondary, lineHeight = 17.sp)
    }
}

@Composable
fun ImfBtContent(data: SpaceWeatherData) {
    Text("${String.format("%.2f",data.bt)} nT", fontSize = 36.sp, fontWeight = FontWeight.Black, color = AppColors.Cyan)
    Spacer(Modifier.height(4.dp))
    Text("TOTAL IMF MAGNITUDE", fontSize = 11.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier.fillMaxWidth().height(90.dp)
            .clip(RoundedCornerShape(8.dp)).background(Color(0xFF060810))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp)).padding(8.dp)
    ) {
        SparklineChart(data.bzHistory.map { kotlin.math.abs(it) }, AppColors.Cyan, Modifier.fillMaxSize())
    }
    Spacer(Modifier.height(4.dp))
    SubLabel("Bt · TOTAL FIELD STRENGTH")
}

@Composable
fun ImfCompsContent(data: SpaceWeatherData) {
    val sign = if (data.bz >= 0) "+" else ""
    listOf(
        Triple("Bz (North/South)", "$sign${String.format("%.2f",data.bz)} nT", bzColor(data.bz)),
        Triple("Bt (Total Magnitude)", "${String.format("%.2f",data.bt)} nT", AppColors.Cyan),
        Triple("Speed", "${data.solarWindSpeed.toInt()} km/s", AppColors.Gold),
        Triple("Density", "${String.format("%.1f",data.solarWindDensity)} p/cm³", AppColors.Green),
    ).forEach { (label, value, color) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, color = AppColors.TextSecondary)
            Text(value, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(AppColors.Divider))
    }
}
