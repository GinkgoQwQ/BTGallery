package com.ginkgoqwq.btgallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BlueLight,
    onPrimary = Color.White,
    primaryContainer = BlueLightContainer,
    onPrimaryContainer = OnBlueLightContainer,
    secondary = TealLight,
    onSecondary = Color.White,
    secondaryContainer = TealLightContainer,
    onSecondaryContainer = OnTealLightContainer,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueDark,
    onPrimary = OnBlueDark,
    primaryContainer = BlueDarkContainer,
    onPrimaryContainer = OnBlueDarkContainer,
    secondary = TealDark,
    onSecondary = OnTealDark,
    secondaryContainer = TealDarkContainer,
    onSecondaryContainer = OnTealDarkContainer,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

/**
 * 全局主题。
 *
 * [dynamicColor] 默认关闭：动态取色（Android 12+ 会跟随壁纸变色）虽然好看，
 * 但会让「蓝牙蓝」品牌色失效。这里固定用我们的配色，保证两端界面风格一致。
 */
@Composable
fun BluetoothConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
