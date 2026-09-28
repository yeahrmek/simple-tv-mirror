package io.github.jqssun.airplay

import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import dagger.hilt.android.HiltAndroidApp
import io.github.jqssun.airplay.diag.DiagServer
import io.github.jqssun.airplay.diag.EventLog
import java.io.File

@HiltAndroidApp
class AirPlayApp : Application() {
    val events by lazy { EventLog(File(filesDir, "logs")) }

    override fun onCreate() {
        super.onCreate()
        events.write("process start pid=${Process.myPid()} version=${packageManager.getPackageInfo(packageName, 0).versionName}")
        logLastExit()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            events.write("crash thread=${t.name} ${e.stackTraceToString()}")
            prev?.uncaughtException(t, e)
        }
        runCatching { DiagServer(DIAG_PORT, events::read).start() }
            .onFailure { events.write("diag server failed: $it") }
        watchNetwork()
        watchScreen()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        events.write("trim memory level=$level")
    }

    private fun logLastExit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val am = getSystemService(ActivityManager::class.java)
        am.getHistoricalProcessExitReasons(packageName, 0, 1).firstOrNull()?.let {
            events.write("last exit reason=${it.reason} status=${it.status} importance=${it.importance} at=${it.timestamp} ${it.description ?: ""}")
        }
    }

    private fun watchNetwork() {
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = events.write("network available $network")
            override fun onLost(network: Network) = events.write("network lost $network")
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) =
                events.write("network $network ${lp.interfaceName} ${lp.linkAddresses.joinToString()}")
        })
    }

    private fun watchScreen() {
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) }
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) = events.write(intent.action!!.substringAfterLast('.'))
        }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    companion object {
        const val DIAG_PORT = 8080
    }
}
