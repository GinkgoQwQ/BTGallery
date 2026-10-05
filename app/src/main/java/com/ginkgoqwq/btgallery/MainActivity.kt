package com.ginkgoqwq.btgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ginkgoqwq.btgallery.data.AppPreferences
import com.ginkgoqwq.btgallery.data.AppStats
import com.ginkgoqwq.btgallery.ui.components.appBackground
import com.ginkgoqwq.btgallery.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 标准 edge-to-edge：系统栏透明、内容延伸其下。
        // 图标明暗由 AppTheme 里的 SystemBarAppearance 按实际主题设置。
        // 必须在 super.onCreate 之前调用。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 偏好与统计在 Activity 生命周期外持有，避免重建时丢失
        val prefs = AppPreferences(this)
        val stats = AppStats(this)
        setContent {
            AppTheme(monetEnabled = prefs.monetEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackground())
                ) {
                    AppRoot(prefs = prefs, stats = stats)
                }
            }
        }
    }
}
