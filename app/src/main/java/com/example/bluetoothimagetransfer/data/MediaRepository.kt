package com.example.bluetoothimagetransfer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File

class MediaRepository(context: Context) {

    /** 接收图片的目录：外部私有目录下 received/ */
    val saveDir: File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "received"
    ).apply { mkdirs() }

    /** 列出本地所有图片，按最后修改时间倒序 */
    fun listImages(): List<ImageItem> {
        return saveDir
            .listFiles { file -> file.extension.lowercase() in IMAGE_EXTS }
            ?.sortedByDescending { it.lastModified() }
            ?.map { ImageItem(it, it.name) }
            ?: emptyList()
    }

    fun delete(file: File): Boolean = file.delete()

    /** 按 id 找文件。第一版 id 就是文件名，但做了目录穿越防护。 */
    fun fileForId(id: String): File? {
        return try {
            val dir = saveDir.canonicalFile
            val target = File(dir, id).canonicalFile
            if (target.parentFile == dir && target.exists()) target else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 生成图片缩略图的 JPEG 字节（用于通过蓝牙传给发送端预览）。
     *
     * 用 C++ 类比：先「探测尺寸」再按 2 的幂次降采样解码（inSampleSize），
     * 避免把整张原图解码进内存导致 OOM，最后缩放到 [maxSize] 以内并压成 JPEG。
     *
     * @return 成功返回 JPEG 字节；失败或无图返回 null。
     */
    fun loadThumbnail(file: File, maxSize: Int = 320): ByteArray? {
        return try {
            // 第一步：只解码边界，不分配像素内存
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // 第二步：计算 2 的幂次采样率，使解码结果不小于 maxSize
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSize &&
                bounds.outHeight / (sample * 2) >= maxSize
            ) {
                sample *= 2
            }

            val decoded = BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null

            // 第三步：精确缩放到 maxSize 以内
            val longest = maxOf(decoded.width, decoded.height)
            val scaled = if (longest > maxSize) {
                val ratio = maxSize.toFloat() / longest
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    companion object {
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp")
    }
}
