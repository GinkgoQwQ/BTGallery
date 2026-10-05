package com.ginkgoqwq.btgallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 统一风格的分区卡片：顶部一个主色小标题，下面是内容。
 * 发送端 / 接收端的每一块功能区都用它，保证两端视觉一致。
 *
 * @param collapsible 是否可折叠（标题行可点击）
 * @param expanded    当前是否展开（仅 collapsible 时有意义）
 * @param onToggle    点击标题行时回调（仅 collapsible 时使用）
 * @param trailing    标题行右侧的附加内容（如状态文字）
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    collapsible: Boolean = false,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (collapsible && onToggle != null) {
                            Modifier.clickable { onToggle() }
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = title,
                    style = AppTextStyle.Subtitle,
                    color = appPrimary(),
                    modifier = Modifier.weight(1f)
                )
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
                if (collapsible) {
                    Spacer(Modifier.width(8.dp))
                    AppText(
                        text = if (expanded) "收起" else "展开",
                        style = AppTextStyle.Caption,
                        color = appPrimary()
                    )
                }
            }

            if (collapsible) {
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        content()
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
    }
}

/** 状态胶囊标签，用于展示「已连接 / 监听中 / 未连接」这类状态。 */
@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(appPrimaryContainer(), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        AppText(
            text = text,
            style = AppTextStyle.Caption,
            color = appOnPrimaryContainer(),
            maxLines = 1
        )
    }
}

/** 空状态占位提示。 */
@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            style = AppTextStyle.Caption,
            color = appOnSurfaceVariant(),
            textAlign = TextAlign.Center
        )
    }
}
