package com.apolloauto.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apolloauto.player.ApolloApp
import com.apolloauto.player.echohunt.EchoHuntEngineStatus
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette
import com.apolloauto.player.ui.hud.ApolloStylePanel

/**
 * Settings → ECHOHUNT — AcoustID application key + library scan controls.
 * Never stores MusicBrainz account passwords.
 */
@Composable
fun EchoHuntSettingsPanel() {
    val app = ApolloApp.instance
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    val interfaceStyle by app.themeStore.interfaceStyle.collectAsState()
    val storedKey by app.echoHuntCredentials.acoustIdApplicationKey.collectAsState()
    val scan by app.echoHuntScan.state.collectAsState()
    val tracks by app.libraryStore.tracks.collectAsState()
    var draftKey by remember(storedKey) { mutableStateOf(storedKey.orEmpty()) }
    var reveal by remember { mutableStateOf(false) }

    ApolloStylePanel(title = "ECHOHUNT / ACOUSTID", interfaceStyle = interfaceStyle) {
        Text(
            "Identify local files via Chromaprint fingerprints + AcoustID. " +
                "Not a Shazam-style mic recognizer.",
            color = p.dim,
            fontFamily = body,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Enable song scanning", color = p.soft, fontFamily = body, fontSize = 14.sp)
                Text(
                    "Opt-in · private until you scan",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = scan.enabled,
                onCheckedChange = { app.echoHuntScan.setEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.accent,
                    checkedTrackColor = p.accent.copy(alpha = 0.45f),
                    uncheckedThumbColor = p.dim,
                    uncheckedTrackColor = p.dim.copy(alpha = 0.35f)
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("AcoustID application API key", color = p.soft, fontFamily = body, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = draftKey,
            onValueChange = { draftKey = it },
            singleLine = true,
            visualTransformation = if (reveal) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = p.soft,
                fontFamily = body,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(p.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, p.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(p.voidBlack.copy(alpha = 0.45f))
                .padding(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HuntChip(if (reveal) "HIDE" else "SHOW") { reveal = !reveal }
            HuntChip("SAVE KEY") {
                app.echoHuntCredentials.setAcoustIdApplicationKey(draftKey)
                app.echoHuntScan.refreshStatus()
            }
            HuntChip("CLEAR") {
                draftKey = ""
                app.echoHuntCredentials.setAcoustIdApplicationKey(null)
                app.echoHuntScan.refreshStatus()
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Status: ${scan.status.name}",
            color = when (scan.status) {
                EchoHuntEngineStatus.ERROR -> p.danger
                EchoHuntEngineStatus.NATIVE_FINGERPRINT_MISSING -> p.accent
                EchoHuntEngineStatus.KEY_CONFIGURED,
                EchoHuntEngineStatus.IDLE_WITH_RESULTS -> p.primary
                else -> p.dim
            },
            fontFamily = body,
            fontSize = 12.sp
        )
        Text(scan.lastMessage, color = p.dim, fontFamily = body, fontSize = 11.sp)
        scan.lastError?.let {
            Text(it, color = p.danger.copy(alpha = 0.85f), fontFamily = body, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Library: ${tracks.size} track(s)",
            color = p.soft,
            fontFamily = body,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HuntChip("SCAN LIBRARY") {
                app.echoHuntScan.setEnabled(true)
                app.echoHuntScan.startLibraryScan(tracks)
            }
            HuntChip("CANCEL") { app.echoHuntScan.cancel() }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Get a free application API key at acoustid.org/webservice. " +
                "Do not paste MusicBrainz account passwords here.",
            color = p.dim,
            fontFamily = body,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HuntChip(label: String, onClick: () -> Unit) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Text(
        label,
        color = p.soft,
        fontFamily = body,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, p.primary.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}
