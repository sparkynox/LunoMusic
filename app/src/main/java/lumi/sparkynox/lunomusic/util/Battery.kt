package lumi.sparkynox.lunomusic.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

// Android likes to kill background playback on some phones, being exempt from battery optimization stops that
object Battery {
    fun exempt(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) ?: true

    // opens the system dialog that asks to let the app run unrestricted, falls back to the full list if that isn't available
    fun request(ctx: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")).from(ctx)
        runCatching { ctx.startActivity(direct) }.onFailure {
            AppLog.w("Battery", "direct request not available, opening the list", it)
            runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).from(ctx)) }
                .onFailure { e -> AppLog.e("Battery", "couldn't open battery settings", e) }
        }
    }

    // a non activity context can only start an activity in a new task
    private fun Intent.from(ctx: Context): Intent {
        if (ctx !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return this
    }
}
