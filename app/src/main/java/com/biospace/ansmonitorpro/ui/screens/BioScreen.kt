package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.data.Biometrics
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun BioScreen(
    bio: Biometrics,
    watchConnected: Boolean,
    watchMac: String,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onManualUpdate: (Biometrics) -> Unit,
    onMacChange: (String) -> Unit
) {
    var showManual by remember { mutableStateOf(!watchConnected) }
    var macInput   by remember { mutableStateOf(watchMac) }

    // Manual field states — pre-populate from current bio
    var mHr     by remember { mutableStateOf(if (bio.heartRate > 0) bio.heartRate.toString() else "") }
    var mSys    by remember { mutableStateOf(if (bio.bpSys > 0) bio.bpSys.toString() else "") }
    var mDia    by remember { mutableStateOf(if (bio.bpDia > 0) bio.bpDia.toString() else "") }
    var mSpo2   by remember { mutableStateOf(if (bio.spO2 > 0) bio.spO2.toString() else "") }
    var mRmssd  by remember { mutableStateOf(if (bio.rmssd > 0f) bio.rmssd.toString() else "") }
    var mStress by remember { mutableStateOf(if (bio.stressScore > 0) bio.stressScore.toString() else "") }
    var mSleep  by remember { mutableStateOf(if (bio.sleepHours > 0f) bio.sleepHours.toString() else "") }
    var mResp   by remember { mutableStateOf(if (bio.respirationRate > 0) bio.respirationRate.toString() else "") }
    var mSteps  by remember { mutableStateOf(if (bio.steps > 0) bio.steps.toString() else "") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Watch connection ──────────────────────────────────────────────
        SectionCard(borderColor = if (watchConnected) AppColors.Green.copy(.4f) else AppColors.Divider) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    SectionHeader("Y007 BLE WATCH", if (watchConnected) AppColors.Green else AppColors.TextDim)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(if (watchConnected) AppColors.Green else AppColors.TextDim))
                        SubLabel(if (watchConnected) "CONNECTED · STREAMING" else "DISCONNECTED")
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .background((if (watchConnected) AppColors.Red else AppColors.Cyan).copy(.15f))
                        .border(1.dp, if (watchConnected) AppColors.Red else AppColors.Cyan, RoundedCornerShape(8.dp))
                        .clickable { if (watchConnected) onDisconnect() else onConnect(macInput) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (watchConnected) "DISCONNECT" else "CONNECT",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (watchConnected) AppColors.Red else AppColors.Cyan,
                        letterSpacing = 1.sp
                    )
                }
            }
            if (!watchConnected) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = macInput,
                    onValueChange = { macInput = it; onMacChange(it) },
                    label = { Text("WATCH MAC ADDRESS", fontSize = 9.sp) },
                    placeholder = { Text("XX:XX:XX:XX:XX:XX", fontSize = 11.sp, color = AppColors.TextDim) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bioFieldColors()
                )
                Spacer(Modifier.height(4.dp))
                SubLabel("Enter your Y007 BP Doctor BLE MAC address from nRF Connect")
            }
        }

        // ── Manual input toggle ───────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.CardBg)
                .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                .clickable { showManual = !showManual }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MANUAL VITALS INPUT", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (showManual) AppColors.Cyan else AppColors.TextSecondary, letterSpacing = 1.sp)
            Text(if (showManual) "▲" else "▼", fontSize = 12.sp,
                color = if (showManual) AppColors.Cyan else AppColors.TextDim)
        }

        if (showManual) {
            SectionCard {
                SubLabel("ENTER VITALS · WATCH DATA OVERRIDES WHEN CONNECTED")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BioField("HEART RATE (bpm)", mHr, { mHr = it }, Modifier.weight(1f))
                    BioField("SpO2 (%)", mSpo2, { mSpo2 = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BioField("BP SYSTOLIC", mSys, { mSys = it }, Modifier.weight(1f))
                    BioField("BP DIASTOLIC", mDia, { mDia = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BioField("HRV RMSSD (ms)", mRmssd, { mRmssd = it }, Modifier.weight(1f))
                    BioField("STRESS (0-100)", mStress, { mStress = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BioField("SLEEP (hrs)", mSleep, { mSleep = it }, Modifier.weight(1f))
                    BioField("RESP (brpm)", mResp, { mResp = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                BioField("STEPS", mSteps, { mSteps = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Cyan.copy(.15f))
                        .border(1.dp, AppColors.Cyan, RoundedCornerShape(8.dp))
                        .clickable {
                            onManualUpdate(bio.copy(
                                heartRate      = mHr.toIntOrNull()    ?: bio.heartRate,
                                bpSys          = mSys.toIntOrNull()   ?: bio.bpSys,
                                bpDia          = mDia.toIntOrNull()   ?: bio.bpDia,
                                spO2           = mSpo2.toIntOrNull()  ?: bio.spO2,
                                stressScore    = mStress.toIntOrNull() ?: bio.stressScore,
                                sleepHours     = mSleep.toFloatOrNull() ?: bio.sleepHours,
                                respirationRate= mResp.toIntOrNull()  ?: bio.respirationRate,
                                rmssd          = mRmssd.toFloatOrNull() ?: bio.rmssd,
                                steps          = mSteps.toIntOrNull() ?: bio.steps,
                                hrSource       = if (mHr.isNotBlank()) "MANUAL" else bio.hrSource,
                                bpSource       = if (mSys.isNotBlank()) "MANUAL" else bio.bpSource,
                                spO2Source     = if (mSpo2.isNotBlank()) "MANUAL" else bio.spO2Source,
                                hrvSource      = if (mRmssd.isNotBlank()) "MANUAL" else bio.hrvSource
                            ))
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▸  APPLY VITALS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = AppColors.Cyan, letterSpacing = 1.sp)
                }
            }
        }

        // ── Live readings ─────────────────────────────────────────────────
        SubLabel("CURRENT READINGS")
        Spacer(Modifier.height(0.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard2("HEART RATE", if (bio.heartRate > 0) "${bio.heartRate}" else "—", "bpm", bio.hrSource,
                when { bio.heartRate > 100 -> AppColors.Red; bio.heartRate in 1..59 -> AppColors.Gold; bio.heartRate > 0 -> AppColors.Green; else -> AppColors.TextDim }, Modifier.weight(1f))
            BioCard2("SpO2", if (bio.spO2 > 0) "${bio.spO2}" else "—", "%", bio.spO2Source,
                when { bio.spO2 in 1..92 -> AppColors.Red; bio.spO2 in 93..95 -> AppColors.Gold; bio.spO2 > 95 -> AppColors.Green; else -> AppColors.TextDim }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard2("BLOOD PRESSURE", if (bio.bpSys > 0) "${bio.bpSys}/${bio.bpDia}" else "—", "mmHg", bio.bpSource,
                when { bio.bpSys > 140 -> AppColors.Red; bio.bpSys in 1..90 -> AppColors.Gold; bio.bpSys > 0 -> AppColors.Green; else -> AppColors.TextDim }, Modifier.weight(1f))
            BioCard2("HRV RMSSD", if (bio.rmssd > 0) "${bio.rmssd.toInt()}" else "—", "ms", bio.hrvSource,
                when { bio.rmssd in 0.1f..20f -> AppColors.Red; bio.rmssd in 20f..35f -> AppColors.Gold; bio.rmssd > 35f -> AppColors.Green; else -> AppColors.TextDim }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard2("SLEEP", if (bio.sleepHours > 0) "${"%.1f".format(bio.sleepHours)}" else "—", "hrs", "WATCH",
                when { bio.sleepHours in 0.1f..5f -> AppColors.Red; bio.sleepHours in 5f..7f -> AppColors.Gold; bio.sleepHours > 7f -> AppColors.Green; else -> AppColors.TextDim }, Modifier.weight(1f))
            BioCard2("STRESS", if (bio.stressScore > 0) "${bio.stressScore}" else "—", "/100", "WATCH",
                when { bio.stressScore > 70 -> AppColors.Red; bio.stressScore > 40 -> AppColors.Gold; bio.stressScore > 0 -> AppColors.Green; else -> AppColors.TextDim }, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioCard2("STEPS", if (bio.steps > 0) "${bio.steps}" else "—", "", "WATCH", AppColors.Cyan, Modifier.weight(1f))
            BioCard2("RESP RATE", if (bio.respirationRate > 0) "${bio.respirationRate}" else "—", "brpm", "MANUAL", AppColors.Cyan, Modifier.weight(1f))
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun BioField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 8.sp, letterSpacing = 0.5.sp) },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = bioFieldColors()
    )
}

@Composable
fun bioFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AppColors.Cyan,
    unfocusedBorderColor = AppColors.Divider,
    focusedTextColor     = AppColors.TextPrimary,
    unfocusedTextColor   = AppColors.TextPrimary,
    containerColor       = AppColors.CardBg,
    cursorColor          = AppColors.Cyan,
    focusedLabelColor    = AppColors.Cyan,
    unfocusedLabelColor  = AppColors.TextDim
)

@Composable
fun BioCard2(label: String, value: String, unit: String, source: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(AppColors.CardBg2)
            .border(1.dp, AppColors.Divider, RoundedCornerShape(10.dp)).padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 8.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
            Text(source, fontSize = 7.sp, color = if (source == "WATCH") AppColors.Cyan else AppColors.Gold,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
            if (unit.isNotBlank()) Text(" $unit", fontSize = 10.sp, color = AppColors.TextDim,
                modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}
