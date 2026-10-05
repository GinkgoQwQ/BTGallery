package com.ginkgoqwq.btgallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 应用自绘图标。
 *
 * 为什么不直接用 Material 图标集：`androidx.compose.material:material-icons-core`
 * 并不在依赖里，而 `material-icons-extended` 体积庞大、会明显拖慢构建。
 * 这里只用 3 个图标，手写 [ImageVector] 更划算——零依赖、任意分辨率清晰、完全可控。
 *
 * 颜色统一用 [Color.Black] 占位；实际渲染时由 Compose 的 `Icon(tint = ...)` 覆盖。
 */
object AppIcons {

    /** 蓝牙符文（描边）：一条竖线 + 两个右向三角。 */
    val Bluetooth: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Bluetooth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // 竖干：顶 -> 底
                moveTo(10f, 3.5f)
                lineTo(10f, 20.5f)
                // 折线：顶 -> 右上 -> 中点 -> 右下 -> 底
                moveTo(10f, 3.5f)
                lineTo(17f, 9f)
                lineTo(10f, 12f)
                lineTo(17f, 15f)
                lineTo(10f, 20.5f)
            }
        }.build()
    }

    /** 主页：房子（描边轮廓 + 门）。 */
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // 屋顶
                moveTo(3f, 11f)
                lineTo(12f, 3.5f)
                lineTo(21f, 11f)
                // 墙体
                moveTo(5.5f, 9.8f)
                lineTo(5.5f, 20f)
                lineTo(18.5f, 20f)
                lineTo(18.5f, 9.8f)
                // 门
                moveTo(9.8f, 20f)
                lineTo(9.8f, 14.2f)
                lineTo(14.2f, 14.2f)
                lineTo(14.2f, 20f)
            }
        }.build()
    }

    /** 发送端：纸飞机。 */
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 21f)
                lineTo(22.5f, 12f)
                lineTo(2f, 3f)
                lineTo(2f, 9.8f)
                lineTo(16.5f, 12f)
                lineTo(2f, 14.2f)
                close()
            }
        }.build()
    }

    /** 接收端：向下箭头落入托盘。 */
    val Receive: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Receive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // 竖干
                moveTo(12f, 3.5f)
                lineTo(12f, 13.5f)
                // 箭头
                moveTo(7.5f, 9f)
                lineTo(12f, 13.5f)
                lineTo(16.5f, 9f)
                // 托盘
                moveTo(4.5f, 19f)
                lineTo(19.5f, 19f)
            }
        }.build()
    }

    /** 设置：两条带滑块的横线（调节杆）。 */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.5f, 8.5f)
                lineTo(20.5f, 8.5f)
                moveTo(3.5f, 15.5f)
                lineTo(20.5f, 15.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                // 滑块 1（中心 9, 8.5）
                moveTo(6.6f, 8.5f)
                arcToRelative(2.4f, 2.4f, 0f, true, true, 4.8f, 0f)
                arcToRelative(2.4f, 2.4f, 0f, true, true, -4.8f, 0f)
                close()
                // 滑块 2（中心 15, 15.5）
                moveTo(12.6f, 15.5f)
                arcToRelative(2.4f, 2.4f, 0f, true, true, 4.8f, 0f)
                arcToRelative(2.4f, 2.4f, 0f, true, true, -4.8f, 0f)
                close()
            }
        }.build()
    }
}
