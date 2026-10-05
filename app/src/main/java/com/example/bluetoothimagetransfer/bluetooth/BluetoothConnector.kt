package com.example.bluetoothimagetransfer.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.example.bluetoothimagetransfer.transfer.ProtocolCodec
import com.example.bluetoothimagetransfer.transfer.ProtocolMessage
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** 发送端使用：与接收端建立 RFCOMM 连接（双向：既能写也能读）。 */
class BluetoothConnector {

    private var socket: BluetoothSocket? = null

    var outputStream: OutputStream? = null
        private set

    var inputStream: InputStream? = null
        private set

    val isConnected: Boolean
        get() = socket?.isConnected == true

    /** 阻塞式连接，请放到 IO 线程里调用。 */
    fun connect(device: BluetoothDevice) {
        disconnect()
        val s = device.createRfcommSocketToServiceRecord(BluetoothConfig.APP_UUID)
        s.connect()
        socket = s
        outputStream = s.outputStream
        inputStream = s.inputStream
    }

    /** 发送一条协议消息。 */
    fun writeMessage(message: ProtocolMessage) {
        val out = outputStream ?: throw IOException("尚未连接")
        ProtocolCodec.writeMessage(out, message)
    }

    /** 阻塞读取一条协议消息，请放到 IO 线程里调用。 */
    fun readMessage(): ProtocolMessage {
        val input = inputStream ?: throw IOException("尚未连接")
        return ProtocolCodec.readMessage(input)
    }

    fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        outputStream = null
        inputStream = null
    }
}
