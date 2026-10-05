package com.ginkgoqwq.btgallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ginkgoqwq.btgallery.ui.components.AppTabs
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.SectionCard
import com.ginkgoqwq.btgallery.ui.components.appOnSurfaceVariant
import com.ginkgoqwq.btgallery.ui.theme.UiStyle
import com.ginkgoqwq.btgallery.ui.theme.UiStyleStore

/** 设置页：目前提供界面风格切换。 */
@Composable
fun SettingsScreen(styleStore: UiStyleStore) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard("界面风格") {
            AppTabs(
                tabs = UiStyle.entries.map { it.label },
                selectedIndex = UiStyle.entries.indexOf(styleStore.style),
                onSelect = { styleStore.set(UiStyle.entries[it]) }
            )
            Spacer(Modifier.height(10.dp))
            AppText(
                text = styleStore.style.description,
                style = AppTextStyle.Caption,
                color = appOnSurfaceVariant()
            )
        }

        SectionCard("关于") {
            AppText(text = "BTGallery", style = AppTextStyle.Subtitle)
            Spacer(Modifier.height(4.dp))
            AppText(
                text = "版本 1.0.0",
                style = AppTextStyle.Caption,
                color = appOnSurfaceVariant()
            )
            Spacer(Modifier.height(10.dp))
            AppText(
                text = "纯蓝牙图片传输与轮播展示工具，不依赖 Wi-Fi、局域网或任何 IP 网络。",
                style = AppTextStyle.Caption,
                color = appOnSurfaceVariant()
            )
        }
    }
}
