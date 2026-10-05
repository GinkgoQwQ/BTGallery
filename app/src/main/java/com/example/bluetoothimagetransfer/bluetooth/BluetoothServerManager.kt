package com.example.bluetoothimagetransfer.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream

/**
 * 接收端使用：循环监听连接。
 *
 * 与旧的 listenOnce 不同，这里会在一个连接结束后继续 accept 下一个连接，
 * 直到调用 [close] 停止。适合接收端“持续服务”的场景。
 */
class BluetoothServerManager {

    private var serverSocket: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null

    @Volatile
    private var closing = false

    /**
     * 阻塞式循环监听，请放到 IO 线程调用。
     *
     * @param onSession 每个新连接会回调一次，传入该连接的输入/输出流；回调返回后关闭该连接并继续监听。
     * @param onError   非主动关闭导致的异常会回调。
     */
    fun listenLoop(
        adapter: BluetoothAdapter,
        onSession: (InputStream, OutputStream) -> Unit,
        onError: (Exception) -> Unit
    ) {
        closing = false
        val server = adapter.listenUsingRfcommWithServiceRecord(
            BluetoothConfig.SERVICE_NAME,
            BluetoothConfig.APP_UUID
        )
        serverSocket = server
        try {
            while (!closing) {
                val s = try {
                    server.accept()
                } catch (e: Exception) {
                    if (closing) break else throw e
                }
                socket = s
                try {
                    onSession(s.inputStream, s.outputStream)
                } catch (e: Exception) {
                    if (!closing) onError(e)
                } finally {
                    try { s.close() } catch (_: Exception) {}
                    socket = null
                }
            }
        } catch (e: Exception) {
            if (!closing) onError(e)
        } finally {
            try { server.close() } catch (_: Exception) {}
            serverSocket = null
        }
    }

    fun close() {
        closing = true
        try { socket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        socket = null
        serverSocket = null
    }
}
