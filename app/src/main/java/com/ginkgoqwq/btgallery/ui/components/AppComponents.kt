package com.ginkgoqwq.btgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ginkgoqwq.btgallery.ui.components.nav.FloatingBottomBar
import com.ginkgoqwq.btgallery.ui.components.nav.FloatingBottomBarItem
import com.ginkgoqwq.btgallery.ui.components.nav.LocalNavContentColor
import com.ginkgoqwq.btgallery.ui.components.nav.navItemMinWidth
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixDivider
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgress
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * ─────────────────────────────────────────────────────────────────────────
 *  组件层（Miuix 单一风格）
 *
 *  界面只调用这里导出的 App* 组件，不直接依赖 Miuix 的具体组件与配色。
 *  好处：配色与样式只在这一处定义，改观感不必动各个界面。
 *
 *  用 C++ 类比：这是一层「接口 + 实现」，各界面只依赖接口。
 * ─────────────────────────────────────────────────────────────────────────
 */

// ========================= 语义配色 =========================

/** 页面背景色。 */
@Composable
fun appBackground(): Color = MiuixTheme.colorScheme.background

/** 主文字色。 */
@Composable
fun appOnSurface(): Color = MiuixTheme.colorScheme.onSurface

/** 次要说明文字色。 */
@Composable
fun appOnSurfaceVariant(): Color = MiuixTheme.colorScheme.onSurfaceVariantSummary

/** 强调色 / 主色。 */
@Composable
fun appPrimary(): Color = MiuixTheme.colorScheme.primary

/** 强调色之上的文字色。 */
@Composable
fun appOnPrimary(): Color = MiuixTheme.colorScheme.onPrimary

/** 强调色容器背景（用于状态胶囊）。 */
@Composable
fun appPrimaryContainer(): Color = MiuixTheme.colorScheme.primaryContainer

/** 强调色容器上的文字色。 */
@Composable
fun appOnPrimaryContainer(): Color = MiuixTheme.colorScheme.onPrimaryContainer

/** 卡片 / 容器背景色。 */
@Composable
fun appSurfaceContainer(): Color = MiuixTheme.colorScheme.surfaceContainer

// ========================= 文字 =========================

/** 语义化文字级别，避免界面直接依赖 Miuix 的 TextStyle 类型。 */
enum class AppTextStyle { Title, Subtitle, Body, Caption, ButtonLabel }

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.Body,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null
) {
    val textStyles = MiuixTheme.textStyles
    val resolved = when (style) {
        AppTextStyle.Title -> textStyles.title1
        AppTextStyle.Subtitle -> textStyles.body1
        AppTextStyle.Body -> textStyles.body1
        AppTextStyle.Caption -> textStyles.footnote1
        AppTextStyle.ButtonLabel -> textStyles.button
    }
    MiuixText(
        text = text,
        modifier = modifier,
        color = color ?: MiuixTheme.colorScheme.onSurface,
        style = resolved,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

// ========================= 容器 =========================

/** 卡片容器。[containerColor] 为 null 时使用默认卡片色。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    MiuixCard(
        modifier = modifier,
        colors = if (containerColor != null) {
            MiuixCardDefaults.defaultColors(color = containerColor)
        } else {
            MiuixCardDefaults.defaultColors()
        },
        content = content
    )
}

/** 横向分割线。 */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    MiuixDivider(modifier = modifier)
}

/** 顶部应用栏。 */
@Composable
fun AppTopBar(title: String) {
    MiuixTopAppBar(title = title)
}

/** 页面骨架：顶部栏 + 底部栏。 */
@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    MiuixScaffold(
        topBar = topBar,
        bottomBar = bottomBar,
        content = content
    )
}

// ========================= 控件 =========================

