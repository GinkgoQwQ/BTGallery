package com.ginkgoqwq.btgallery.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ginkgoqwq.btgallery.data.AppStats
import com.ginkgoqwq.btgallery.data.MediaRepository
import com.ginkgoqwq.btgallery.ui.components.AppIcons
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.SectionCard
import com.ginkgoqwq.btgallery.ui.components.appOnSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 主页顶部大卡片的高度。 */
private val HERO_CARD_HEIGHT = 132.dp

/**
 * 主页：蓝牙状态大卡片 + 运行状态 + 文件统计。
 *
 * 顶部大卡片参考 KernelSU 的 `StatusCard`：大圆角卡片 + **右下角溢出的大号半透明图标**
 * + 左上角大标题/副标题 + 左下角状态标签。
 */
@Composable
fun HomeScreen(stats: AppStats) {
    val context = LocalContext.current

    val adapter = remember {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    var bluetoothEnabled by remember { mutableStateOf(adapter?.isEnabled == true) }
    var adapterDetail by remember { mutableStateOf("") }
    var imageCount by remember { mutableIntStateOf(0) }

    // 实时跟随蓝牙开关变化（而不是只在进入页面时读一次）
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                bluetoothEnabled = adapter?.isEnabled == true
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        bluetoothEnabled = adapter?.isEnabled == true
        onDispose { context.unregisterReceiver(receiver) }
    }

    // 接收数变化时重新统计图片库（收到新图或删除后都会变）。
    // 目录扫描是文件 I/O，必须放到 IO 线程，否则会卡主线程导致掉帧。
    LaunchedEffect(stats.receivedCount) {
        imageCount = withContext(Dispatchers.IO) {
            MediaRepository(context).listImages().size
        }
    }

    // 读取本机蓝牙适配器名称。
    // 注意：adapter.address 自 Android 6 起对普通应用不可用（需系统级 LOCAL_MAC_ADDRESS）。
    // 未授权时读 name 会抛 SecurityException，这里统一兜底。
    LaunchedEffect(bluetoothEnabled) {
        adapterDetail = if (adapter == null) {
            "设备不支持蓝牙"
        } else {
            try {
                adapter.name?.takeIf { it.isNotBlank() } ?: "本机蓝牙"
            } catch (_: SecurityException) {
                "缺少蓝牙权限"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BluetoothStatusCard(
                enabled = bluetoothEnabled,
                detail = adapterDetail
            )
        }

        item {
            SectionCard("运行状态") {
                InfoRow(label = "发送端", value = stats.senderStatus)
                InfoRow(label = "接收端", value = stats.receiverStatus)
            }
        }

        item {
            SectionCard("文件统计") {
                InfoRow(label = "已发送", value = "${stats.sentCount} 个")
                InfoRow(label = "已接收", value = "${stats.receivedCount} 个")
                InfoRow(label = "本地图片库", value = "$imageCount 张")
            }
        }
    }
}

/**
 * 蓝牙状态大卡片（布局参考 KernelSU 的 `StatusCard`）。
 *
 * 用 Miuix 的 `Card` / `Text` 直接实现以保持观感一致；
 * 配色取自 `MiuixTheme.colorScheme`，因此跟随 Monet 取色。
 */
@Composable
private fun BluetoothStatusCard(
    enabled: Boolean,
    detail: String
) {
    val cs = MiuixTheme.colorScheme

    MiuixCard(
        modifier = Modifier.fillMaxWidth(),
        colors = MiuixCardDefaults.defaultColors(color = cs.secondaryContainer)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(HERO_CARD_HEIGHT)
        ) {
            // 右下角大号半透明图标：刻意溢出卡片边界，被圆角裁切
            MiuixIcon(
                imageVector = AppIcons.Bluetooth,
                contentDescription = null,
                tint = cs.primary.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 27.dp, y = 31.dp)
                    .size(110.dp)
            )

            // 左上：标题 + 副标题
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp)
                    .padding(end = 96.dp)
            ) {
                MiuixText(
                    text = if (enabled) "蓝牙已开启" else "蓝牙未开启",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSecondaryContainer
                )
                Spacer(Modifier.height(2.dp))
                MiuixText(
                    text = detail,
                    fontSize = 15.sp,
                    color = cs.onSecondaryVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 左下：状态标签
            MiuixText(
                text = if (enabled) "已开启" else "未开启",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 10.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "蓝牙已开启", widthDp = 380, heightDp = 200, showBackground = true)
@Composable
private fun BluetoothOnPreview() {
    androidx.compose.material3.MaterialTheme {
        com.ginkgoqwq.btgallery.ui.theme.AppTheme {
            Box(
                Modifier
                    .background(com.ginkgoqwq.btgallery.ui.components.appBackground())
                    .padding(16.dp)
            ) {
                BluetoothStatusCard(enabled = true, detail = "Xiaomi 15")
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "蓝牙未开启", widthDp = 380, heightDp = 200, showBackground = true)
@Composable
private fun BluetoothOffPreview() {
    androidx.compose.material3.MaterialTheme {
        com.ginkgoqwq.btgallery.ui.theme.AppTheme {
            Box(
                Modifier
                    .background(com.ginkgoqwq.btgallery.ui.components.appBackground())
                    .padding(16.dp)
            ) {
                BluetoothStatusCard(enabled = false, detail = "本机蓝牙")
            }
        }
    }
}

/** 「标签 — 值」一行。 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(text = label, style = AppTextStyle.Body)
        // weight 占满剩余宽度、内容靠右，标签不会被挤走
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            AppText(
                text = value,
                style = AppTextStyle.Body,
                color = appOnSurfaceVariant(),
                maxLines = 2
            )
        }
    }
}
