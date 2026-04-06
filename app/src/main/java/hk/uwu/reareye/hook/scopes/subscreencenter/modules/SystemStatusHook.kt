package hk.uwu.reareye.hook.scopes.subscreencenter.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.Handler
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import hk.uwu.reareye.ui.config.ConfigKeys
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * System Status Data Source Hook
 *
 * Periodically collects system information and stores it in SharedPreferences
 * so that rear screen widget templates can access real-time data via content providers
 * or direct preference reads.
 *
 * Data collected:
 * - Battery level, charging status, temperature
 * - Available RAM percentage
 * - Internal storage usage
 * - System uptime
 * - WiFi signal (if available)
 */
class SystemStatusHook : YukiBaseHooker() {

    companion object {
        private const val TAG = "REAREye-SysStatus"
        private const val PREFS_NAME = "reareye_system_status"
        private const val UPDATE_INTERVAL_MS = 30_000L // 30 seconds

        // Pref keys
        const val KEY_BATTERY_LEVEL = "battery_level"
        const val KEY_BATTERY_STATUS = "battery_status"
        const val KEY_BATTERY_TEMP = "battery_temp"
        const val KEY_BATTERY_HEALTH = "battery_health"
        const val KEY_RAM_USED_PCT = "ram_used_pct"
        const val KEY_RAM_TOTAL_MB = "ram_total_mb"
        const val KEY_RAM_AVAIL_MB = "ram_avail_mb"
        const val KEY_STORAGE_USED_PCT = "storage_used_pct"
        const val KEY_STORAGE_TOTAL_GB = "storage_total_gb"
        const val KEY_STORAGE_AVAIL_GB = "storage_avail_gb"
        const val KEY_UPTIME_HOURS = "uptime_hours"
        const val KEY_UPTIME_MINUTES = "uptime_minutes"
        const val KEY_CPU_USAGE = "cpu_usage"
        const val KEY_LAST_UPDATE = "last_update"
    }

    private val hookInitialized = AtomicBoolean(false)
    private val updateScheduled = AtomicBoolean(false)
    private var hostContext: Context? = null
    private var mainHandler: Handler? = null

    private val updateRunnable = Runnable {
        updateSystemStatus()
        scheduleNextUpdate()
    }

