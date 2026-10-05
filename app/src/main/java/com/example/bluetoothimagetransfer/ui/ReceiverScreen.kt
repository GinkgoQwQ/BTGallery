package com.example.bluetoothimagetransfer.ui

import android.bluetooth.BluetoothManager
import android.content.Context
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.bluetoothimagetransfer.bluetooth.BluetoothServerManager
import com.example.bluetoothimagetransfer.data.ImageItem
import com.example.bluetoothimagetransfer.data.MediaRepository
import com.example.bluetoothimagetransfer.transfer.ReceiverSession
import com.example.bluetoothimagetransfer.ui.components.EmptyHint
import com.example.bluetoothimagetransfer.ui.components.SectionCard
import com.example.bluetoothimagetransfer.ui.components.StatusPill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 轮播间隔的单位。默认「小时」，符合长时间展示（如电子相框）的场景。 */
private enum class IntervalUnit(
    val label: String,
    val min: Int,
    val max: Int,
    val secondsPerUnit: Long
) {
    Seconds("秒", 1, 30, 1L),
    Minutes("分", 1, 60, 60L),
    Hours("时", 1, 24, 3600L);

    fun clamp(value: Int): Int = value.coerceIn(min, max)

    fun toMillis(value: Int): Long = value * secondsPerUnit * 1000L
}

@Composable
fun ReceiverScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember { MediaRepository(context) }
    val serverManager = remember { BluetoothServerManager() }
    DisposableEffect(Unit) {
        onDispose { serverManager.close() }
    }

    var isListening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("未监听") }
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    // 轮播间隔：数值 + 单位（默认 1 小时）
    var intervalUnit by remember { mutableStateOf(IntervalUnit.Hours) }
    var intervalValue by remember { mutableIntStateOf(1) }

    // 是否正在全屏轮播（用独立 Dialog 窗口覆盖整屏）
    var isFullscreen by remember { mutableStateOf(false) }

    val intervalMillis = intervalUnit.toMillis(intervalValue)

    fun refreshImages() {
        images = repository.listImages()
        if (currentIndex >= images.size) currentIndex = 0
    }

    LaunchedEffect(Unit) { refreshImages() }

    // 轮播计时：无论是否全屏都在走，全屏 Dialog 只是「显示当前这张」
    LaunchedEffect(images, intervalMillis, isPlaying) {
        if (!isPlaying || images.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(intervalMillis)
            currentIndex = (currentIndex + 1) % images.size
        }
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        statusText = "监听中…"
        scope.launch(Dispatchers.IO) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter
            serverManager.listenLoop(
                adapter = adapter,
                onSession = { input, output ->
                    scope.launch(Dispatchers.Main) { statusText = "已连接" }
                    ReceiverSession(
                        inputStream = input,
                        outputStream = output,
                        repository = repository,
                        onFilesChanged = {
                            scope.launch(Dispatchers.Main) {
                                refreshImages()
                                statusText = "图片库已更新"
                            }
                        }
                    ).run()
                },
                onError = { e ->
                    scope.launch(Dispatchers.Main) {
                        statusText = "连接错误：${e.message}"
                    }
                }
            )
            scope.launch(Dispatchers.Main) {
                isListening = false
                statusText = "已停止监听"
            }
        }
    }

    fun stopListening() {
        serverManager.close()
        isListening = false
        statusText = "已停止监听"
    }

    // 整体单一滚动容器：头部卡片与图片网格都在同一个 LazyVerticalGrid 里，
    // 因此整页可以一起上下滑动，而不是只有底部一小条能滚。
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---------- ① 接收 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard("① 接收") {
                StatusPill(statusText)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { if (isListening) stopListening() else startListening() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isListening) "停止监听" else "开始监听")
                }
            }
        }

        // ---------- ② 轮播 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard("② 轮播播放") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isPlaying) "暂停轮播" else "开始轮播")
                    }
                    Button(
                        onClick = { isFullscreen = true },
                        modifier = Modifier.weight(1f),
                        enabled = images.isNotEmpty()
                    ) {
                        Text("全屏轮播")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 单位选择：秒 / 分 / 时
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntervalUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = intervalUnit == unit,
                            onClick = {
                                intervalUnit = unit
                                intervalValue = unit.clamp(intervalValue)
                            },
                            label = { Text(unit.label) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "间隔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = intervalValue.toFloat(),
                        onValueChange = {
                            intervalValue = intervalUnit.clamp(it.roundToInt())
                        },
                        valueRange = intervalUnit.min.toFloat()..intervalUnit.max.toFloat(),
                        steps = (intervalUnit.max - intervalUnit.min - 1).coerceAtLeast(0),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(
                        "${intervalValue}${intervalUnit.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ---------- 当前预览 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            if (images.isNotEmpty()) {
                val current = images[currentIndex.coerceIn(0, images.lastIndex)]
                Column {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        AsyncImage(
                            model = current.file,
                            contentDescription = current.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "当前：${current.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ---------- ③ 图片库标题 ----------
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "③ 图片库",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "共 ${images.size} 张 · 点击删除",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (images.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyHint("暂无图片\n连接发送端后会自动接收并加入轮播")
            }
        } else {
            items(images, key = { it.name }) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            repository.delete(item.file)
                            refreshImages()
                        }
                ) {
                    AsyncImage(
                        model = item.file,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // 全屏轮播：独立窗口，盖住包括顶部标题栏在内的整个界面
    if (isFullscreen) {
        FullscreenCarouselDialog(
            images = images,
            currentIndex = currentIndex,
            isPlaying = isPlaying,
            onPrev = {
                if (images.isNotEmpty()) {
                    currentIndex = (currentIndex - 1 + images.size) % images.size
                }
            },
            onNext = {
                if (images.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % images.size
                }
            },
            onTogglePlay = { isPlaying = !isPlaying },
            onExit = { isFullscreen = false }
        )
    }
}

/**
 * 全屏轮播对话框：使用独立窗口（usePlatformDefaultWidth=false + decorFitsSystemWindows=false），
 * 因此可以覆盖包括顶部标题栏在内的整个屏幕，真正纯图片全屏。
 *
 * 单击屏幕切换控件显隐；横屏/竖屏都用 ContentScale.Fit 自适应，不裁切。
 */
@Composable
private fun FullscreenCarouselDialog(
    images: List<ImageItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTogglePlay: () -> Unit,
    onExit: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }

    // 控件自动隐藏
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        // 注意：这里的 LocalView.current 是「对话框窗口」的 View，
        // 操作它的 Window 才能正确隐藏系统栏。
        val view = LocalView.current
        DisposableEffect(Unit) {
            val window = view.findDialogWindow()
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
        ) {
            if (images.isEmpty()) {
                Text(
                    "暂无图片",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val current = images[currentIndex.coerceIn(0, images.lastIndex)]
                AsyncImage(
                    model = current.file,
                    contentDescription = current.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (showControls) {
                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${currentIndex + 1} / ${images.size}",
                                color = Color.White
                            )
                            Text(
                                current.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Button(onClick = onPrev) { Text("上一张") }
                            Button(onClick = onNext) { Text("下一张") }
                            Button(onClick = onTogglePlay) {
                                Text(if (isPlaying) "暂停" else "播放")
                            }
                            Button(onClick = onExit) { Text("退出") }
                        }
                    }
                }
            }
        }
    }
}

/** 从当前 View 向上找到宿主对话框的 Window。 */
private fun View.findDialogWindow(): Window? {
    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return null
}
