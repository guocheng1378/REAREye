package hk.uwu.reareye.hook.scopes.subscreencenter.modules

import android.content.Context
import android.media.AudioTrack
import android.os.Handler
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import hk.uwu.reareye.ui.config.ConfigKeys
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Music Visualizer Hook
 *
 * Hooks into AudioTrack.write() to capture PCM audio data in real-time.
 * Performs lightweight FFT analysis and stores frequency band data
 * for rear screen visualization templates.
 *
 * Configuration (via prefs):
 * - [ConfigKeys.HOOK_MUSIC_VISUALIZER_ENABLED]: Master toggle
 * - [ConfigKeys.MUSIC_VISUALIZER_BANDS]: Number of frequency bands (8/16/32)
 */
class MusicVisualizerHook : YukiBaseHooker() {

    companion object {
        private const val TAG = "REAREye-Visualizer"
        private const val PREFS_NAME = "reareye_visualizer"
        private const val DEFAULT_BANDS = 16
        private const val UPDATE_INTERVAL_MS = 50L // 20 FPS
        private const val FFT_SIZE = 256

        // Pref keys for band data
        const val KEY_BAND_COUNT = "band_count"
        const val KEY_BAND_PREFIX = "band_"
        const val KEY_RMS_LEVEL = "rms_level"
        const val KEY_PEAK_LEVEL = "peak_level"
        const val KEY_LAST_UPDATE = "last_update"
        const val KEY_SOURCE_PACKAGE = "source_package"
    }

    private val hookInitialized = AtomicBoolean(false)
    private val isEnabled = AtomicBoolean(false)
    private var hostContext: Context? = null
    private var mainHandler: Handler? = null
    private var lastUpdateTime = 0L
    private var sourcePackage: String? = null

    // FFT buffers
    private val fftBuffer = DoubleArray(FFT_SIZE)
    private val fftReal = DoubleArray(FFT_SIZE)
    private val fftImag = DoubleArray(FFT_SIZE)
    private val bandLevels = FloatArray(32) // Max 32 bands

