package com.ginkgoqwq.btgallery.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ginkgoqwq.btgallery.bluetooth.BluetoothConfig
import com.ginkgoqwq.btgallery.bluetooth.BluetoothConnector
import com.ginkgoqwq.btgallery.data.AppStats
import com.ginkgoqwq.btgallery.data.DeviceItem
import com.ginkgoqwq.btgallery.transfer.RemoteFileInfo
import com.ginkgoqwq.btgallery.transfer.SenderSession
import com.ginkgoqwq.btgallery.ui.components.AppButton
import com.ginkgoqwq.btgallery.ui.components.AppCard
import com.ginkgoqwq.btgallery.ui.components.AppDivider
import com.ginkgoqwq.btgallery.ui.components.AppProgress
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextButton
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.EmptyHint
import com.ginkgoqwq.btgallery.ui.components.SectionCard
import com.ginkgoqwq.btgallery.ui.components.appOnSurfaceVariant
import com.ginkgoqwq.btgallery.ui.components.appPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SenderScreen(stats: AppStats) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bluetoothAdapter = remember {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    val connector = remember { BluetoothConnector() }
    DisposableEffect(Unit) {
        onDispose { connector.disconnect() }
    }

    var devices by remember { mutableStateOf<List<DeviceItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var sendProgress by remember { mutableStateOf(0f) }
    var isSending by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("未连接") }

    // 同步到全局状态，主页可以实时看到发送端在干什么
    SideEffect { stats.senderStatus = statusText }

    // 连接模块是否展开（连接成功后自动折叠）
    var connectionExpanded by remember { mutableStateOf(true) }

    // 接收端图片管理状态
    var remoteFiles by remember { mutableStateOf<List<RemoteFileInfo>>(emptyList()) }
    var thumbnails by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }
    // 缩略图拉取失败的 id，用于把「加载中…」变成明确的「无缩略图」
    var failedThumbs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isRemoteBusy by remember { mutableStateOf(false) }
    var remoteStatus by remember { mutableStateOf("请先连接接收端") }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        // 某些机型未授权时读取 name 会抛 SecurityException，这里统一兜底
                        val name = try {
                            device?.name
                        } catch (_: SecurityException) {
                            null
                        }
                        val address = device?.address ?: return
                        if (devices.none { it.address == address }) {
                            devices = devices + DeviceItem(name ?: "未知设备", address, device)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    /**
     * 逐个拉取缺失的缩略图。
     *
     * 必须串行：一条蓝牙连接上同时只有一个字节流，
     * 并发 readMessage 会互相踩踏导致解析错乱。
     *
     * 失败（对端不认 GET_THUMBNAIL、超时等）会被记录到 [failedThumbs] 并写日志，
     * 这样界面上会显示「无缩略图」而不是永远「加载中…」。
     */
    suspend fun fetchThumbnails(session: SenderSession, list: List<RemoteFileInfo>) {
        for (file in list) {
            if (thumbnails.containsKey(file.id)) continue
            try {
                val bytes = session.getThumbnail(file.id)
                if (bytes != null) {
                    withContext(Dispatchers.Main) {
                        thumbnails = thumbnails + (file.id to bytes)
                    }
                } else {
                    Log.w(BluetoothConfig.TAG, "缩略图为空：${file.id}")
                    withContext(Dispatchers.Main) { failedThumbs = failedThumbs + file.id }
                }
            } catch (e: Exception) {
                Log.w(BluetoothConfig.TAG, "获取缩略图失败：${file.id} -> ${e.message}")
                withContext(Dispatchers.Main) { failedThumbs = failedThumbs + file.id }
            }
        }
    }

    /** 刷新接收端图片列表 + 缩略图。 */
    fun refreshRemoteList() {
        if (!connector.isConnected) {
            remoteStatus = "请先连接接收端"
            return
        }
        if (isRemoteBusy) return
        isRemoteBusy = true
        remoteStatus = "刷新中…"
        // 重新刷新时清掉上一次的失败标记，允许重试
        failedThumbs = emptySet()
        scope.launch(Dispatchers.IO) {
            try {
                val session = SenderSession(connector)
                val list = session.getFileList()
                withContext(Dispatchers.Main) {
                    remoteFiles = list
                    remoteStatus = "共 ${list.size} 张图片"
                }
                fetchThumbnails(session, list)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    remoteStatus = "刷新失败：${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) { isRemoteBusy = false }
            }
        }
    }

    /** 删除接收端某张图片，成功后刷新列表。 */
    fun deleteRemote(id: String) {
        if (isRemoteBusy) return
        isRemoteBusy = true
        remoteStatus = "删除中…"
        scope.launch(Dispatchers.IO) {
            try {
                val session = SenderSession(connector)
                val result = session.deleteFile(id)
                withContext(Dispatchers.Main) {
                    remoteStatus = if (result.success) "已删除：$id" else "删除失败：${result.error}"
                    if (result.success) {
                        thumbnails = thumbnails - id
                        failedThumbs = failedThumbs - id
                    }
                }
                if (result.success) {
                    val list = session.getFileList()
                    withContext(Dispatchers.Main) { remoteFiles = list }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    remoteStatus = "删除失败：${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) { isRemoteBusy = false }
            }
        }
    }

    // 整页单一滚动容器：连接卡片 / 发送卡片 / 图片网格共处同一个 LazyVerticalGrid，
    // 头部卡片用 GridItemSpan(maxLineSpan) 横跨所有列，因此整页可以一起上下滑。
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---------- ① 连接 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard(
                title = "① 连接接收端",
                collapsible = true,
                expanded = connectionExpanded,
                onToggle = { connectionExpanded = !connectionExpanded },
                trailing = {
                    AppText(
                        text = statusText,
                        style = AppTextStyle.Caption,
                        color = appOnSurfaceVariant(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            ) {
                AppButton(
                    text = if (isScanning) "扫描中…" else "扫描附近设备",
                    onClick = {
                        if (!isScanning) {
                            devices = emptyList()
                            isScanning = true
                            try {
                                if (bluetoothAdapter?.isEnabled == true) {
                                    bluetoothAdapter.startDiscovery()
                                }
                            } catch (_: SecurityException) {
                                isScanning = false
                                statusText = "缺少蓝牙扫描权限"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning
                )

                if (devices.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    AppCard {
                        LazyColumn(Modifier.heightIn(max = 160.dp)) {
                            items(devices) { item ->
                                val connected = connectedDevice?.address == item.address
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    try {
                                                        bluetoothAdapter?.cancelDiscovery()
                                                    } catch (_: SecurityException) {
                                                        // 忽略：取消扫描失败不影响后续连接
                                                    }
                                                    connector.connect(item.device)
                                                    withContext(Dispatchers.Main) {
                                                        connectedDevice = item.device
                                                        statusText = "已连接：${item.name}"
                                                        remoteStatus = "已连接，可刷新列表"
                                                        // 换了接收端，旧缩略图全部失效
                                                        remoteFiles = emptyList()
                                                        thumbnails = emptyMap()
                                                        failedThumbs = emptySet()
                                                        // 连接成功后自动折叠连接模块
                                                        connectionExpanded = false
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        statusText = "连接失败：${e.message}"
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        AppText(
                                            text = item.name,
                                            style = AppTextStyle.Body,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        AppText(
                                            text = item.address,
                                            style = AppTextStyle.Caption,
                                            color = appOnSurfaceVariant()
                                        )
                                    }
                                    AppText(
                                        text = if (connected) "已连接" else "连接",
                                        style = AppTextStyle.ButtonLabel,
                                        color = appPrimary()
                                    )
                                }
                                if (item != devices.last()) {
                                    AppDivider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------- ② 发送图片 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard("② 发送图片") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = "选择图片",
                        onClick = { pickImageLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        primary = false
                    )
                    AppButton(
                        text = if (isSending) "发送中…" else "发送图片",
                        onClick = {
                            val uri = selectedImageUri ?: return@AppButton
                            if (!connector.isConnected) return@AppButton
                            scope.launch(Dispatchers.IO) {
                                isSending = true
                                sendProgress = 0f
                                try {
                                    val session = SenderSession(connector)
                                    session.sendFile(context, uri) { sent, total ->
                                        if (total > 0) sendProgress = sent.toFloat() / total
                                    }
                                    val list = session.getFileList()
                                    withContext(Dispatchers.Main) {
                                        statusText = "发送完成"
                                        stats.onFileSent()
                                        remoteFiles = list
                                        remoteStatus = "共 ${list.size} 张图片"
                                    }
                                    fetchThumbnails(session, list)
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        statusText = "发送失败：${e.message}"
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) { isSending = false }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedImageUri != null &&
                                connector.isConnected &&
                                !isSending &&
                                !isRemoteBusy
                    )
                }

                if (selectedImageUri != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "待发送的图片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        AppText(
                            text = "已选择待发送图片",
                            style = AppTextStyle.Caption,
                            color = appOnSurfaceVariant()
                        )
                    }
                }

                if (isSending) {
                    Spacer(Modifier.height(10.dp))
                    AppProgress(fraction = sendProgress)
                    Spacer(Modifier.height(4.dp))
                    AppText(
                        text = "发送进度：${(sendProgress * 100).toInt()}%",
                        style = AppTextStyle.Caption
                    )
                }
            }
        }

        // ---------- ③ 接收端图片 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    text = "③ 接收端图片",
                    style = AppTextStyle.Subtitle,
                    color = appPrimary()
                )
                Spacer(Modifier.width(10.dp))
                AppText(
                    text = remoteStatus,
                    style = AppTextStyle.Caption,
                    color = appOnSurfaceVariant(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AppButton(
                    text = "刷新列表",
                    onClick = { refreshRemoteList() },
                    enabled = connector.isConnected && !isRemoteBusy
                )
            }
        }

        if (remoteFiles.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyHint("暂无接收端图片数据\n连接后点「刷新列表」获取")
            }
        } else {
            items(remoteFiles, key = { it.id }) { file ->
                RemoteImageCard(
                    file = file,
                    thumbnail = thumbnails[file.id],
                    failed = file.id in failedThumbs,
                    deleteEnabled = !isRemoteBusy,
                    onDelete = { deleteRemote(file.id) }
                )
            }
        }
    }
}

/**
 * 接收端单张图片卡片：缩略图 + 文件名 + 大小 + 删除。
 *
 * @param failed 缩略图拉取失败（此时显示「无缩略图」，而非永远「加载中」）
 */
@Composable
private fun RemoteImageCard(
    file: RemoteFileInfo,
    thumbnail: ByteArray?,
    failed: Boolean,
    deleteEnabled: Boolean,
    onDelete: () -> Unit
) {
    AppCard {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AppText(
                        text = if (failed) "无缩略图" else "加载中…",
                        style = AppTextStyle.Caption,
                        color = Color.White
                    )
                }
            }
            Column(Modifier.padding(10.dp)) {
                AppText(
                    text = file.name,
                    style = AppTextStyle.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AppText(
                    text = formatSize(file.size),
                    style = AppTextStyle.Caption,
                    color = appOnSurfaceVariant()
                )
                AppTextButton(
                    text = "删除",
                    onClick = onDelete,
                    enabled = deleteEnabled
                )
            }
        }
    }
}

/** 把字节数格式化成人类可读大小。 */
private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
