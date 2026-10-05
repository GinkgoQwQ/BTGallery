package com.example.bluetoothimagetransfer.transfer

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

/**
 * 自定义二进制协议。
 *
 * 用 C++ 类比：这就是一个固定头 + 变长体的「报文封装」，
 * 相当于你自己定义 struct Header + 一个字节缓冲区，然后手写序列化/反序列化。
 * 这里用 Kotlin 的 [DataInputStream] / [DataOutputStream] 处理大端序读写。
 *
 * 一帧的布局（全部大端序 / Big-Endian）：
 *
 * ┌──────────────┬────────────┬────────────┬──────────────┬────────────┬──────────────┐
 * │  Magic (4B)  │ Version(1) │ Command(1) │ Length (4B)  │  CRC (4B)  │ Payload(可变) │
 * ├──────────────┼────────────┼────────────┼──────────────┼────────────┼──────────────┤
 * │   "BTIM"     │   0x01     │ 见 Command │  payload 字节数│ payload 的 │  Length 字节  │
 * │              │            │            │   (不含 CRC)  │  CRC32 值  │              │
 * └──────────────┴────────────┴────────────┴──────────────┴────────────┴──────────────┘
 *
 * 说明：
 * - [Protocol.MAGIC]：用于快速判断“对端发来的到底是不是我们这套协议”，避免把乱数据当协议解析。
 * - [Protocol.VERSION]：将来改协议时用于兼容旧版本。
 * - [Protocol.Command]：一条消息的“类型”，类似 C++ 里的 enum。
 * - CRC32 只对 payload 计算：payload 完整且未损坏时，CRC 一定对得上。
 */
object Protocol {

    /** 帧头魔数，固定 4 字节 "BTIM"（Bluetooth Image Transfer）。 */
    val MAGIC: ByteArray = byteArrayOf(
        'B'.code.toByte(),
        'T'.code.toByte(),
        'I'.code.toByte(),
        'M'.code.toByte()
    )

    /** 当前协议版本。 */
    const val VERSION: Byte = 0x01

    /** 单帧 payload 上限，防止对端发一个伪造的超大 Length 把内存打爆。 */
    const val MAX_PAYLOAD = 64 * 1024 * 1024 // 64 MB

    /**
     * 命令类型。数值和你在需求里列的保持一致：
     * 0x01 HELLO、0x10 GET_FILE_LIST、0x11 FILE_LIST、0x40 DELETE_FILE、0x41 DELETE_RESULT ……
     */
    object Command {
        const val HELLO: Byte = 0x01
        const val HELLO_ACK: Byte = 0x02
        const val GET_DEVICE_INFO: Byte = 0x03
        const val DEVICE_INFO: Byte = 0x04

        const val GET_FILE_LIST: Byte = 0x10
        const val FILE_LIST: Byte = 0x11

        const val GET_THUMBNAIL: Byte = 0x20
        const val THUMBNAIL: Byte = 0x21
        const val GET_FILE: Byte = 0x28

        const val SEND_FILE: Byte = 0x30
        const val FILE_DATA: Byte = 0x31
        const val FILE_FINISH: Byte = 0x32

        const val DELETE_FILE: Byte = 0x40
        const val DELETE_RESULT: Byte = 0x41

        const val FILE_STATUS: Byte = 0x50

        const val ERROR: Byte = 0x7F
    }
}

/** 协议层面的错误：魔数不对、版本不支持、长度非法、CRC 校验失败等。 */
class ProtocolException(message: String) : IOException(message)

/** 一条协议消息：命令类型 + 原始 payload 字节。 */
data class ProtocolMessage(
    val command: Byte,
    val payload: ByteArray
)

/** 负责把 [ProtocolMessage] 写成字节流 / 从字节流读回来。 */
object ProtocolCodec {

    /** 把一条消息按协议帧格式写到 [out]。 */
    fun writeMessage(out: OutputStream, message: ProtocolMessage) {
        val dos = DataOutputStream(out)
        dos.write(Protocol.MAGIC)
        dos.writeByte(Protocol.VERSION.toInt())
        dos.writeByte(message.command.toInt())
        dos.writeInt(message.payload.size)
        dos.writeInt(crc32(message.payload))
        dos.write(message.payload)
        dos.flush()
    }

    /** 从 [input] 读出一条消息；数据不完整或损坏会抛异常。 */
    fun readMessage(input: InputStream): ProtocolMessage {
        val dis = DataInputStream(input)

        val magic = ByteArray(4)
        dis.readFully(magic)
        if (!magic.contentEquals(Protocol.MAGIC)) {
            throw ProtocolException("Magic 不匹配，连接已不同步")
        }

        val version = dis.readByte()
        if (version != Protocol.VERSION) {
            throw ProtocolException("不支持的协议版本：$version")
        }

        val command = dis.readByte()
        val length = dis.readInt()
        if (length < 0 || length > Protocol.MAX_PAYLOAD) {
            throw ProtocolException("非法的 payload 长度：$length")
        }

        val expectedCrc = dis.readInt()
        val payload = ByteArray(length)
        dis.readFully(payload)

        val actualCrc = crc32(payload)
        if (actualCrc != expectedCrc) {
            throw ProtocolException("CRC 校验失败")
        }

        return ProtocolMessage(command, payload)
    }

    /** 计算 payload 的 CRC32，返回 4 字节有符号 int（和 DataOutputStream.writeInt 对齐）。 */
    fun crc32(data: ByteArray): Int {
        val crc = CRC32()
        crc.update(data)
        return crc.value.toInt()
    }
}
