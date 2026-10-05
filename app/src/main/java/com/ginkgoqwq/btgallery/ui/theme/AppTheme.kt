package com.ginkgoqwq.btgallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 顶层主题。根据 [style] 选择 Material 3 或 Miuix 主题，并把当前风格下发给组件层。
 *
 * 注意 Miuix 不会自动跟随系统深色模式，必须显式传 darkColorScheme / lightColorScheme。
 */
@Composable
fun AppTheme(
    style: UiStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalUiStyle provides style) {
        when (style) {
            UiStyle.Miuix -> MiuixTheme(
                colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
                content = content
            )

            UiStyle.Material -> BluetoothConnectTheme(
                darkTheme = darkTheme,
                content = content
            )
        }
    }
}
