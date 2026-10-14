package lumi.sparkynox.lunomusic

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import lumi.sparkynox.lunomusic.ui.LoginScreen
import lumi.sparkynox.lunomusic.ui.LunoTheme
import lumi.sparkynox.lunomusic.ui.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LunoTheme {
                val vm = viewModel<MusicVM>()
                if (vm.showLogin) LoginScreen(onDone = vm::onLoginDone, onBack = { vm.showLogin = false })
                else Screen(vm)
            }
        }
    }
}
