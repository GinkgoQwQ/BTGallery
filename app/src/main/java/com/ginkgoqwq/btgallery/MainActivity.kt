package com.ginkgoqwq.btgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ginkgoqwq.btgallery.ui.components.appBackground
import com.ginkgoqwq.btgallery.ui.theme.AppTheme
import com.ginkgoqwq.btgallery.ui.theme.UiStyleStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 界面风格偏好：在 Activity 生命周期外持有，避免旋转屏幕时重建丢失
        val styleStore = UiStyleStore(this)
        setContent {
            AppTheme(style = styleStore.style) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(appBackground())
                ) {
                    AppRoot(styleStore = styleStore)
                }
            }
        }
    }
}
