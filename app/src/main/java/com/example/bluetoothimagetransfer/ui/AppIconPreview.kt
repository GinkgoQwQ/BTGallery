package com.example.bluetoothimagetransfer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluetoothimagetransfer.R

/**
 * 应用图标的预览（仅设计时使用，不参与运行时逻辑）。
 *
 * 左侧 = 完整 108x108 画布；
 * 中间 = 圆形裁切（多数启动器的圆形图标）；
 * 右侧 = 圆角方形裁切（多见于小米/Pixel 等）。
 * 用来确认前景图形落在自适应图标安全区内、不会被裁掉。
 */
@Preview(name = "App Icon", widthDp = 380, heightDp = 150)
@Composable
private fun AppIconPreview() {
    Row(
        modifier = Modifier
            .background(Color(0xFF22262B))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconLayers(Modifier.size(104.dp))
        IconLayers(Modifier.size(104.dp).clip(CircleShape))
        IconLayers(Modifier.size(104.dp).clip(RoundedCornerShape(24.dp)))
    }
}

@Composable
private fun IconLayers(modifier: Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
