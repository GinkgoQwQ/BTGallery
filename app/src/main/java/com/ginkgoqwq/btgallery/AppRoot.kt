package com.ginkgoqwq.btgallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ginkgoqwq.btgallery.ui.ReceiverScreen
import com.ginkgoqwq.btgallery.ui.SenderScreen
import com.ginkgoqwq.btgallery.ui.SettingsScreen
import com.ginkgoqwq.btgallery.ui.components.AppScaffold
import com.ginkgoqwq.btgallery.ui.components.AppTabs
import com.ginkgoqwq.btgallery.ui.components.AppText
import com.ginkgoqwq.btgallery.ui.components.AppTextStyle
import com.ginkgoqwq.btgallery.ui.components.AppTopBar
import com.ginkgoqwq.btgallery.ui.theme.UiStyleStore

/** 应用的三个页面。 */
private enum class AppTab(val label: String) {
    Sender("发送端"),
    Receiver("接收端"),
    Settings("设置")
}

@Composable
fun AppRoot(styleStore: UiStyleStore) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AppTab.Sender) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else emptyArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (requiredPermissions.isEmpty()) {
            permissionsGranted = true
            return@LaunchedEffect
        }
        val all = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (all) permissionsGranted = true else permissionLauncher.launch(requiredPermissions)
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

    AppScaffold(
        topBar = { AppTopBar("蓝牙图片传输") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            AppTabs(
                tabs = AppTab.entries.map { it.label },
                selectedIndex = AppTab.entries.indexOf(tab),
                onSelect = { tab = AppTab.entries[it] }
            )

            Spacer(Modifier.height(12.dp))

            when (tab) {
                AppTab.Sender -> SenderScreen()
                AppTab.Receiver -> ReceiverScreen()
                AppTab.Settings -> SettingsScreen(styleStore)
            }
        }
    }
}
