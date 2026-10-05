package com.ginkgoqwq.btgallery.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用偏好设置。
 *
 * 每个字段都是 Compose State，各自的 setter 会**自动写入 SharedPreferences**，
 * 所以调用方直接赋值即可，不需要额外的“保存”步骤。
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var monetState: Boolean by mutableStateOf(prefs.getBoolean(KEY_MONET, true))
    private var floatingNavState: Boolean by mutableStateOf(prefs.getBoolean(KEY_FLOATING_NAV, false))
    private var lastPageState: Int by mutableIntStateOf(prefs.getInt(KEY_LAST_PAGE, 0))

    /**
     * 是否启用动态取色（Monet）。
     *
     * Miuix 侧由 `ThemeController(ColorSchemeMode.Monet*)` 实现；
     * Android 12 以下会自动回退到内置配色。
     */
    var monetEnabled: Boolean
        get() = monetState
        set(value) {
            if (value == monetState) return
            monetState = value
            prefs.edit().putBoolean(KEY_MONET, value).apply()
        }

    /**
     * 是否启用悬浮底栏。
     *
     * 关闭（默认）→ 贴底导航栏；开启 → 悬浮胶囊底栏。
     */
    var floatingNavBarEnabled: Boolean
        get() = floatingNavState
        set(value) {
            if (value == floatingNavState) return
            floatingNavState = value
            prefs.edit().putBoolean(KEY_FLOATING_NAV, value).apply()
        }

    /**
     * 上次所在的页面索引。
     *
     * 启动时用它作为初始页，因此不需要每次手动切回去。
     */
    var lastPage: Int
        get() = lastPageState
        set(value) {
            if (value == lastPageState) return
            lastPageState = value
            prefs.edit().putInt(KEY_LAST_PAGE, value).apply()
        }

    private companion object {
        const val PREFS_NAME = "btgallery_settings"
        const val KEY_MONET = "monet_enabled"
        const val KEY_FLOATING_NAV = "floating_nav_bar"
        const val KEY_LAST_PAGE = "last_page"
    }
}
