package com.ginkgoqwq.btgallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ginkgoqwq.btgallery.data.AppPreferences
import com.ginkgoqwq.btgallery.data.AppStats
import com.ginkgoqwq.btgallery.ui.HomeScreen
import com.ginkgoqwq.btgallery.ui.ReceiverScreen
import com.ginkgoqwq.btgallery.ui.SenderScreen
import com.ginkgoqwq.btgallery.ui.SettingsScreen
import com.ginkgoqwq.btgallery.ui.components.AppBottomBar
import com.ginkgoqwq.btgallery.ui.components.AppIcon
import com.ginkgoqwq.btgallery.ui.components.AppNavItem
import com.ginkgoqwq.btgallery.ui.components.AppScaffold
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.AppTopBar
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/** 应用的四个页面，对应底栏四项。 */
private val NAV_ITEMS = listOf(
    AppNavItem("主页", AppIcon.Home),
    AppNavItem("发送端", AppIcon.Send),
    AppNavItem("接收端", AppIcon.Receive),
    AppNavItem("设置", AppIcon.Settings)
)

/** 每个页面自身的内边距（放在页面内部，这样滑动时不会有「白框」）。 */
private val PAGE_HORIZONTAL_PADDING = 16.dp

/**
 * 切页弹簧规格，与 KernelSU / HyperMusicCover / Miuix 一致。
 *
 * `dampingRatio = 32.31 / (2 * sqrt(322.2))` ≈ 0.9，接近临界阻尼，
 * 所以是「快速滑到位、几乎不过冲」的手感。
 */
private val PagerNavigationSpringSpec: SpringSpec<Float> = spring(
    stiffness = 322.2f,
    dampingRatio = 32.31f / (2f * sqrt(322.2f)),
    visibilityThreshold = 0.5f
)

/**
 * 用弹簧动画把 pager 滚到第 [index] 页。
 *
 * `animateScrollToPage` 默认使用系统预设缓动，手感与参考应用不一致；
 * 传入自定义弹簧后即与 KernelSU 相同。
 */
private suspend fun PagerState.navigateToPage(index: Int) {
    animateScrollToPage(page = index, animationSpec = PagerNavigationSpringSpec)
}

@Composable
fun AppRoot(
    prefs: AppPreferences,
    stats: AppStats
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else emptyArray()
    }

    // 关键：在**首次组合时同步**算一次权限状态，而不是等 LaunchedEffect。
    // 否则首帧会先渲染「正在请求蓝牙权限」，下一帧才被改成已授权，
    // 就是启动时那一闪。
    val initiallyGranted = remember {
        requiredPermissions.isEmpty() || requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var permissionsGranted by remember { mutableStateOf(initiallyGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        // 只有确实尚未授权时才弹系统对话框
        if (!initiallyGranted) permissionLauncher.launch(requiredPermissions)
    }

    if (!permissionsGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                AppText(
                    text = "正在请求蓝牙权限…\n请在弹出的对话框中选择「允许」",
                    style = AppTextStyle.Caption,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // 初始页 = 上次所在的页（重启后自动回到原地，不用手动点回去）
    val pagerState = rememberPagerState(
        initialPage = prefs.lastPage.coerceIn(0, NAV_ITEMS.size - 1),
        pageCount = { NAV_ITEMS.size }
    )
    var selectedIndex by remember { mutableIntStateOf(pagerState.currentPage) }
    // 动画进行中：此时不要用手势位置反推选中项，否则会和动画互相打架
    var isNavigating by remember { mutableStateOf(false) }
    var navJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun goToPage(index: Int) {
        if (index == selectedIndex) return
        navJob?.cancel()
        selectedIndex = index
        isNavigating = true
        navJob = scope.launch {
            pagerState.navigateToPage(index)
            isNavigating = false
        }
    }

    // 手势滑动结束后，把 pager 的真实位置反向同步到选中项；顺带记住当前页
    LaunchedEffect(pagerState.currentPage) {
        if (!isNavigating) selectedIndex = pagerState.currentPage
        prefs.lastPage = pagerState.currentPage
    }

    // 返回键：先回到第一页（与 KernelSU 行为一致）
    BackHandler(enabled = selectedIndex != 0) { goToPage(0) }

    val floating = prefs.floatingNavBarEnabled

    // 悬浮底栏的底部间距：与 KernelSU 相同的算法——
    // 有导航栏就“贴着导航栏再留 8dp”，没有导航栏（手势导航/桌面模式）则离屏幕底边 28dp。
    // 这样在两类设备上都既不会贴底、也不会浮得太高。
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val floatingBottomGap = if (navBarInset != 0.dp) 8.dp + navBarInset else 28.dp


    AppScaffold(
        topBar = { AppTopBar(NAV_ITEMS[selectedIndex].label) },
        bottomBar = {
            // 与 KernelSU 相同的结构：Scaffold 的 bottomBar 槽内再包一层 Box 做居中对齐
            Box(Modifier.fillMaxWidth()) {
                AppBottomBar(
                    items = NAV_ITEMS,
                    selectedIndex = selectedIndex,
                    onSelect = { goToPage(it) },
                    floating = floating,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(
                            // 悬浮时左右留白 28dp，底部间距按导航栏 inset 动态计算
                            if (floating) {
                                Modifier.padding(
                                    start = 28.dp,
                                    end = 28.dp,
                                    bottom = floatingBottomGap
                                )
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    ) { innerPadding ->
        // 注意：pager 本身**不加水平内边距**，否则滑动时相邻页会露出「白框」。
        // 改由每个页面自己在内部加边距，pager 就能整屏平移。
        HorizontalPager(
            state = pagerState,
            // 预组合全部页面：否则首次切页时要在动画中途现场组合目标页
            // （里面有蓝牙初始化、注册广播等开销），会直接导致掉帧。
            // KernelSU 同样用 3（= 全部页）。
            beyondViewportPageCount = NAV_ITEMS.size,
            overscrollEffect = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = PAGE_HORIZONTAL_PADDING)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                when (page) {
                    0 -> HomeScreen(stats = stats)
                    1 -> SenderScreen(stats = stats)
                    2 -> ReceiverScreen(stats = stats)
                    else -> SettingsScreen(prefs = prefs)
                }
            }
        }
    }
}
