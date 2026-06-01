package com.biospace.ansmonitorpro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biospace.ansmonitorpro.data.ChatMessage
import com.biospace.ansmonitorpro.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Local-only chat — messages persist for the session only.
 * Firebase was declared but never added to build.gradle and
 * google-services.json was a placeholder, so all Firebase calls
 * have been replaced with in-memory state until a real project
 * ID and google-services.json are provided.
 */
@Composable
fun ChatScreen(kp: Double, lastUpdated: String) {
    var messages  by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var callsign  by remember { mutableStateOf("") }
    var input     by remember { mutableStateOf("") }
    var showCallsignDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(Modifier.fillMaxSize().background(AppColors.Background)) {

        // ── Status bar ────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(AppColors.CardBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(AppColors.Green)
                )
                Text(
                    "LOCAL", fontSize = 10.sp, color = AppColors.Green,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
                Text("· SESSION CHANNEL", fontSize = 10.sp, color = AppColors.TextDim)
            }
            Text(
                callsign.ifEmpty { "ANON" },
                fontSize = 11.sp, color = AppColors.Cyan, fontWeight = FontWeight.Bold
            )
        }

        // ── Message list ──────────────────────────────────────────────────
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No messages yet.\nSet your callsign and transmit.",
                            fontSize = 12.sp, color = AppColors.TextDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            items(messages) { msg ->
                val isMe = msg.callsign.equals(callsign, ignoreCase = true)
                val timeStr = try {
                    SimpleDateFormat("HH:mm", Locale.US).format(Date(msg.timestamp))
                } catch (e: Exception) { "" }

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Text(
                        msg.callsign.uppercase(), fontSize = 10.sp, color = AppColors.Cyan,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Box(
                        Modifier.fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMe) Color(0xFF1A3A5A) else AppColors.CardBg2)
                            .border(
                                1.dp,
                                if (isMe) AppColors.Cyan.copy(0.3f) else AppColors.Divider,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(msg.message, fontSize = 13.sp, color = AppColors.TextPrimary, lineHeight = 18.sp)
                    }
                    Text(
                        timeStr, fontSize = 9.sp, color = AppColors.TextDim,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ── Auto-data footer ──────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().background(AppColors.CardBg)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "AUTO · Kp=${String.format("%.2f", kp)} · $lastUpdated",
                fontSize = 9.sp, color = AppColors.TextDim,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── Input row ─────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().background(AppColors.CardBg2)
                .padding(horizontal = 10.dp, vertical = 8.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                textStyle = TextStyle(color = AppColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(AppColors.Cyan),
                modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("transmit…", fontSize = 13.sp, color = AppColors.TextDim)
                    inner()
                }
            )

            // Callsign / ID button
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                    .clickable { showCallsignDialog = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("ID", fontSize = 11.sp, color = AppColors.TextDim, fontWeight = FontWeight.Bold)
            }

            // Send button
            val canSend = input.isNotBlank() && callsign.isNotEmpty()
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (canSend) AppColors.Cyan else AppColors.CardBg)
                    .clickable {
                        if (canSend) {
                            val msg = ChatMessage(
                                callsign    = callsign.uppercase(),
                                message     = input.trim(),
                                timestamp   = System.currentTimeMillis(),
                                kp          = String.format("%.2f", kp)
                            )
                            messages = messages + msg
                            input = ""
                            scope.launch {
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                            }
                        } else if (callsign.isEmpty()) {
                            showCallsignDialog = true
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "▶", fontSize = 14.sp,
                    color = if (canSend) AppColors.Background else AppColors.TextDim
                )
            }
        }
    }

    if (showCallsignDialog) {
        CallsignDialog(
            onDismiss = { showCallsignDialog = false },
            onConfirm = { cs ->
                callsign = cs.trim().uppercase().take(12)
                showCallsignDialog = false
            }
        )
    }
}

@Composable
fun CallsignDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(AppColors.CardBg2)
                .border(1.dp, AppColors.Divider, RoundedCornerShape(16.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "SET CALLSIGN", fontSize = 16.sp, fontWeight = FontWeight.Black,
                color = AppColors.Cyan, letterSpacing = 2.sp
            )
            Text("Your display name in the session channel.", fontSize = 13.sp, color = AppColors.TextSecondary)
            BasicTextField(
                value = text, onValueChange = { text = it },
                textStyle = TextStyle(color = AppColors.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(AppColors.Cyan),
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("e.g. SENSOR-7", fontSize = 13.sp, color = AppColors.TextDim)
                    inner()
                }
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = AppColors.TextDim, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) {
                    Text("CONFIRM", color = AppColors.Cyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
