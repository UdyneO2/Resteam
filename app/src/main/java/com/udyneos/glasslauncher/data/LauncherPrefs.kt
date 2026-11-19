package com.udyneos.glasslauncher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tiny SharedPreferences wrapper. Values are mirrored into StateFlows so
 * Compose recomposes as soon as a setting changes.
 */
class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    private val _showLabels = MutableStateFlow(prefs.getBoolean(KEY_LABELS, true))
    val showLabels: StateFlow<Boolean> = _showLabels

    private val _blurDp = MutableStateFlow(prefs.getFloat(KEY_BLUR, 10f))
    val blurDp: StateFlow<Float> = _blurDp

    private val _tintAlpha = MutableStateFlow(prefs.getFloat(KEY_TINT, 0.16f))
    val tintAlpha: StateFlow<Float> = _tintAlpha

    fun setShowLabels(value: Boolean) {
        prefs.edit().putBoolean(KEY_LABELS, value).apply()
        _showLabels.value = value
    }

    fun setBlurDp(value: Float) {
        prefs.edit().putFloat(KEY_BLUR, value).apply()
        _blurDp.value = value
    }

    fun setTintAlpha(value: Float) {
        prefs.edit().putFloat(KEY_TINT, value).apply()
        _tintAlpha.value = value
    }

    private companion object {
        const val KEY_LABELS = "show_labels"
        const val KEY_BLUR = "blur_dp"
        const val KEY_TINT = "tint_alpha"
    }
}
