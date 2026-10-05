package com.ginkgoqwq.btgallery.transfer

import android.content.Context
import android.net.Uri
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileTransfer {

    /** 发送图片：先发头部，再发内容 */
    fun sendImage(
        context: Context,
        uri: Uri,
        outputStream: OutputStream,
        onProgress: (sent: Long, total: Long) -> Unit
    ) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("无法打开图片")

        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val fileNameBytes = fileName.toByteArray(Charsets.UTF_8)

        // 从 contentResolver 拿准确的文件大小
        val totalSize = context.contentResolver
            .openFileDescriptor(uri, "r")
            ?.use { it.statSize }
            ?: -1L

        val dataOutputStream = DataOutputStream(outputStream)
        dataOutputStream.writeInt(fileNameBytes.size)
        dataOutputStream.write(fileNameBytes)
        dataOutputStream.writeLong(totalSize)
        dataOutputStream.flush()

        val buffer = ByteArray(8192)
        var sent = 0L
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
            sent += read
            onProgress(sent, totalSize)
        }
        outputStream.flush()
        inputStream.close()
    }

    /** 接收文件：先存 .part，接收完整再重命名为正式文件 */
    fun receiveFile(
        inputStream: InputStream,
        saveDir: File,
        onFileReceived: (String) -> Unit
    ) {
        val dataInputStream = DataInputStream(inputStream)
        val fileNameLen = dataInputStream.readInt()
        val fileNameBytes = ByteArray(fileNameLen)
        dataInputStream.readFully(fileNameBytes)
        val fileName = String(fileNameBytes, Charsets.UTF_8)
        val fileSize = dataInputStream.readLong()

        val partFile = File(saveDir, "$fileName.part")
        val finalFile = File(saveDir, fileName)
        val outputStream = FileOutputStream(partFile)

        val buffer = ByteArray(8192)
        var received = 0L
        while (received < fileSize) {
            val toRead = minOf(buffer.size.toLong(), fileSize - received).toInt()
            val read = dataInputStream.read(buffer, 0, toRead)
            if (read == -1) break
            outputStream.write(buffer, 0, read)
            received += read
        }
        outputStream.close()

        if (received == fileSize) {
            partFile.renameTo(finalFile)
            onFileReceived(fileName)
        } else {
            partFile.delete()
            throw IOException("文件接收不完整：$received / $fileSize")
        }
    }
}