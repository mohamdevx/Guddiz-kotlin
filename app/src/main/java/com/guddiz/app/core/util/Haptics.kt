package com.guddiz.app.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Minimal, tactile feedback at four deliberate moments.
 * Requires android.permission.VIBRATE (declared in the manifest).
 * Silently no-ops if the permission is somehow missing, or the device has no vibrator.
 */
object Haptics {

    private fun vibrator(context: Context): Vibrator? {
        val hasPermission = context.checkSelfPermission(Manifest.permission.VIBRATE) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun buzz(context: Context, durationMs: Long, amplitude: Int) {
        try {
            val v = vibrator(context) ?: return
            if (!v.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Never crash on haptics — it's a nicety, not a feature
        }
    }

    fun success(context: Context) = buzz(context, 20L, 80)
    fun destructive(context: Context) = buzz(context, 40L, 150)
    fun error(context: Context) = buzz(context, 60L, 200)
    fun tap(context: Context) = buzz(context, 15L, 60)
}
