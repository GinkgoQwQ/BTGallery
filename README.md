# 蓝牙相册（BluetoothGallery）

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![minSdk](https://img.shields.io/badge/minSdk-26-brightgreen)](https://developer.android.com/about/versions/oreo)

一个**完全基于蓝牙**的 Android 图片传输与展示系统。

发送端通过蓝牙把手机本地图片发到接收端；接收端自动保存、加入图片库并轮播播放。
整个方案**不依赖 Wi-Fi、局域网或任何 IP 网络**，适用于无网络环境以及wifi损坏的设备

后续如有需求，可考虑添加局域网传输功能

## 目录

- [功能](#功能)
- [技术栈](#技术栈)
- [通信协议](#通信协议)
- [项目结构](#项目结构)
- [构建与运行](#构建与运行)
- [双机联调步骤](#双机联调步骤)
- [权限](#权限)
- [路线图](#路线图)
- [已知限制](#已知限制)
- [许可证](#许可证)

---

## 功能

### 发送端

- 扫描附近设备，通过 **Bluetooth Classic / RFCOMM** 连接接收端
- 选择手机本地图片，分块发送（带 **CRC32 校验**，保证完整性）
- 查看接收端已有图片列表，**带缩略图预览**（只传几十 KB，不传原图）
- 删除接收端的图片
- 实时显示发送进度

### 接收端

- **持续监听**连接（一个连接结束后自动等待下一个）
- 收到图片自动保存到本地，并自动加入图片库
- 收到文件先写 `.part`，通过 **大小 + CRC32 双重校验** 后才改名为正式文件
- 自动轮播，间隔可调（**秒 / 分 / 时**）
- **全屏轮播**：隐藏系统栏、保持屏幕常亮、支持横竖屏自适应
- 播放**完全不依赖发送端**，只用本地已保存的图片

---

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 异步 | Kotlin Coroutines |
| 图片加载 | Coil 2.6 |
| 传输 | Bluetooth Classic / RFCOMM（`BluetoothSocket` / `BluetoothServerSocket`） |
| 构建 | Gradle（AGP 9.x，Kotlin 2.2） |

**SDK 配置**

- `minSdk = 26`
- `compileSdk = 37`
- `targetSdk = 37`

> 选择 `minSdk 26` 的原因之一：自适应图标（Adaptive Icon）正是 API 26 引入的，
> 因此应用图标可以用**纯矢量**实现，全设备覆盖且任意分辨率不糊。

**明确不使用**：Wi-Fi、局域网、HTTP / WebSocket / TCP / UDP、Flutter / React Native、Rust、C++。
（`kotlinx-coroutines` 底层虽基于 JVM 线程，但业务层不涉及任何 IP 网络通信。）

---

## 通信协议

收发双方使用一套**自定义二进制协议**，定义在
[`transfer/Protocol.kt`](app/src/main/java/com/example/bluetoothimagetransfer/transfer/Protocol.kt)。

### 帧格式

所有整数均为**大端序（Big-Endian）**。

| 字段 | 长度 | 说明 |
|---|---|---|
| Magic | 4 B | 固定 `"BTIM"`，用于快速识别本协议 |
| Version | 1 B | 当前 `0x01` |
| Command | 1 B | 命令类型，见下表 |
| Length | 4 B | payload 字节数（不含 CRC） |
| CRC32 | 4 B | payload 的 CRC32 校验值 |
| Payload | 变长 | 命令参数 / 数据 |

### 命令表

| 命令 | 值 | 方向 | 说明 |
|---|---|---|---|
| `HELLO` | `0x01` | 发送端 → 接收端 | 握手 |
| `HELLO_ACK` | `0x02` | 接收端 → 发送端 | 握手应答 |
| `GET_DEVICE_INFO` | `0x03` | — | 预留 |
| `DEVICE_INFO` | `0x04` | — | 预留 |
| `GET_FILE_LIST` | `0x10` | 发送端 → 接收端 | 请求图片列表 |
| `FILE_LIST` | `0x11` | 接收端 → 发送端 | 返回图片列表 |
| `GET_THUMBNAIL` | `0x20` | 发送端 → 接收端 | 请求缩略图 |
| `THUMBNAIL` | `0x21` | 接收端 → 发送端 | 返回缩略图（JPEG） |
| `GET_FILE` | `0x28` | — | 预留（下载原图） |
| `SEND_FILE` | `0x30` | 发送端 → 接收端 | 开始发送文件（元信息） |
| `FILE_DATA` | `0x31` | 发送端 → 接收端 | 文件数据块（8 KB/块） |
| `FILE_FINISH` | `0x32` | 发送端 → 接收端 | 发完，携带 CRC32 |
| `DELETE_FILE` | `0x40` | 发送端 → 接收端 | 请求删除 |
| `DELETE_RESULT` | `0x41` | 接收端 → 发送端 | 删除结果 |
| `FILE_STATUS` | `0x50` | 接收端 → 发送端 | 单文件传输结果 |
| `ERROR` | `0x7F` | 双向 | 未知命令 / 错误 |

### 文件传输流程

```
发送端                                       接收端
  │  SEND_FILE（id/name/size/mime/offset）  ──►
  │  FILE_DATA（8KB 块） × N                ──►   写入 *.part，边写边算 CRC
  │  FILE_FINISH（totalSize/crc32）         ──►   校验大小 + CRC
  │                                          ◄──  FILE_STATUS（成功/失败）
```

校验不通过时接收端**删除 `.part` 文件**并返回失败原因，不会污染图片库。

### 缩略图生成

接收端收到 `GET_THUMBNAIL` 后**现场生成**缩略图，采用两级降采样避免 OOM：

1. `inJustDecodeBounds` 只读图片尺寸，不分配像素内存
2. 计算 2 的幂次 `inSampleSize` 粗降采样
3. 精确缩放到最长边 ≤ 320 px，压缩为 JPEG（约 10–40 KB）

---

## 项目结构

```
app/src/main/java/com/example/bluetoothimagetransfer/
├── MainActivity.kt                  # 入口 Activity
├── AppRoot.kt                       # 顶层 Scaffold + 发送/接收模式切换
├── bluetooth/
│   ├── BluetoothConfig.kt           # TAG / SERVICE_NAME / 服务 UUID
│   ├── BluetoothConnector.kt        # 发送端：双向连接（读写协议帧）
│   └── BluetoothServerManager.kt    # 接收端：循环监听连接
├── transfer/
│   ├── Protocol.kt                  # 帧格式 + CRC32 编解码
│   ├── PayloadCodec.kt              # 各命令 payload 编解码
│   ├── SenderSession.kt             # 发送端命令封装（请求-响应）
│   ├── ReceiverSession.kt           # 接收端命令循环（服务端）
│   └── FileTransfer.kt              # 早期协议实现（已废弃，保留参考）
├── data/
│   ├── Models.kt                    # DeviceItem / ImageItem
│   └── MediaRepository.kt           # 图片扫描 / 删除 / 缩略图生成
└── ui/
    ├── SenderScreen.kt              # 发送端界面
    ├── ReceiverScreen.kt            # 接收端界面（含全屏轮播）
    ├── AppIconPreview.kt            # 应用图标预览（仅设计时）
    ├── components/CommonUi.kt       # SectionCard / StatusPill / EmptyHint
    └── theme/                       # 配色 / 主题 / 字体
```

**测试**

```
app/src/test/          # JVM 单元测试：协议与 payload 编解码
app/src/androidTest/   # 真机测试：缩略图链路、Coil 解码
```

---

## 构建与运行

**环境要求**

- Android Studio（建议最新稳定版）
- JDK 11+
- 两台支持蓝牙的 Android 6.0（API 26）以上真机

**命令行构建**

```bash
# 编译 Debug 包
./gradlew assembleDebug

# 运行 JVM 单元测试（协议编解码，无需设备）
./gradlew test

# 运行真机测试（需要连接设备）
./gradlew connectedAndroidTest
```

Windows 下把 `./gradlew` 换成 `gradlew.bat`。

---

## 双机联调步骤

1. 两台手机分别安装本应用，并**配对**（系统设置里完成蓝牙配对）。
2. **A 机（接收端）**：切到「接收端」→ 点「开始监听」。
   首次会请求「附近的设备」权限，选择**允许**。
3. **B 机（发送端）**：切到「发送端」→ 点「扫描附近设备」→
   在列表里点 A 机的设备名 → 状态变为「已连接」。
4. **看列表**：发送端点「刷新列表」→ 显示 A 机图片的**缩略图 + 文件名 + 大小**。
5. **发图片**：发送端点「选择图片」选一张 → 点「发送图片」→ 进度走完，
   A 机自动接收并加入轮播。
6. **删图片**：发送端点某张缩略图下的「删除」→ A 机该图消失，列表自动刷新。
7. **全屏轮播（在 A 机）**：点「全屏轮播」→ 纯图片铺满整屏，
   单击屏幕可切换控制条（上一张 / 下一张 / 暂停 / 退出），4 秒无操作自动隐藏。

> 排查提示：调试日志 TAG 统一为 `BluetoothImageTransfer`，可用
> `adb logcat -s BluetoothImageTransfer` 观察协议收发过程。

---

## 权限

在 [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) 中声明：

| 权限 | 生效范围 | 用途 |
|---|---|---|
| `BLUETOOTH` | API ≤ 30 | 经典蓝牙 |
| `BLUETOOTH_ADMIN` | API ≤ 30 | 扫描 / 开关蓝牙 |
| `ACCESS_FINE_LOCATION` | API ≤ 30 | Android 6–11 扫描蓝牙所需 |
| `BLUETOOTH_SCAN` | API ≥ 31 | 扫描设备（`neverForLocation`） |
| `BLUETOOTH_CONNECT` | API ≥ 31 | 连接 / 传输 |

另声明 `uses-feature android.hardware.bluetooth required="true"`。

---

## 路线图

- [x] 自定义二进制协议（Magic / Version / Command / Length / CRC32）
- [x] 发送端查看 / 删除接收端图片
- [x] 缩略图传输（两级降采样，只传小图）
- [x] 接收端全屏轮播 + 横竖屏自适应
- [x] 轮播间隔支持秒 / 分 / 时
- [ ] Room 数据库管理图片元信息（id / 路径 / 尺寸 / MIME / 排序）
- [ ] 断点续传（协议已预留 `offset` 字段）
- [ ] 接收端 Foreground Service（后台持续接收）
- [ ] 视频支持

---

## 已知限制

- **模式互斥**：一台设备上一次只能扮演发送端或接收端；切换模式会重置该页状态
  （接收端切走后会停止监听）。
- **缩略图串行拉取**：一条蓝牙连接是单一字节流，并发读取会导致协议错乱，
  因此缩略图逐张获取，图片很多时会有轻微「逐张出现」的延迟。
- **尚未断点续传**：传输中断需重新发送整张图片（协议已预留 `offset`）。
- **旧协议文件 `FileTransfer.kt`** 已不再被调用，保留作为演进参考，后续会移除。

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源。

```
Copyright (c) 2026 GinkgoQwQ
```

你可以自由使用、修改、分发本软件，包括商业用途，
只需保留原始的版权声明与许可声明。软件按「原样」提供，不附带任何担保。
