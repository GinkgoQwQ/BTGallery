package com.ginkgoqwq.btgallery

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ginkgoqwq.btgallery.ui.theme.AppTheme
import com.ginkgoqwq.btgallery.ui.theme.UiStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 回归测试：切换界面风格时，内容子树必须被“移动”而不是“销毁重建”。
 *
 * 若 AppTheme 退化成普通的 `when` 分支（不使用 movableContentOf），
 * 这两个断言都会失败 —— 对应线上表现就是：
 *   - 发送端正在建立的蓝牙连接被断开（DisposableEffect.onDispose 触发）
 *   - 接收端正在进行的监听被停止
 *   - 当前页签被重置（在设置页切换风格会被弹回首页）
 */
@RunWith(AndroidJUnit4::class)
class ThemeSwitchStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 记录副作用被销毁的次数：切换风格时应当保持为 0。 */
    private var disposeCount = 0

    @Test
    fun switchingStyle_movesContentWithoutDisposing() {
        var style by mutableStateOf(UiStyle.Material)
        var marker: Any? = null

        composeTestRule.setContent {
            AppTheme(style = style) {
                marker = remember { Any() }
                DisposableEffect(Unit) {
                    onDispose { disposeCount++ }
                }
            }
        }

        val first = marker
        assertNotNull("内容应已完成组合", first)
        assertEquals("初次组合不应触发 onDispose", 0, disposeCount)

        // 默认 -> Miuix
        composeTestRule.runOnIdle { style = UiStyle.Miuix }
        composeTestRule.waitForIdle()
        assertSame(
            "切到 Miuix 后 remember 状态应保留（对象引用不变）",
            first,
            marker
        )
        assertEquals(
            "切到 Miuix 不应触发 onDispose（否则蓝牙连接会被断开）",
            0,
            disposeCount
        )

        // Miuix -> 默认
        composeTestRule.runOnIdle { style = UiStyle.Material }
        composeTestRule.waitForIdle()
        assertSame("切回默认风格也应保留状态", first, marker)
        assertEquals("切回默认风格也不应触发 onDispose", 0, disposeCount)
    }
}