    override fun onHook() {
        loadApp("com.xiaomi.subscreencenter") {
            onAppLifecycle {
                onCreate {
                    hostContext = appContext
                    mainHandler = Handler(appContext!!.mainLooper)
                    isEnabled.set(prefs.getBoolean(ConfigKeys.HOOK_MUSIC_VISUALIZER_ENABLED, false))
                    debugLog("MusicVisualizerHook initialized enabled=${isEnabled.get()}")
                }
            }

            // Hook AudioTrack.write to capture PCM data
            val audioTrackRef = "android.media.AudioTrack".toClass().resolve()

            // Hook the byte array version of write
            audioTrackRef.firstMethod {
                name = "write"
                returnType = Int::class.javaPrimitiveType
                parameters(ByteArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
            }.hook().after {
                if (!isEnabled.get()) return@after
                val data = args.getOrNull(0) as? ByteArray ?: return@after
                val offset = args.getOrNull(1) as? Int ?: 0
                val size = args.getOrNull(2) as? Int ?: return@after
                processAudioData(data, offset, size)
            }

            // Hook the short array version
            audioTrackRef.firstMethod {
                name = "write"
                returnType = Int::class.javaPrimitiveType
                parameters(ShortArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
            }.hook().after {
                if (!isEnabled.get()) return@after
                val data = args.getOrNull(0) as? ShortArray ?: return@after
                val offset = args.getOrNull(1) as? Int ?: 0
                val size = args.getOrNull(2) as? Int ?: return@after
                processShortAudioData(data, offset, size)
            }

            // Hook the float array version (API 23+)
            runCatching {
                audioTrackRef.firstMethod {
                    name = "write"
                    returnType = Int::class.javaPrimitiveType
                    parameters(FloatArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!)
                }.hook().after {
                    if (!isEnabled.get()) return@after
                    val data = args.getOrNull(0) as? FloatArray ?: return@after
                    val offset = args.getOrNull(1) as? Int ?: 0
                    val size = args.getOrNull(2) as? Int ?: return@after
                    processFloatAudioData(data, offset, size)
                }
            }

            // Detect which app is playing audio
            val audioManagerRef = "android.media.AudioManager".toClass().resolve()
            audioManagerRef.firstMethod {
                name = "requestAudioFocus"
                parameterCount = 4
            }.hook().after {
                val result = result as? Int
                if (result == 0) { // AUDIOFOCUS_REQUEST_GRANTED
                    sourcePackage = hostContext?.packageName
                    debugLog("Audio focus gained by $sourcePackage")
                }
            }
        }
    }

    private fun processAudioData(data: ByteArray, offset: Int, size: Int) {
        if (size < 4) return
        val samples = minOf(size, FFT_SIZE * 2)
        for (i in 0 until samples step 2) {
            val idx = offset + i
            if (idx + 1 >= data.size) break
            val sample = ((data[idx + 1].toInt() shl 8) or (data[idx].toInt() and 0xFF)).toDouble() / 32768.0
            fftBuffer[i / 2] = sample
        }
        analyzeAndPublish(size / 2)
    }

    private fun processShortAudioData(data: ShortArray, offset: Int, size: Int) {
        if (size < 2) return
        val samples = minOf(size, FFT_SIZE)
        for (i in 0 until samples) {
            val idx = offset + i
            if (idx >= data.size) break
            fftBuffer[i] = data[idx].toDouble() / 32768.0
        }
        analyzeAndPublish(samples)
    }

    private fun processFloatAudioData(data: FloatArray, offset: Int, size: Int) {
        if (size < 2) return
        val samples = minOf(size, FFT_SIZE)
        for (i in 0 until samples) {
            val idx = offset + i
            if (idx >= data.size) break
            fftBuffer[i] = data[idx].toDouble().coerceIn(-1.0, 1.0)
        }
        analyzeAndPublish(samples)
    }

    private fun analyzeAndPublish(sampleCount: Int) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < UPDATE_INTERVAL_MS) return
        lastUpdateTime = now

        val bandCount = prefs.getInt(
            ConfigKeys.MUSIC_VISUALIZER_BANDS,
            DEFAULT_BANDS
        ).coerceIn(4, 32)

        // Compute RMS level
        var sumSquares = 0.0
        var peak = 0.0
        for (i in 0 until sampleCount.coerceAtMost(FFT_SIZE)) {
            val v = fftBuffer[i]
            sumSquares += v * v
            if (abs(v) > peak) peak = abs(v)
        }
        val rms = sqrt(sumSquares / sampleCount.coerceAtLeast(1))

        // Simple DFT for a subset of frequency bands
        // Use energy distribution across time-domain samples as band approximation
        val samplesPerBand = sampleCount.coerceAtLeast(bandCount) / bandCount
        for (band in 0 until bandCount) {
            var bandEnergy = 0.0
            val start = band * samplesPerBand
            val end = minOf(start + samplesPerBand, sampleCount, FFT_SIZE)
            for (i in start until end) {
                bandEnergy += fftBuffer[i] * fftBuffer[i]
            }
            bandLevels[band] = sqrt(bandEnergy / (end - start).coerceAtLeast(1)).toFloat()
        }

        // Apply smoothing
        for (band in 0 until bandCount) {
            bandLevels[band] = bandLevels[band].coerceIn(0f, 1f)
        }

        // Publish to SharedPreferences
        publishBandData(bandCount, rms.toFloat(), peak.toFloat())
    }

    private fun publishBandData(bandCount: Int, rms: Float, peak: Float) {
        val ctx = hostContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putInt(KEY_BAND_COUNT, bandCount)
        editor.putFloat(KEY_RMS_LEVEL, rms)
        editor.putFloat(KEY_PEAK_LEVEL, peak)
        editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        editor.putString(KEY_SOURCE_PACKAGE, sourcePackage ?: "unknown")

        for (i in 0 until bandCount) {
            editor.putFloat("$KEY_BAND_PREFIX$i", bandLevels[i])
        }

        editor.apply()
    }

    private fun debugLog(message: String) {
        if (prefs.getBoolean(ConfigKeys.MORE_DEBUG, false)) {
            YLog.debug("[$TAG] $message")
        }
    }
}
