package com.ginkgoqwq.btgallery.transfer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * 命令 payload 的编解码。
 *
 * payload 本身又是一段结构化二进制数据：文本用「4 字节长度前缀 + UTF-8 字节」表示，
 * 整数全部大端序。这样就能把字符串、数字、列表打包成一段可校验的字节流。
 */

/** 单个字符串最大字节数，防止恶意超长字符串。 */
private const val MAX_STRING_BYTES = 1024 * 1024

/** 列表条目最大数量，防止伪造超大 count。 */
private const val MAX_ENTRIES = 100_000

/** 单个缩略图字节上限（约 2MB），防止伪造超大长度。 */
private const val MAX_THUMBNAIL_BYTES = 2 * 1024 * 1024

/** 接收端一张图片的元信息（发送端请求 FILE_LIST 时返回）。 */
data class RemoteFileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val createdAt: Long
)

/** DELETE_FILE 命令的执行结果。 */
data class DeleteResult(
    val id: String,
    val success: Boolean,
    val error: String = ""
)

/**
 * SEND_FILE 命令的文件描述。
 *
 * [offset] 为将来断点续传预留：0 表示从头发送，>0 表示从该字节位置续传。
 * [size] 为 -1 表示大小未知（接收端读到 FILE_FINISH 为止）。
 */
data class FileMeta(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val offset: Long
)

/** FILE_FINISH 命令携带的收尾信息。 */
data class FileFinishInfo(
    val id: String,
    val totalSize: Long,
    val crc32: Long
)

/** FILE_STATUS 命令携带的单文件传输结果。 */
data class FileStatus(
    val id: String,
    val success: Boolean,
    val receivedSize: Long,
    val message: String = ""
)

/**
 * THUMBNAIL 命令携带的缩略图数据。
 *
 * 用普通 class 而不是 data class：因为内部有 ByteArray，
 * data class 自动生成的 equals/hashCode 对数组只比较引用，容易误导。
 * [bytes] 为空表示接收端无法为该 id 生成缩略图。
 */
class ThumbnailData(val id: String, val bytes: ByteArray)

object PayloadCodec {

    // ---------- 字符串辅助 ----------

    private fun DataOutputStream.writeString(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String {
        val len = readInt()
        if (len < 0 || len > MAX_STRING_BYTES) {
            throw ProtocolException("非法字符串长度：$len")
        }
        val bytes = ByteArray(len)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    // ---------- FILE_LIST ----------

    fun encodeFileList(files: List<RemoteFileInfo>): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeInt(files.size)
        for (f in files) {
            dos.writeString(f.id)
            dos.writeString(f.name)
            dos.writeLong(f.size)
            dos.writeString(f.mimeType)
            dos.writeLong(f.createdAt)
        }
        dos.flush()
        return out.toByteArray()
    }

    fun decodeFileList(payload: ByteArray): List<RemoteFileInfo> {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        val count = dis.readInt()
        if (count < 0 || count > MAX_ENTRIES) {
            throw ProtocolException("非法文件数量：$count")
        }
        return List(count) {
            RemoteFileInfo(
                id = dis.readString(),
                name = dis.readString(),
                size = dis.readLong(),
                mimeType = dis.readString(),
                createdAt = dis.readLong()
            )
        }
    }

    // ---------- DELETE_FILE ----------

    fun encodeDeleteFile(id: String): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(id)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeDeleteFile(payload: ByteArray): String {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        return dis.readString()
    }

    // ---------- DELETE_RESULT ----------

    fun encodeDeleteResult(result: DeleteResult): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(result.id)
        dos.writeBoolean(result.success)
        dos.writeString(result.error)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeDeleteResult(payload: ByteArray): DeleteResult {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        val id = dis.readString()
        val success = dis.readBoolean()
        val error = dis.readString()
        return DeleteResult(id, success, error)
    }

    // ---------- SEND_FILE ----------

    fun encodeFileMeta(meta: FileMeta): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(meta.id)
        dos.writeString(meta.name)
        dos.writeLong(meta.size)
        dos.writeString(meta.mimeType)
        dos.writeLong(meta.offset)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeFileMeta(payload: ByteArray): FileMeta {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        return FileMeta(
            id = dis.readString(),
            name = dis.readString(),
            size = dis.readLong(),
            mimeType = dis.readString(),
            offset = dis.readLong()
        )
    }

    // ---------- FILE_FINISH ----------

    fun encodeFileFinish(info: FileFinishInfo): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(info.id)
        dos.writeLong(info.totalSize)
        dos.writeLong(info.crc32)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeFileFinish(payload: ByteArray): FileFinishInfo {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        return FileFinishInfo(
            id = dis.readString(),
            totalSize = dis.readLong(),
            crc32 = dis.readLong()
        )
    }

    // ---------- FILE_STATUS ----------

    fun encodeFileStatus(status: FileStatus): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(status.id)
        dos.writeBoolean(status.success)
        dos.writeLong(status.receivedSize)
        dos.writeString(status.message)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeFileStatus(payload: ByteArray): FileStatus {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        return FileStatus(
            id = dis.readString(),
            success = dis.readBoolean(),
            receivedSize = dis.readLong(),
            message = dis.readString()
        )
    }

    // ---------- GET_THUMBNAIL ----------

    fun encodeThumbnailRequest(id: String): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(id)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeThumbnailRequest(payload: ByteArray): String {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        return dis.readString()
    }

    // ---------- THUMBNAIL ----------

    fun encodeThumbnail(data: ThumbnailData): ByteArray {
        val out = ByteArrayOutputStream()
        val dos = DataOutputStream(out)
        dos.writeString(data.id)
        dos.writeInt(data.bytes.size)
        dos.write(data.bytes)
        dos.flush()
        return out.toByteArray()
    }

    fun decodeThumbnail(payload: ByteArray): ThumbnailData {
        val dis = DataInputStream(ByteArrayInputStream(payload))
        val id = dis.readString()
        val len = dis.readInt()
        if (len < 0 || len > MAX_THUMBNAIL_BYTES) {
            throw ProtocolException("非法缩略图长度：$len")
        }
        val bytes = ByteArray(len)
        dis.readFully(bytes)
        return ThumbnailData(id, bytes)
    }
}
