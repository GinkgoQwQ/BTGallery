@file:OptIn(ExperimentalMaterial3Api::class)

package com.ginkgoqwq.btgallery.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider as M3Divider
import androidx.compose.material3.LinearProgressIndicator as M3LinearProgress
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as M3Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider as M3Slider
import androidx.compose.material3.Switch as M3Switch
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.material3.TopAppBar as M3TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ginkgoqwq.btgallery.ui.theme.LocalUiStyle
import com.ginkgoqwq.btgallery.ui.theme.UiStyle
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgress
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/*
 * ─────────────────────────────────────────────────────────────────────────
 *  组件包装层
 *
 *  界面只调用这里导出的 App* 组件，由它们根据 LocalUiStyle 决定渲染
 *  Material 3 还是 Miuix 组件。好处：新增界面不用写两套，新增风格也只改这里。
 *
 *  用 C++ 类比：这是一层「接口 + 两个实现」，调用方只依赖接口。
 * ─────────────────────────────────────────────────────────────────────────
 */

// ========================= 语义配色 =========================

/** 页面背景色。 */
@Composable
fun appBackground(): Color = when (LocalUiStyle.current) {
    UiStyle.Miuix -> MiuixTheme.colorScheme.background
    UiStyle.Material -> MaterialTheme.colorScheme.background
}

/** 卡片内主文字色。 */
@Composable
fun appOnSurface(): Color = when (LocalUiStyle.current) {
    UiStyle.Miuix -> MiuixTheme.colorScheme.onSurface
    UiStyle.Material -> MaterialTheme.colorScheme.onSurface
}

/** 次要说明文字色。 */
@Composable
fun appOnSurfaceVariant(): Color = when (LocalUiStyle.current) {
    UiStyle.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    UiStyle.Material -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** 强调色 / 主色。 */
@Composable
fun appPrimary(): Color = when (LocalUiStyle.current) {
    UiStyle.Miuix -> MiuixTheme.colorScheme.primary
    UiStyle.Material -> MaterialTheme.colorScheme.primary
}

/** 强调色容器背景（用于状态胶囊）。 */
@Composable
fun appPrimaryContainer(): Color = when (LocalUiStyle.current) {
    UiStyle.Miuix -> MiuixTheme.colorScheme.primaryContainer
    UiStyle.Material -> MaterialTheme.colorScheme.primaryContainer
}

/** 强调色容器上的文字色。 */
@Composable
fun appOnPrimaryContainer(): Color = when (LocalUiStyle.current) {
    UiStyle.Miuix -> MiuixTheme.colorScheme.onPrimaryContainer
    UiStyle.Material -> MaterialTheme.colorScheme.onPrimaryContainer
}

// ========================= 文字 =========================

/** 语义化文字级别，避免界面直接依赖某套组件库的 TextStyle 类型。 */
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
    when (LocalUiStyle.current) {
        UiStyle.Material -> {
            val m3 = when (style) {
                AppTextStyle.Title -> MaterialTheme.typography.titleLarge
                AppTextStyle.Subtitle -> MaterialTheme.typography.titleSmall
                AppTextStyle.Body -> MaterialTheme.typography.bodyMedium
                AppTextStyle.Caption -> MaterialTheme.typography.bodySmall
                AppTextStyle.ButtonLabel -> MaterialTheme.typography.labelLarge
            }
            M3Text(
                text = text,
                modifier = modifier,
                color = color ?: appOnSurface(),
                style = m3,
                maxLines = maxLines,
                overflow = overflow,
                textAlign = textAlign
            )
        }

        UiStyle.Miuix -> {
            val t = MiuixTheme.textStyles
            val mx = when (style) {
                AppTextStyle.Title -> t.title1
                AppTextStyle.Subtitle -> t.body1
                AppTextStyle.Body -> t.body1
                AppTextStyle.Caption -> t.footnote1
                AppTextStyle.ButtonLabel -> t.button
            }
            MiuixText(
                text = text,
                modifier = modifier,
                color = color ?: MiuixTheme.colorScheme.onSurface,
                style = mx,
                maxLines = maxLines,
                overflow = overflow,
                textAlign = textAlign
            )
        }
    }
}

// ========================= 容器 =========================

/** 卡片容器。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3Card(modifier = modifier, content = content)
        UiStyle.Miuix -> MiuixCard(modifier = modifier, content = content)
    }
}

/** 横向分割线。 */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3Divider(modifier = modifier)
        UiStyle.Miuix -> MiuixDivider(modifier = modifier)
    }
}

/** 顶部应用栏。 */
@Composable
fun AppTopBar(title: String) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3TopAppBar(title = { M3Text(title) })
        UiStyle.Miuix -> MiuixTopAppBar(title = title)
    }
}

/** 带顶部栏的页面骨架。 */
@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3Scaffold(topBar = topBar) { padding -> content(padding) }
        UiStyle.Miuix -> MiuixScaffold(topBar = topBar) { padding -> content(padding) }
    }
}

// ========================= 控件 =========================

/** 主按钮。[primary] 为 true 时使用强调色填充。 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            M3Text(text)
        }

        UiStyle.Miuix -> MiuixButton(
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
}

/** 文字按钮（低强调操作，如「删除」）。 */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        ) {
            M3Text(text)
        }

        UiStyle.Miuix -> MiuixTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        )
    }
}

/** 开关。 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )

        UiStyle.Miuix -> MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
    }
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
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            steps = steps
        )

        UiStyle.Miuix -> MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/** 水平分段选择器（用于模式切换、单位切换）。 */
@Composable
fun AppTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> SingleChoiceSegmentedButtonRow(
            modifier = modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, tabs.size)
                ) {
                    M3Text(label)
                }
            }
        }

        UiStyle.Miuix -> MiuixTabRow(
            tabs = tabs,
            selectedTabIndex = selectedIndex,
            onTabSelected = { onSelect(it) },
            modifier = modifier.fillMaxWidth()
        )
    }
}

/** 确定进度的水平进度条，[fraction] 取值 0f..1f。 */
@Composable
fun AppProgress(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    when (LocalUiStyle.current) {
        UiStyle.Material -> M3LinearProgress(
            progress = { fraction },
            modifier = modifier.fillMaxWidth()
        )

        UiStyle.Miuix -> MiuixLinearProgress(
            progress = fraction,
            modifier = modifier.fillMaxWidth()
        )
    }
}
