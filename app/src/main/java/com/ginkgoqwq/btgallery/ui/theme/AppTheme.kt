package com.ginkgoqwq.btgallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 顶层主题。根据 [style] 选择 Material 3 或 Miuix 主题，并把当前风格下发给组件层。
 *
 * ### 为什么用 movableContentOf
 *
 * 这里按风格走了两个不同的分支（调用了两个不同的主题函数），而 Compose 中
 * **不同分支属于不同的组合组**。若直接写 `when (style) { ... }`，切换风格时整棵内容
 * 子树会被销毁重建，副作用是：
 *   - 发送端正在建立的蓝牙连接被断开（DisposableEffect 的 onDispose 触发）
 *   - 接收端正在进行的监听被停止
 *   - 当前页签被重置（在设置页切换风格会被弹回首页）
 *
 * [movableContentOf] 正是为此设计：让同一份内容在两个位置之间“移动”，
 * 且**保留全部 remember 状态与已注册的副作用**。
 *
 * 注意：Miuix 不会自动跟随系统深色模式，必须显式传 darkColorScheme / lightColorScheme。
 */
@Composable
fun AppTheme(
    style: UiStyle,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 记住这份内容，使其在风格切换时被“移动”而不是重建
    val movableContent = remember { movableContentOf(content) }

    CompositionLocalProvider(LocalUiStyle provides style) {
        when (style) {
            UiStyle.Miuix -> MiuixTheme(
                colors = if (darkTheme) darkColorScheme() else lightColorScheme(),
                content = movableContent
            )

            UiStyle.Material -> BluetoothConnectTheme(
                darkTheme = darkTheme,
                content = movableContent
            )
        }
    }
}
