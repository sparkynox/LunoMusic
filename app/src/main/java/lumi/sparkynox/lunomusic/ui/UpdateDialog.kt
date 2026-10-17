package lumi.sparkynox.lunomusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.data.UpdateInfo

@Composable
fun UpdateDialog(vm: MusicVM, info: UpdateInfo) {
    val progress = vm.updateProgress
    val busy = progress != null

    AlertDialog(
        onDismissRequest = { if (!busy) vm.dismissUpdate(false) },
        icon = { Icon(painterResource(R.drawable.update), null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "LunoMusic v${info.version}" + if (info.size > 0) "  •  ${"%.1f".format(info.size / 1048576.0)} MB" else "",
                    style = MaterialTheme.typography.titleMedium
                )
                if (info.notes.isNotBlank()) {
                    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            info.notes.trim().take(1500),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    Text("Downloading ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
                if (vm.updateStatus.startsWith("Allow") || vm.updateStatus.startsWith("Download failed")) {
                    Text(vm.updateStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (!busy) {
                    TextButton(onClick = { vm.dismissUpdate(true) }) { Text("Skip this version") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = vm::startUpdate, enabled = !busy) { Text(if (busy) "Downloading..." else "Update") }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissUpdate(false) }, enabled = !busy) { Text("Later") }
        }
    )
}
