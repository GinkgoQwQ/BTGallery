package com.ginkgoqwq.btgallery.transfer

import android.content.Context
import android.net.Uri
import com.ginkgoqwq.btgallery.bluetooth.BluetoothConnector
import java.io.IOException
import java.util.zip.CRC32

/**
 * 发送端命令封装：在一条已连接的双向连接上做「请求 - 响应」。
 *
 * 用 C++ 类比：这是客户端的 sendRequest() + recvResponse() 组合。
 * 所有方法都是阻塞式的，必须在 IO 线程调用。
 */
class SenderSession(private val connector: BluetoothConnector) {

    /** 握手：发 HELLO，等待 HELLO_ACK。 */
    fun hello() {
        connector.writeMessage(ProtocolMessage(Protocol.Command.HELLO, ByteArray(0)))
        val reply = connector.readMessage()
        if (reply.command != Protocol.Command.HELLO_ACK) {
            throw ProtocolException("HELLO 应答错误：${reply.command}")
        }
    }

    /** 请求接收端图片列表。 */
    fun getFileList(): List<RemoteFileInfo> {
        connector.writeMessage(ProtocolMessage(Protocol.Command.GET_FILE_LIST, ByteArray(0)))
        val reply = connector.readMessage()
        if (reply.command != Protocol.Command.FILE_LIST) {
            throw ProtocolException("期望 FILE_LIST，实际：${reply.command}")
        }
        return PayloadCodec.decodeFileList(reply.payload)
    }

    /**
     * 请求接收端某张图片的缩略图。
     *
     * @return 缩略图 JPEG 字节；接收端无法生成时返回 null。
     */
    fun getThumbnail(id: String): ByteArray? {
        connector.writeMessage(
            ProtocolMessage(
                Protocol.Command.GET_THUMBNAIL,
                PayloadCodec.encodeThumbnailRequest(id)
            )
        )
        val reply = connector.readMessage()
        if (reply.command != Protocol.Command.THUMBNAIL) {
            throw ProtocolException("期望 THUMBNAIL，实际：${reply.command}")
        }
        val data = PayloadCodec.decodeThumbnail(reply.payload)
        return data.bytes.takeIf { it.isNotEmpty() }
    }

    /** 请求删除接收端某张图片。 */
    fun deleteFile(id: String): DeleteResult {
        connector.writeMessage(
            ProtocolMessage(Protocol.Command.DELETE_FILE, PayloadCodec.encodeDeleteFile(id))
        )
        val reply = connector.readMessage()
        if (reply.command != Protocol.Command.DELETE_RESULT) {
            throw ProtocolException("期望 DELETE_RESULT，实际：${reply.command}")
        }
        return PayloadCodec.decodeDeleteResult(reply.payload)
    }

    /**
     * 发送一张图片。
     *
     * 三段式流程：
     *   SEND_FILE（元信息） -> FILE_DATA × N（文件分块） -> FILE_FINISH（收尾 + CRC）
     * 最后等待接收端回 FILE_STATUS 确认。
     *
     * @param onProgress 实时进度回调 (已发送字节, 总字节)，总字节未知时为 -1。
     */
    fun sendFile(
        context: Context,
        uri: Uri,
        onProgress: (sent: Long, total: Long) -> Unit
    ) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = "image_${System.currentTimeMillis()}.${extensionFor(mime)}"
        val total = resolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L

        val meta = FileMeta(id = name, name = name, size = total, mimeType = mime, offset = 0)
        connector.writeMessage(
            ProtocolMessage(Protocol.Command.SEND_FILE, PayloadCodec.encodeFileMeta(meta))
        )

        val input = resolver.openInputStream(uri) ?: throw IOException("无法打开图片")
        val crc = CRC32()
        val buffer = ByteArray(CHUNK_SIZE)
        var sent = 0L
        input.use { inputStream ->
            while (true) {
                val read = inputStream.read(buffer)
                if (read <= 0) break
                crc.update(buffer, 0, read)
                connector.writeMessage(
                    ProtocolMessage(Protocol.Command.FILE_DATA, buffer.copyOf(read))
                )
                sent += read
                onProgress(sent, total)
            }
        }

        connector.writeMessage(
            ProtocolMessage(
                Protocol.Command.FILE_FINISH,
                PayloadCodec.encodeFileFinish(FileFinishInfo(name, sent, crc.value))
            )
        )

        val reply = connector.readMessage()
        if (reply.command != Protocol.Command.FILE_STATUS) {
            throw ProtocolException("期望 FILE_STATUS，实际：${reply.command}")
        }
        val status = PayloadCodec.decodeFileStatus(reply.payload)
        if (!status.success) {
            throw IOException("接收端保存失败：${status.message}")
        }
    }

    private fun extensionFor(mime: String): String = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg" -> "jpg"
        else -> "jpg"
    }

    companion object {
        /** 每个 FILE_DATA 帧携带的字节数（分块大小）。 */
        const val CHUNK_SIZE = 8 * 1024
    }
}
