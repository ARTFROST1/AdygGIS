package com.adygyes.app.presentation.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber

/**
 * Haptic feedback utility for providing tactile feedback
 */
class HapticFeedback(private val context: Context) {
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Light haptic feedback for button taps
     */
    fun light() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to provide light haptic feedback")
        }
    }
    
    /**
     * Medium haptic feedback for selections
     */
    fun medium() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to provide medium haptic feedback")
        }
    }
    
    /**
     * Heavy haptic feedback for important actions
     */
    fun heavy() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to provide heavy haptic feedback")
        }
    }
    
    /**
     * Success haptic feedback pattern
     */
    fun success() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 100, 50, 100)
                    val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(0, 100, 50, 100)
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to provide success haptic feedback")
        }
    }
    
    /**
     * Error haptic feedback pattern
     */
    fun error() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
                    val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
                    @Suppress("DEPRECATION")
                    vib.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to provide error haptic feedback")
        }
    }
    
    /**
     * Check if haptic feedback is available
     */
    fun isAvailable(): Boolean {
        return vibrator?.hasVibrator() == true
    }
}

/**
 * Composable function to remember haptic feedback instance
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val context = LocalContext.current
    return remember { HapticFeedback(context) }
}
