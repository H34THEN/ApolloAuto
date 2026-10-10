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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.apolloauto.player.data.M3uImportPreview
import com.apolloauto.player.theme.LocalBodyFont
import com.apolloauto.player.theme.LocalHudPalette

@Composable
fun M3uImportConfirmDialog(
    preview: M3uImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val p = LocalHudPalette.current
    val body = LocalBodyFont.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, p.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .background(p.panel.copy(alpha = 0.98f))
                .padding(16.dp)
        ) {
            Text("IMPORT M3U", color = p.primary, fontFamily = body, fontSize = 13.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(preview.title, color = p.soft, fontFamily = body, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "${preview.resolved}/${preview.totalEntries} resolved · ${preview.missing} missing · ${preview.ambiguous} ambiguous",
                color = p.dim,
                fontFamily = body,
                fontSize = 12.sp
            )
            Text("Encoding: ${preview.encoding}", color = p.dim, fontFamily = body, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CANCEL",
                    color = p.dim,
                    fontFamily = body,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, p.dim.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Text(
                    "IMPORT",
                    color = p.voidBlack,
                    fontFamily = body,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(p.accent)
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
