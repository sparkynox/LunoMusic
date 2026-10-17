package lumi.sparkynox.lunomusic.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lumi.sparkynox.lunomusic.MusicVM
import lumi.sparkynox.lunomusic.R
import lumi.sparkynox.lunomusic.data.Downloads
import lumi.sparkynox.lunomusic.data.Play
import lumi.sparkynox.lunomusic.data.Stats

@Composable
fun ProfileScreen(vm: MusicVM) {
    var editName by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.setAvatarFrom(it) }
    }
    val plays by produceState<List<Play>>(emptyList()) {
        value = withContext(Dispatchers.IO) { runCatching { Stats.since(0) }.getOrDefault(emptyList()) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 200.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(R.drawable.arrow_back, vm::pop)
            Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
        }

        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(136.dp)) {
                Avatar(vm, 136.dp)
                RoundIcon(
                    R.drawable.image,
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.align(Alignment.BottomEnd),
                    size = 44.dp, iconSize = 22.dp,
                    container = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(vm.displayName(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                RoundIcon(R.drawable.edit, { editName = true }, size = 36.dp, iconSize = 18.dp, container = Color.Transparent)
            }
            if (vm.accountHandle.isNotBlank()) {
                Text(vm.accountHandle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (vm.accountEmail.isNotBlank()) {
                Text(vm.accountEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text("Change photo")
                }
                if (vm.customAvatar.isNotBlank()) {
                    OutlinedButton(onClick = vm::resetAvatar) { Text("Use YT Music photo") }
                }
            }
        }

        // bio
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
                Text("Bio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp))
                TextField(
                    value = vm.bio,
                    onValueChange = vm::setBio,
                    placeholder = { Text("Tell people about your taste in music") },
                    minLines = 2,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${vm.bio.length}/200", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End).padding(end = 12.dp)
                )
            }
        }

        // numbers
        val minutes = plays.sumOf { it.ms } / 60000
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat("Plays", plays.size.toString(), Modifier.weight(1f))
                ProfileStat("Listened", if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileStat("Offline songs", Downloads.ids.size.toString(), Modifier.weight(1f))
                ProfileStat("Liked songs", if (vm.loggedIn) vm.likedIds.size.toString() else "-", Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.loggedIn) {
                OutlinedButton(onClick = { vm.pop(); vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
            } else {
                Button(onClick = { vm.pop(); vm.showLogin = true }, modifier = Modifier.fillMaxWidth()) { Text("Log in with Google") }
                Text(
                    "Log in to show your YouTube Music photo and name here",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (editName) {
        var draft by remember { mutableStateOf(vm.customName.ifBlank { vm.displayName() }) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Display name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = draft, onValueChange = { draft = it.take(40) }, singleLine = true)
                    Text("Only shown inside LunoMusic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { vm.setCustomName(draft); editName = false }) { Text("Save") } },
            dismissButton = {
                TextButton(onClick = { vm.setCustomName(""); editName = false }) { Text("Reset") }
            }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(24.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
