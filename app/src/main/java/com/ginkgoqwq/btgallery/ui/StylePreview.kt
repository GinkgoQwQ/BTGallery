package com.ginkgoqwq.btgallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ginkgoqwq.btgallery.ui.components.AppButton
import com.ginkgoqwq.btgallery.ui.components.AppCard
import com.ginkgoqwq.btgallery.ui.components.AppDivider
import com.ginkgoqwq.btgallery.ui.components.AppProgress
import com.ginkgoqwq.btgallery.ui.components.AppSlider
import com.ginkgoqwq.btgallery.ui.components.AppSwitch
import com.ginkgoqwq.btgallery.ui.components.AppTabs
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextButton
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.SectionCard
import com.ginkgoqwq.btgallery.ui.components.StatusPill
import com.ginkgoqwq.btgallery.ui.components.appBackground
import com.ginkgoqwq.btgallery.ui.components.appOnSurfaceVariant
import com.ginkgoqwq.btgallery.ui.theme.AppTheme
import com.ginkgoqwq.btgallery.ui.theme.UiStyle

/*
 * 设计时预览：把同一套组件分别渲染成「默认风格」和「Miuix 风格」，
 * 用于确认包装层两种外观都正常。不参与运行时逻辑。
 */

@Preview(name = "默认风格", widthDp = 390, heightDp = 620)
@Composable
private fun StylePreviewMaterial() {
    AppTheme(style = UiStyle.Material) { StyleShowcase() }
}

@Preview(name = "Miuix 风格", widthDp = 390, heightDp = 620)
@Composable
private fun StylePreviewMiuix() {
    AppTheme(style = UiStyle.Miuix) { StyleShowcase() }
}

@Composable
private fun StyleShowcase() {
    Box(
        Modifier
            .fillMaxSize()
            .background(appBackground())
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard("① 连接接收端") {
                StatusPill("已连接：Xiaomi 15")
                Spacer(Modifier.height(10.dp))
                AppButton(
                    text = "扫描附近设备",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                AppCard {
                    Column(Modifier.padding(12.dp)) {
                        AppText(text = "Redmi K40", style = AppTextStyle.Body)
                        AppText(
                            text = "AA:BB:CC:DD:EE:FF",
                            style = AppTextStyle.Caption,
                            color = appOnSurfaceVariant()
                        )
                    }
                }
            }

            SectionCard("② 轮播播放") {
                AppTabs(
                    tabs = listOf("秒", "分", "时"),
                    selectedIndex = 2,
                    onSelect = {}
                )
                Spacer(Modifier.height(10.dp))
                AppSlider(value = 6f, onValueChange = {}, valueRange = 1f..24f, steps = 22)
                Spacer(Modifier.height(6.dp))
                AppProgress(fraction = 0.42f)
                Spacer(Modifier.height(10.dp))
                AppDivider()
                Spacer(Modifier.height(10.dp))
                AppSwitch(checked = true, onCheckedChange = {})
                Spacer(Modifier.height(6.dp))
                AppTextButton(text = "删除", onClick = {})
            }
        }
    }
}
