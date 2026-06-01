package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biospace.ansmonitorpro.ui.components.*
import com.biospace.ansmonitorpro.ui.theme.AppColors

@Composable
fun ReportScreen(
    reportOutput: String,
    reportLoading: Boolean,
    hasGeminiKey: Boolean,
    onGenerate: (clinical: Boolean) -> Unit
) {
    var clinical by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        SectionCard(borderColor = AppColors.Cyan.copy(.3f)) {
            SectionHeader("AI HEALTH REPORT", AppColors.Cyan)
            SubLabel("POWERED BY GEMINI 2.5 FLASH · HELIOBIOLOGICAL ANALYSIS")
            Spacer(Modifier.height(10.dp))
            Text(
                "Generates a comprehensive report analyzing your current ANS burden, space weather drivers, biometric readings, and symptom forecasts.",
                fontSize = 12.sp, color = AppColors.TextSecondary, lineHeight = 18.sp
            )
        }

        if (!hasGeminiKey) {
            SectionCard(borderColor = AppColors.Gold.copy(.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚠", fontSize = 18.sp)
                    Column {
                        Text("GEMINI API KEY REQUIRED", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = AppColors.Gold)
                        Spacer(Modifier.height(2.dp))
                        Text("Add your free Gemini key in Settings to enable AI reports.",
                            fontSize = 11.sp, color = AppColors.TextSecondary)
                    }
                }
            }
        }

        // ── Report style toggle ───────────────────────────────────────────
        SectionCard {
            SubLabel("REPORT STYLE")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StyleButton("GENERAL", !clinical, { clinical = false }, Modifier.weight(1f))
                StyleButton("CLINICAL", clinical, { clinical = true }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (clinical) "Includes physiological mechanisms, HRV analysis, and research context."
                else "Plain language — suitable for sharing with family or non-specialist providers.",
                fontSize = 10.sp, color = AppColors.TextDim, lineHeight = 14.sp
            )
        }

        // ── Generate button ───────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (reportLoading) AppColors.CardBg else AppColors.Cyan.copy(.15f))
                .border(1.dp, if (reportLoading) AppColors.TextDim else AppColors.Cyan, RoundedCornerShape(10.dp))
                .clickable(enabled = !reportLoading && hasGeminiKey) { onGenerate(clinical); copied = false }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (reportLoading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = AppColors.Cyan, strokeWidth = 2.dp)
                    Text("GENERATING REPORT…", fontSize = 11.sp, color = AppColors.TextDim, letterSpacing = 1.sp)
                }
            } else {
                Text(
                    "▸  GENERATE ${if (clinical) "CLINICAL" else "GENERAL"} REPORT",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (hasGeminiKey) AppColors.Cyan else AppColors.TextDim,
                    letterSpacing = 1.sp
                )
            }
        }

        // ── Report output ─────────────────────────────────────────────────
        if (reportOutput.isNotBlank()) {
            // Copy button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(AppColors.CardBg)
                        .border(1.dp, AppColors.Divider, RoundedCornerShape(6.dp))
                        .clickable { clipboard.setText(AnnotatedString(reportOutput)); copied = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (copied) "✓ COPIED" else "COPY",
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = if (copied) AppColors.Green else AppColors.TextDim,
                        letterSpacing = 1.sp
                    )
                }
            }
            SectionCard(borderColor = AppColors.Divider) {
                Text(
                    reportOutput,
                    fontSize = 12.sp, color = AppColors.TextPrimary,
                    lineHeight = 19.sp
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun StyleButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AppColors.Cyan.copy(.15f) else AppColors.CardBg)
            .border(1.dp, if (selected) AppColors.Cyan else AppColors.Divider, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (selected) AppColors.Cyan else AppColors.TextSecondary,
            letterSpacing = 1.sp
        )
    }
}
