package com.ginkgoqwq.btgallery.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 全局运行状态与统计。
 *
 * 分两类：
 * - **累计计数**（[sentCount] / [receivedCount]）：持久化到 SharedPreferences，重启后保留
 * - **实时状态**（[senderStatus] / [receiverStatus]）：仅内存，进程结束后重置
 *
 * 所有字段都是 Compose State，读写会触发/参与重组，
 * 因此发送端、接收端更新状态时，主页会立刻跟着刷新。
 */
class AppStats(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var sentState: Int by mutableStateOf(prefs.getInt(KEY_SENT, 0))
    private var receivedState: Int by mutableStateOf(prefs.getInt(KEY_RECEIVED, 0))

    /** 累计已发送文件数。 */
    var sentCount: Int
        get() = sentState
        set(value) {
            if (value == sentState) return
            sentState = value
            prefs.edit().putInt(KEY_SENT, value).apply()
        }

    /** 累计已接收文件数。 */
    var receivedCount: Int
        get() = receivedState
        set(value) {
            if (value == receivedState) return
            receivedState = value
            prefs.edit().putInt(KEY_RECEIVED, value).apply()
        }

    /** 发送端当前状态（如「已连接：Xiaomi 15」）。 */
    var senderStatus: String by mutableStateOf("未连接")

    /** 接收端当前状态（如「监听中…」）。 */
    var receiverStatus: String by mutableStateOf("未监听")

    /** 成功发送一个文件。 */
    fun onFileSent() {
        sentCount += 1
    }

    /** 成功接收一个文件。 */
    fun onFileReceived() {
        receivedCount += 1
    }

    private companion object {
        const val PREFS_NAME = "btgallery_stats"
        const val KEY_SENT = "sent_count"
        const val KEY_RECEIVED = "received_count"
    }
}