    override fun onHook() {
        loadApp("com.xiaomi.subscreencenter") {
            onAppLifecycle {
                onCreate {
                    hostContext = appContext
                    mainHandler = Handler(appContext!!.mainLooper)

                    if (hookInitialized.compareAndSet(false, true)) {
                        // Initial update
                        updateSystemStatus()
                        scheduleNextUpdate()

                        // Listen for battery changes (more responsive than polling)
                        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                        ContextCompat.registerReceiver(
                            appContext,
                            object : BroadcastReceiver() {
                                override fun onReceive(context: Context?, intent: Intent?) {
                                    if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                                        updateBatteryFromIntent(intent)
                                    }
                                }
                            },
                            batteryFilter,
                            ContextCompat.RECEIVER_NOT_EXPORTED,
                        )

                        debugLog("SystemStatusHook initialized")
                    }
                }

                onTerminate {
                    mainHandler?.removeCallbacks(updateRunnable)
                    updateScheduled.set(false)
                    debugLog("SystemStatusHook terminated")
                }
            }
        }
    }

    private fun scheduleNextUpdate() {
        val handler = mainHandler ?: return
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS)
        updateScheduled.set(true)
    }

    private fun updateSystemStatus() {
        val ctx = hostContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            // Battery (from cached intent or current)
            val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { updateBatteryFromIntent(it) }

            // RAM
            updateRamInfo(prefs)

            // Storage
            updateStorageInfo(prefs)

            // Uptime
            updateUptime(prefs)

            // CPU
            updateCpuUsage(prefs)

            // Timestamp
            prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()

            debugLog("System status updated")
        } catch (e: Exception) {
            debugLog("System status update failed: ${e.message}")
            YLog.error("[$TAG] update failed", e)
        }
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        val ctx = hostContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (scale > 0) (level * 100 / scale) else -1

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }

        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over_voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "cold"
            else -> "unknown"
        }

        prefs.edit()
            .putInt(KEY_BATTERY_LEVEL, batteryPct)
            .putString(KEY_BATTERY_STATUS, status)
            .putInt(KEY_BATTERY_TEMP, temp)
            .putString(KEY_BATTERY_HEALTH, health)
            .apply()
    }

    private fun updateRamInfo(prefs: android.content.SharedPreferences) {
        try {
            val reader = BufferedReader(FileReader("/proc/meminfo"))
            var totalMem = 0L
            var availMem = 0L
            var freeMem = 0L
            var buffers = 0L
            var cached = 0L

            reader.use { r ->
                var line: String?
                while (r.readLine().also { line = it } != null) {
                    when {
                        line!!.startsWith("MemTotal:") -> {
                            totalMem = line!!.split(Regex("\\s+"))[1].toLongOrNull() ?: 0
                        }
                        line!!.startsWith("MemAvailable:") -> {
                            availMem = line!!.split(Regex("\\s+"))[1].toLongOrNull() ?: 0
                        }
                        line!!.startsWith("MemFree:") -> {
                            freeMem = line!!.split(Regex("\\s+"))[1].toLongOrNull() ?: 0
                        }
                        line!!.startsWith("Buffers:") -> {
                            buffers = line!!.split(Regex("\\s+"))[1].toLongOrNull() ?: 0
                        }
                        line!!.startsWith("Cached:") -> {
                            cached = line!!.split(Regex("\\s+"))[1].toLongOrNull() ?: 0
                        }
                    }
                }
            }

            // If MemAvailable is not available, calculate it
            if (availMem == 0L) {
                availMem = freeMem + buffers + cached
            }

            val usedMem = totalMem - availMem
            val usedPct = if (totalMem > 0) ((usedMem * 100) / totalMem).toInt() else 0
            val totalMb = (totalMem / 1024).toInt()
            val availMb = (availMem / 1024).toInt()

            prefs.edit()
                .putInt(KEY_RAM_USED_PCT, usedPct)
                .putInt(KEY_RAM_TOTAL_MB, totalMb)
                .putInt(KEY_RAM_AVAIL_MB, availMb)
                .apply()
        } catch (e: Exception) {
            debugLog("RAM info read failed: ${e.message}")
        }
    }

    private fun updateStorageInfo(prefs: android.content.SharedPreferences) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val availBytes = stat.availableBytes
            val usedBytes = totalBytes - availBytes

            val usedPct = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
            val totalGb = String.format("%.1f", totalBytes / (1024.0 * 1024 * 1024))
            val availGb = String.format("%.1f", availBytes / (1024.0 * 1024 * 1024))

            prefs.edit()
                .putInt(KEY_STORAGE_USED_PCT, usedPct)
                .putString(KEY_STORAGE_TOTAL_GB, totalGb)
                .putString(KEY_STORAGE_AVAIL_GB, availGb)
                .apply()
        } catch (e: Exception) {
            debugLog("Storage info read failed: ${e.message}")
        }
    }

    private fun updateUptime(prefs: android.content.SharedPreferences) {
        try {
            val uptimeMs = android.os.SystemClock.elapsedRealtime()
            val uptimeMinutes = uptimeMs / 60_000
            val hours = (uptimeMinutes / 60).toInt()
            val minutes = (uptimeMinutes % 60).toInt()

            prefs.edit()
                .putInt(KEY_UPTIME_HOURS, hours)
                .putInt(KEY_UPTIME_MINUTES, minutes)
                .apply()
        } catch (e: Exception) {
            debugLog("Uptime read failed: ${e.message}")
        }
    }

    private fun updateCpuUsage(prefs: android.content.SharedPreferences) {
        try {
            // Simple CPU usage approximation from /proc/stat
            val reader = BufferedReader(FileReader("/proc/stat"))
            reader.use { r ->
                val line = r.readLine() ?: return
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 5) {
                    val user = parts[1].toLongOrNull() ?: 0
                    val nice = parts[2].toLongOrNull() ?: 0
                    val system = parts[3].toLongOrNull() ?: 0
                    val idle = parts[4].toLongOrNull() ?: 0
                    val total = user + nice + system + idle
                    val usedPct = if (total > 0) (((total - idle) * 100) / total).toInt() else 0

                    prefs.edit().putInt(KEY_CPU_USAGE, usedPct).apply()
                }
            }
        } catch (e: Exception) {
            debugLog("CPU usage read failed: ${e.message}")
        }
    }

    private fun debugLog(message: String) {
        if (prefs.getBoolean(ConfigKeys.MORE_DEBUG, false)) {
            YLog.debug("[$TAG] $message")
        }
    }
}