/** 按钮。[primary] 为 true 时使用强调色填充，否则为次级样式。 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true
) {
    MiuixButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = if (primary) {
            MiuixButtonDefaults.buttonColorsPrimary()
        } else {
            MiuixButtonDefaults.buttonColors()
        }
    ) {
        MiuixText(text)
    }
}

/** 文字按钮（低强调操作，如「删除」）。 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    MiuixTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

/** 滑块。 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    MiuixSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        steps = steps
    )
}

/**
 * 水平分段选择器（用于间隔单位切换）。
 *
 * 自绘实现：**整排先整体裁成胶囊形，再放高亮块**。
 * 之前用 M3 `SegmentedButton` 时，中间项的高亮块是矩形，与外框圆角对不齐，
 * 会在边框处露出直角。整体裁切可从根上避免这个问题。
 */
@Composable
fun AppTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pill = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(pill)
            .background(appSurfaceContainer())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(pill)
                    .background(if (selected) appPrimary() else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = label,
                    style = AppTextStyle.ButtonLabel,
                    color = if (selected) appOnPrimary() else appOnSurfaceVariant(),
                    maxLines = 1
                )
            }
        }
    }
}

/** 确定进度的水平进度条，[fraction] 取值 0f..1f。 */
@Composable
fun AppProgress(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    MiuixLinearProgress(
        progress = fraction,
        modifier = modifier.fillMaxWidth()
    )
}

// ========================= 底部导航 =========================

/** 语义化图标，避免界面直接依赖具体图标资源。 */
enum class AppIcon { Home, Send, Receive, Settings }

private fun iconVector(icon: AppIcon) = when (icon) {
    AppIcon.Home -> AppIcons.Home
    AppIcon.Send -> AppIcons.Send
    AppIcon.Receive -> AppIcons.Receive
    AppIcon.Settings -> AppIcons.Settings
}

/** 一个底部导航项。 */
data class AppNavItem(val label: String, val icon: AppIcon)

/**
 * 底栏调度：[floating] 为 true 时渲染**悬浮胶囊底栏**，否则渲染**贴底导航栏**。
 * 与 KernelSU / HyperMusicCover 一致：一个布尔开关决定底栏形态。
 */
@Composable
fun AppBottomBar(
    items: List<AppNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    floating: Boolean,
    modifier: Modifier = Modifier
) {
    if (floating) {
        AppFloatingNavBar(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    } else {
        AppDockedNavBar(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    }
}

/** 贴底导航栏：整条贴在屏幕底部，无左右留白。 */
@Composable
fun AppDockedNavBar(
    items: List<AppNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixNavigationBar(
        modifier = modifier,
        mode = NavigationBarDisplayMode.IconAndText
    ) {
        items.forEachIndexed { index, item ->
            MiuixNavigationBarItem(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                icon = iconVector(item.icon),
                label = item.label,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 悬浮底栏：移植自 KernelSU 的实现（详见 `nav/FloatingBottomBar.kt`）。
 *
 * 特性：
 * - 指示器（胶囊）**可按住横向拖拽**，松手吸附到最近一项
 * - 指示器内会叠一份内容副本并染成强调色，因此滑动时图标/文字颜色平滑过渡
 * - 无模糊 / 玻璃效果
 */
@Composable
fun AppFloatingNavBar(
    items: List<AppNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingBottomBar(
        modifier = modifier,
        selectedIndex = selectedIndex,
        onSelected = onSelect,
        tabsCount = items.size
    ) { activateTab ->
        items.forEachIndexed { index, item ->
            FloatingBottomBarItem(
                selected = index == selectedIndex,
                onClick = { activateTab(index) },
                modifier = Modifier.navItemMinWidth()
            ) {
                // 颜色必须取自 LocalNavContentColor：
                // 指示器里那份副本会自动把它换成强调色，从而实现颜色平滑过渡
                MiuixIcon(
                    imageVector = iconVector(item.icon),
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp),
                    tint = LocalNavContentColor.current
                )
                AppText(
                    text = item.label,
                    style = AppTextStyle.Caption,
                    color = LocalNavContentColor.current,
                    maxLines = 1
                )
            }
        }
    }
}

// ========================= 设置行 =========================

/** 带开关的设置行：标题 + 说明，右侧开关。 */
@Composable
fun AppSwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SuperSwitch(
        title = title,
        summary = summary ?: "",
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled
    )
}
