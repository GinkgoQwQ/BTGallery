package com.ginkgoqwq.btgallery.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 顶层主题：Miuix（HyperOS 设计语言）。
 *
 * 用 `MiuixTheme(controller = ThemeController(<ColorSchemeMode>))` 构造 ——
 * 这是 KernelSU / HyperMusicCover 的同一套做法：`ColorSchemeMode` 同时决定
 * 深浅色与是否走 Monet 取色。
 *
 * 状态 → 模式映射：
 * - [monetEnabled] 开 → `MonetSystem`（跟随系统深浅 + 壁纸取色）
 * - [monetEnabled] 关 → `System`（跟随系统深浅，用 Miuix 内置配色）
 */
@Composable
fun AppTheme(
    monetEnabled: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val mode = if (monetEnabled) ColorSchemeMode.MonetSystem else ColorSchemeMode.System

    SystemBarAppearance(darkTheme = darkTheme)

    MiuixTheme(
        controller = remember(mode) { ThemeController(mode) },
        content = content
    )
}

/**
 * 让**系统栏图标**与 App 的实际明暗保持一致。
 *
 * Android 15（API 35）起，只要 targetSdk ≥ 35，edge-to-edge 就是强制生效的：
 * 状态栏与导航栏变透明，App 自己的背景会透到系统栏下面。
 * 但**图标颜色不会自动跟随**——系统仍按窗口主题给默认值。
 * 结果就是「浅色 App + 白色图标」互相看不见。
 *
 * 所以必须显式声明：浅色主题下用深色图标（[WindowCompat.getInsetsController]
 * 的 `isAppearanceLightStatusBars = true`），深色主题下用浅色图标。
 */
@Composable
private fun SystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    // 设计时预览（Android Studio）里拿不到 Activity，跳过
    if (view.isInEditMode) return

    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        // true = 深色图标，用于浅色背景
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

/** 从 Context 向上找到宿主 Activity（Compose 里拿 Window 需要）。 */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
