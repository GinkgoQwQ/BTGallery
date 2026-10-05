package com.ginkgoqwq.btgallery.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 界面风格。
 *
 * 用 C++ 类比：这就是一个 enum，决定「用哪套 UI 组件库渲染」。
 */
enum class UiStyle(val label: String, val description: String) {
    /** 默认风格：Material 3 + 本项目自定义配色。 */
    Material("默认", "Material 3 配色，蓝白清爽风格"),

    /** Miuix 风格：HyperOS / MIUI 设计语言。 */
    Miuix("Miuix", "HyperOS 观感，圆润卡片与胶囊控件")
}

/**
 * 当前风格，通过 CompositionLocal 下发。
 *
 * 组件包装层（AppComponents）读取它来决定渲染 Material 还是 Miuix 组件，
 * 这样各个界面就不用自己判断风格了。
 */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.Material }

/**
 * 风格偏好的持久化存储。
 *
 * [style] 用 Compose State 包装：赋值即触发重组，同时写入 SharedPreferences，
 * 因此重启 App 后仍保留上次的选择。
 */
class UiStyleStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var style: UiStyle by mutableStateOf(load())
        private set

    fun set(value: UiStyle) {
        if (value == style) return
        style = value
        prefs.edit().putString(KEY_STYLE, value.name).apply()
    }

    private fun load(): UiStyle =
        UiStyle.entries.firstOrNull { it.name == prefs.getString(KEY_STYLE, null) }
            ?: UiStyle.Material

    private companion object {
        const val PREFS_NAME = "btgallery_settings"
        const val KEY_STYLE = "ui_style"
    }
}
