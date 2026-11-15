package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffect {
    WOOD_KNOCK,
    ICE_CRACK,
    GLASS_SHATTER,
    RUBBER_BOING,
    MAGNET_HUM,
    COIN_DING,
    TNT_BOOM,
    COLLAPSE_BOOM,
    HEARTBEAT,
    UI_CLICK,
    LEVEL_WIN,
    COMBO_CHIME,
    FREEZE_CHILL
}

class SoundAndHapticsEngine(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var soundEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var musicVolume: Float = 0.65f

    @Volatile private var towerIntensity: Float = 0.2f
    @Volatile private var isPaused: Boolean = false
    private var musicJob: Job? = null

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    fun updateSettings(
        sound: Boolean,
        music: Boolean,
        haptics: Boolean,
        sfxVol: Float,
        musicVol: Float
    ) {
        soundEnabled = sound
        musicEnabled = music
        hapticsEnabled = haptics
        sfxVolume = sfxVol.coerceIn(0f, 1f)
        musicVolume = musicVol.coerceIn(0f, 1f)
        if (!musicEnabled && musicJob != null) {
            musicJob?.cancel()
            musicJob = null
        } else if (musicEnabled && !isPaused && musicJob == null) {
            startDynamicMusic()
        }
    }

    fun setIntensity(intensity: Float) {
        towerIntensity = intensity.coerceIn(0f, 1f)
    }

    fun startDynamicMusic() {
        if (!musicEnabled || isPaused || musicJob?.isActive == true) return
        musicJob = scope.launch {
            val scale = floatArrayOf(220f, 261.63f, 293.66f, 329.63f, 392.00f, 440f, 523.25f)
            var step = 0
            while (isActive && musicEnabled && !isPaused) {
                val vol = musicVolume * 0.14f
                if (vol > 0.01f) {
                    val baseFreq = scale[step % scale.size]
                    val octave = if (towerIntensity > 0.65f && step % 2 == 1) 1.5f else 1.0f
                    playToneInternal(
                        freqStart = baseFreq * octave,
                        freqEnd = baseFreq * octave * 0.995f,
                        durationMs = 240,
                        volume = vol,
                        noiseMix = 0.02f
                    )
                }
                val tempoMs = (560L - (towerIntensity * 260L).toLong()).coerceIn(260L, 560L)
                step++
                delay(tempoMs)
            }
        }
    }

    fun pauseAudio() {
        isPaused = true
        musicJob?.cancel()
        musicJob = null
    }

    fun resumeAudio() {
        isPaused = false
        if (musicEnabled) startDynamicMusic()
    }

    fun playSfx(effect: SoundEffect) {
        if (!soundEnabled || sfxVolume <= 0.01f || isPaused) return
        scope.launch {
            val vol = sfxVolume * 0.45f
            when (effect) {
                SoundEffect.WOOD_KNOCK -> playToneInternal(220f, 120f, 85, vol, 0.25f)
                SoundEffect.ICE_CRACK -> playToneInternal(1450f, 680f, 130, vol, 0.4f)
                SoundEffect.GLASS_SHATTER -> playToneInternal(2400f, 920f, 180, vol, 0.75f)
                SoundEffect.RUBBER_BOING -> playToneInternal(180f, 420f, 160, vol, 0.05f)
                SoundEffect.MAGNET_HUM -> playToneInternal(310f, 360f, 140, vol * 0.7f, 0.02f)
                SoundEffect.COIN_DING -> {
                    playToneInternal(987.77f, 987.77f, 70, vol, 0.0f)
                    delay(55)
                    playToneInternal(1318.51f, 1318.51f, 140, vol, 0.0f)
                }
                SoundEffect.TNT_BOOM -> playToneInternal(110f, 38f, 340, vol * 1.15f, 0.85f)
                SoundEffect.COLLAPSE_BOOM -> playToneInternal(130f, 42f, 420, vol * 1.1f, 0.7f)
                SoundEffect.HEARTBEAT -> playToneInternal(72f, 46f, 110, vol * 0.85f, 0.05f)
                SoundEffect.UI_CLICK -> playToneInternal(640f, 520f, 35, vol * 0.6f, 0.05f)
                SoundEffect.FREEZE_CHILL -> playToneInternal(1100f, 1650f, 220, vol * 0.7f, 0.2f)
                SoundEffect.COMBO_CHIME -> {
                    playToneInternal(587.33f, 587.33f, 75, vol, 0f)
                    delay(50)
                    playToneInternal(880f, 880f, 140, vol, 0f)
                }
                SoundEffect.LEVEL_WIN -> {
                    val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f)
                    notes.forEachIndexed { idx, freq ->
                        playToneInternal(freq, freq, if (idx == 3) 260 else 95, vol, 0f)
                        delay(80)
                    }
                }
            }
        }
    }

    fun vibrate(durationMs: Long = 28L, amplitude: Int = 110) {
        if (!hapticsEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        durationMs.coerceIn(10L, 400L),
                        amplitude.coerceIn(1, 255)
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun playToneInternal(
        freqStart: Float,
        freqEnd: Float,
        durationMs: Int,
        volume: Float,
        noiseMix: Float
    ) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        if (count <= 0) return
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = freqStart + (freqEnd - freqStart) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-3.8 * t).toFloat()
            val tone = sin(phase).toFloat()
            val noise = (Random.nextFloat() * 2f - 1f) * noiseMix
            val mixed = ((tone * (1f - noiseMix) + noise) * env * volume).coerceIn(-1f, 1f)
            samples[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
        }
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 30L)
            track.stop()
            track.release()
        } catch (_: Exception) {
        }
    }

    fun release() {
        pauseAudio()
    }
}
