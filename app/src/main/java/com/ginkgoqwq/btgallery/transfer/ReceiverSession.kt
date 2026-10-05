package com.ginkgoqwq.btgallery.transfer

import android.util.Log
import com.ginkgoqwq.btgallery.bluetooth.BluetoothConfig
import com.ginkgoqwq.btgallery.data.MediaRepository
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

/**
 * 接收端的命令循环。
 *
 * 用 C++ 类比：这就是服务端的 while(true) { recv(); dispatch(); } 主循环。
 * 每收到一条协议消息，按 command 分发处理并回复。
 *
 * 支持的命令：
 * - HELLO          -> HELLO_ACK
 * - GET_FILE_LIST  -> FILE_LIST
 * - DELETE_FILE    -> DELETE_RESULT
 * - SEND_FILE      -> 开始接收（建 .part 文件）
 * - FILE_DATA      -> 追加数据
 * - FILE_FINISH    -> 校验 + 改名 + FILE_STATUS
 * - 未知命令        -> ERROR
 */
class ReceiverSession(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val repository: MediaRepository,
    private val onFilesChanged: () -> Unit = {},
    private val onProgress: (received: Long, total: Long) -> Unit = { _, _ -> },
    /** 仅在一个文件**完整落盘成功后**回调（与 onFilesChanged 不同，后者删除也会触发）。 */
    private val onFileReceived: () -> Unit = {}
) {

    /** 当前正在接收的文件状态；同一时刻只允许一个。 */
    private class Incoming(
        val meta: FileMeta,
        val safeName: String,
        val partFile: File,
        val output: FileOutputStream
    ) {
        val crc = CRC32()
        var received: Long = 0
    }

    private var incoming: Incoming? = null

    /** 阻塞式主循环，请放到 IO 线程调用；对端断开时返回。 */
    fun run() {
        try {
            while (true) {
                val message = try {
                    ProtocolCodec.readMessage(inputStream)
                } catch (e: Exception) {
                    Log.i(BluetoothConfig.TAG, "连接结束：${e.message}")
                    break
                }
                handle(message)
            }
        } finally {
            abortIncoming()
        }
    }

    private fun handle(message: ProtocolMessage) {
        when (message.command) {
            Protocol.Command.HELLO ->
                write(ProtocolMessage(Protocol.Command.HELLO_ACK, ByteArray(0)))

            Protocol.Command.GET_THUMBNAIL -> handleGetThumbnail(message.payload)

            Protocol.Command.GET_FILE_LIST -> handleGetFileList()

            Protocol.Command.DELETE_FILE -> handleDeleteFile(message.payload)

            Protocol.Command.SEND_FILE -> handleSendFile(message.payload)

            Protocol.Command.FILE_DATA -> handleFileData(message.payload)

            Protocol.Command.FILE_FINISH -> handleFileFinish(message.payload)

            else -> write(
                ProtocolMessage(
                    Protocol.Command.ERROR,
                    "未知命令：${message.command}".toByteArray()
                )
            )
        }
    }

    // ---------- 列表 / 删除 ----------

    private fun handleGetFileList() {
        val files = repository.listImages().map { image ->
            RemoteFileInfo(
                id = image.name,
                name = image.name,
                size = image.file.length(),
                mimeType = mimeTypeFor(image.name),
                createdAt = image.file.lastModified()
            )
        }
        write(ProtocolMessage(Protocol.Command.FILE_LIST, PayloadCodec.encodeFileList(files)))
    }

    private fun handleDeleteFile(payload: ByteArray) {
        val id = PayloadCodec.decodeDeleteFile(payload)
        val file = repository.fileForId(id)
        val result = if (file != null && repository.delete(file)) {
            onFilesChanged()
            DeleteResult(id, true)
        } else {
            DeleteResult(id, false, "文件不存在")
        }
        write(ProtocolMessage(Protocol.Command.DELETE_RESULT, PayloadCodec.encodeDeleteResult(result)))
    }

    // ---------- 缩略图 ----------

    /**
     * 收到 GET_THUMBNAIL 后现场生成缩略图并回复 THUMBNAIL。
     * 只传几十 KB 的缩略图，避免每次预览都传原图。
     */
    private fun handleGetThumbnail(payload: ByteArray) {
        val id = PayloadCodec.decodeThumbnailRequest(payload)
        val file = repository.fileForId(id)
        if (file == null) {
            Log.w(BluetoothConfig.TAG, "缩略图请求：找不到文件 id=$id")
        }
        val bytes = file?.let { repository.loadThumbnail(it) }
        if (file != null && bytes == null) {
            Log.w(BluetoothConfig.TAG, "缩略图请求：解码失败 id=$id")
        }
        Log.i(
            BluetoothConfig.TAG,
            "缩略图请求：id=$id -> ${bytes?.size ?: 0} 字节"
        )
        val data = ThumbnailData(id, bytes ?: ByteArray(0))
        write(ProtocolMessage(Protocol.Command.THUMBNAIL, PayloadCodec.encodeThumbnail(data)))
    }

    // ---------- 文件接收 ----------

    private fun handleSendFile(payload: ByteArray) {
        // 若上一次未收完，先清掉
        abortIncoming()

        val meta = PayloadCodec.decodeFileMeta(payload)
        if (meta.offset != 0L) {
            // 第一版不支持断点续传
            writeFileStatus(FileStatus(meta.id, false, 0, "暂不支持断点续传(offset=${meta.offset})"))
            return
        }
        val safeName = sanitizeName(meta.name)
        if (safeName == null) {
            writeFileStatus(FileStatus(meta.id, false, 0, "非法文件名"))
            return
        }

        val partFile = File(repository.saveDir, "$safeName.part")
        val output = FileOutputStream(partFile)
        incoming = Incoming(meta, safeName, partFile, output)
    }

    private fun handleFileData(payload: ByteArray) {
        val inc = incoming
        if (inc == null) {
            // 没有 SEND_FILE 直接来 FILE_DATA，忽略（对端异常时可能出现）
            return
        }
        inc.output.write(payload)
        inc.crc.update(payload)
        inc.received += payload.size
        onProgress(inc.received, inc.meta.size)
    }

    private fun handleFileFinish(payload: ByteArray) {
        val info = PayloadCodec.decodeFileFinish(payload)
        val inc = incoming
        if (inc == null) {
            writeFileStatus(FileStatus(info.id, false, 0, "没有正在接收的文件"))
            return
        }
        incoming = null
        inc.output.close()

        // 校验
        val sizeOk = inc.meta.size < 0 || inc.received == inc.meta.size
        val crcOk = info.crc32 == inc.crc.value
        if (!sizeOk || !crcOk) {
            inc.partFile.delete()
            val reason = if (!sizeOk) "大小不符 ${inc.received}/${inc.meta.size}" else "CRC 校验失败"
            writeFileStatus(FileStatus(inc.meta.id, false, inc.received, reason))
            return
        }

        // 改名成正式文件
        val finalFile = File(repository.saveDir, inc.safeName)
        if (finalFile.exists()) finalFile.delete()
        if (!inc.partFile.renameTo(finalFile)) {
            inc.partFile.delete()
            writeFileStatus(FileStatus(inc.meta.id, false, inc.received, "重命名失败"))
            return
        }

        onFileReceived()
        onFilesChanged()
        writeFileStatus(FileStatus(inc.meta.id, true, inc.received, ""))
    }

    private fun abortIncoming() {
        val inc = incoming ?: return
        incoming = null
        try { inc.output.close() } catch (_: Exception) {}
        try { inc.partFile.delete() } catch (_: Exception) {}
    }

    // ---------- 工具 ----------

    private fun writeFileStatus(status: FileStatus) {
        write(ProtocolMessage(Protocol.Command.FILE_STATUS, PayloadCodec.encodeFileStatus(status)))
    }

    private fun write(message: ProtocolMessage) {
        ProtocolCodec.writeMessage(outputStream, message)
    }

    /** 拒绝含路径分隔符或 ".." 的文件名，返回安全文件名或 null。 */
    private fun sanitizeName(name: String): String? {
        if (name.isBlank()) return null
        if (name.contains('/') || name.contains('\\')) return null
        if (name == "." || name == "..") return null
        if (File(name).name != name) return null
        return name
    }

    private fun mimeTypeFor(name: String): String =
        when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
}
