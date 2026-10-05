/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * 本文件移植自 KernelSU。
 *
 *   原项目：KernelSU  https://github.com/tiann/KernelSU
 *   原文件：me.weishu.kernelsu.ui.component.FloatingBottomBar
 *   原作者：weishu 及 KernelSU 贡献者
 *   原许可：GNU General Public License v3.0
 *
 * 本项目（BTGallery）同样以 GNU GPL v3.0 发布，详见根目录 LICENSE。
 *
 * 相对原实现的改动：
 *   1. 去掉全部「液态玻璃 / 模糊」分支（只保留 isBlurEnabled = false 的效果）
 *   2. 用自定义 LocalNavContentColor 替代 Miuix 的 LocalContentColor，
 *      以实现「指示器内内容变强调色」的效果；配色仍取 MiuixTheme
 *   3. 修复点击：原实现的 FloatingBottomBarItem 没有 clickable（只声明了 semantics
 *      onClick，那是无障碍用的），KernelSU 靠其自研手势检测器兼容点击。
 *      这里给 item 补上 clickable（无涟漪），与父级拖拽手势可共存。
 */

package com.ginkgoqwq.btgallery.ui.components.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/** 底栏内容的语义色（由 Bar 提供；指示器内会被换成强调色）。 */
val LocalNavContentColor = staticCompositionLocalOf { Color.Unspecified }

/**
 * 当前是否处于「指示器的内容副本」中。
 *
 * 副本只用于视觉（让颜色平滑过渡），必须**不接收触摸**：
 * 它盖在最上层，若副本里的项也参与点击，就会把触摸吃掉，
 * 导致「按住滑块无法拖动」。
 */
internal val LocalNavInOverlayCopy = staticCompositionLocalOf { false }

/**
 * 底栏里的一个可点击项。
 *
 * 注意作用域是 [RowScope]：靠 `weight(1f)` 等分宽度。
 */
@Composable
fun RowScope.FloatingBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    // 副本层不参与交互，避免遮挡父级拖拽手势
    val interactive = !LocalNavInOverlayCopy.current
    Column(
        modifier
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                onClick { onClick(); true }
            }
            // 无涟漪点击：父级 Row 的拖拽手势仍能接管横向拖动
            .then(
                if (interactive) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .focusable(enabled = interactive)
            .fillMaxHeight()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * 悬浮底栏。
 *
 * 核心机制（与 KernelSU 相同）：
 * 1. 主 [Row] 作为底栏背景，测量出总宽后算出单项宽度
 * 2. 一个**独立的指示器 Box** 叠在上面，宽度 = 单项宽，位置 = `动画值 × 单项宽`
 * 3. 指示器内部**再放一份内容副本**，用强调色渲染并反向平移对齐，
 *    再被指示器裁剪 —— 于是只有落在指示器范围内的内容显示为强调色，
 *    胶囊滑动时颜色就「平滑过渡」到相邻项上
 * 4. 位置由 [DampedDragAnimation] 驱动，因此**可以按住直接横向拖拽**
 */
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    tabsCount: Int,
    content: @Composable RowScope.((Int) -> Unit) -> Unit
) {
    val pillShape = remember { CircleShape }
    // 与 KernelSU 一致：直接取 Miuix 主题色
    val accentColor = MiuixTheme.colorScheme.primary
    val tabContentColor = MiuixTheme.colorScheme.onSurface
    val containerColor = MiuixTheme.colorScheme.surfaceContainer

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    /** 由触摸点 x 坐标反推是第几项。 */
    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            // 78/56：按住时整体放大约 1.39 倍，和 KernelSU 一致
            pressedScale = 78f / 56f,
            canDrag = { offset -> offset.x in 0f..totalWidthPx },
            onDragStarted = { position -> updateValue(indexAt(position.x).toFloat()) },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onSelectedUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
            },
            onDragCancelled = { updateValue(currentIndex.toFloat()) },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat())
                    )
                }
            }
        )
    }

    // 外部（点击/手势切页）改变选中项时，把指示器动画过去
    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDragAnimation.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (index !in 0 until tabsCount) return
        if (currentIndex != index) {
            currentIndex = index
            onSelectedUpdated(index)
        }
        dampedDragAnimation.animateToValue(index.toFloat())
    }

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart
    ) {
        // ---------- 底栏本体 ----------
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    // 去掉左右各 4dp 内边距后，再按项数等分
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .selectableGroup()
                .dropShadow(
                    shape = pillShape,
                    shadow = Shadow(radius = 10.dp, color = Color.Black, alpha = 0.1f)
                )
                .background(containerColor, pillShape)
                .then(dampedDragAnimation.modifier)
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalNavContentColor provides tabContentColor) {
                content(::activateTab)
            }
        }

        // ---------- 滑动指示器 ----------
        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        // 连续位置 × 单项宽 = 指示器位移
                        translationX = dampedDragAnimation.value * tabWidthPx
                    }
                    .clip(pillShape)
                    .background(accentColor.copy(alpha = 0.15f), pillShape)
                    .height(56.dp)
                    .width(tabWidthDp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 内容副本：强调色渲染 + 反向平移，使其与底下真实内容逐像素对齐，
                // 再由指示器裁剪 —— 只有覆盖到的部分显示强调色
                CompositionLocalProvider(
                    LocalNavContentColor provides accentColor,
                    LocalNavInOverlayCopy provides true
                ) {
                    Row(
                        Modifier
                            .clearAndSetSemantics {}
                            .wrapContentWidth(align = Alignment.Start, unbounded = true)
                            .requiredWidth(
                                with(density) { (totalWidthPx - 8.dp.toPx()).toDp() }
                            )
                            .height(56.dp)
                            .graphicsLayer {
                                translationX = -dampedDragAnimation.value * tabWidthPx
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        content = { content(::activateTab) }
                    )
                }
            }
        }
    }
}

/** 每个 tab 的最小宽度（与 KernelSU 相同）。 */
val NavItemMinWidth = 76.dp

/** 便捷写法：给 tab 项套上 KernelSU 用的最小宽度。 */
fun Modifier.navItemMinWidth(): Modifier = defaultMinSize(minWidth = NavItemMinWidth)
