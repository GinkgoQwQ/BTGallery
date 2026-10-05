package com.ginkgoqwq.btgallery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ginkgoqwq.btgallery.BuildConfig
import com.ginkgoqwq.btgallery.data.AppPreferences
import com.ginkgoqwq.btgallery.ui.components.AppSwitchRow
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.SectionCard
import com.ginkgoqwq.btgallery.ui.components.appOnSurfaceVariant

/** 设置页：外观开关 + 关于信息。 */
@Composable
fun SettingsScreen(prefs: AppPreferences) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard("外观") {
            AppSwitchRow(
                title = "动态取色",
                summary = "跟随系统壁纸取色（Android 12+）",
                checked = prefs.monetEnabled,
                onCheckedChange = { prefs.monetEnabled = it }
            )
            Spacer(Modifier.height(4.dp))
            AppSwitchRow(
                title = "悬浮底栏",
                summary = "开启后底栏悬浮于内容之上；关闭则为贴底导航栏",
                checked = prefs.floatingNavBarEnabled,
                onCheckedChange = { prefs.floatingNavBarEnabled = it }
            )
        }

        SectionCard("关于") {
            AppText(text = "BTGallery", style = AppTextStyle.Subtitle)
            Spacer(Modifier.height(4.dp))
            // 直接读 Gradle 配置里的版本，不需要手写（也不会忘了同步）
            AppText(
                text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
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
