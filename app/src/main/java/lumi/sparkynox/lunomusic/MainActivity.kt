package lumi.sparkynox.lunomusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import lumi.sparkynox.lunomusic.ui.LoginScreen
import lumi.sparkynox.lunomusic.ui.LunoTheme
import lumi.sparkynox.lunomusic.ui.Screen
import lumi.sparkynox.lunomusic.util.Battery
import lumi.sparkynox.lunomusic.util.Prefs

class MainActivity : ComponentActivity() {
    private var askBattery by mutableStateOf(false)

    // notifications first (the player controls live there), then the battery question, so two system dialogs never stack
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { maybeAskBattery() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            maybeAskBattery()
        }

        setContent {
            LunoTheme {
                val vm = viewModel<MusicVM>()
                if (vm.showLogin) LoginScreen(onDone = vm::onLoginDone, onBack = { vm.showLogin = false })
                else Screen(vm)

                if (askBattery) {
                    AlertDialog(
                        onDismissRequest = { snoozeBattery() },
                        title = { Text("Keep the music playing") },
                        text = {
                            Text(
                                "To keep playing with the screen off, LunoMusic needs to be exempt from battery optimization. " +
                                    "Without it Android can stop the music after a while. You can change this any time in Settings."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { askBattery = false; Battery.request(this@MainActivity) }) { Text("Allow") }
                        },
                        dismissButton = { TextButton(onClick = { snoozeBattery() }) { Text("Not now") } }
                    )
                }
            }
        }
    }

    private fun maybeAskBattery() {
        if (!Battery.exempt(this) && System.currentTimeMillis() >= Prefs.batteryAskAfter) askBattery = true
    }

    // asked again in three days if it still isn't allowed
    private fun snoozeBattery() {
        askBattery = false
        Prefs.batteryAskAfter = System.currentTimeMillis() + 3 * 24 * 3600_000L
    }
}
