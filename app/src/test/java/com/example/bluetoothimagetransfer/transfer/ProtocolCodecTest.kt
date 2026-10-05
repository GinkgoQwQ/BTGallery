package com.example.bluetoothimagetransfer.transfer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Random

class ProtocolCodecTest {

    /** 辅助：把消息写进内存，再从内存读回来。 */
    private fun roundTrip(message: ProtocolMessage): ProtocolMessage {
        val out = ByteArrayOutputStream()
        ProtocolCodec.writeMessage(out, message)
        return ProtocolCodec.readMessage(ByteArrayInputStream(out.toByteArray()))
    }

    @Test
    fun roundTrip_emptyPayload() {
        val msg = ProtocolMessage(Protocol.Command.HELLO, ByteArray(0))
        val read = roundTrip(msg)
        assertEquals(msg.command, read.command)
        assertArrayEquals(msg.payload, read.payload)
    }

    @Test
    fun roundTrip_smallPayload() {
        val msg = ProtocolMessage(Protocol.Command.FILE_LIST, "hello".toByteArray())
        val read = roundTrip(msg)
        assertEquals(msg.command, read.command)
        assertArrayEquals(msg.payload, read.payload)
    }

    @Test
    fun roundTrip_largePayload() {
        // 1MB 随机数据，验证长度和 CRC 在大 payload 下依然正确
        val payload = ByteArray(1024 * 1024)
        Random(42).nextBytes(payload)
        val msg = ProtocolMessage(Protocol.Command.FILE_DATA, payload)
        val read = roundTrip(msg)
        assertEquals(msg.command, read.command)
        assertArrayEquals(msg.payload, read.payload)
    }

    @Test
    fun corruptedPayload_throws() {
        val msg = ProtocolMessage(Protocol.Command.HELLO, "abc".toByteArray())
        val out = ByteArrayOutputStream()
        ProtocolCodec.writeMessage(out, msg)
        val bytes = out.toByteArray()

        // 翻转 payload 最后一个字节，CRC 必然对不上
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0xFF).toByte()

        assertThrows(ProtocolException::class.java) {
            ProtocolCodec.readMessage(ByteArrayInputStream(bytes))
        }
    }

    @Test
    fun wrongMagic_throws() {
        val out = ByteArrayOutputStream()
        ProtocolCodec.writeMessage(out, ProtocolMessage(Protocol.Command.HELLO, ByteArray(0)))
        val bytes = out.toByteArray()

        // 破坏第一个魔数字节 'B' -> 'X'
        bytes[0] = 'X'.code.toByte()

        assertThrows(ProtocolException::class.java) {
            ProtocolCodec.readMessage(ByteArrayInputStream(bytes))
        }
    }

    @Test
    fun truncatedStream_throws() {
        val out = ByteArrayOutputStream()
        ProtocolCodec.writeMessage(
            out,
            ProtocolMessage(Protocol.Command.HELLO, "12345678".toByteArray())
        )
        val bytes = out.toByteArray()
        val truncated = bytes.copyOf(bytes.size - 3)

        // 数据被截断，readFully 会抛 EOFException（属于 IOException）
        assertThrows(IOException::class.java) {
            ProtocolCodec.readMessage(ByteArrayInputStream(truncated))
        }
    }
}
