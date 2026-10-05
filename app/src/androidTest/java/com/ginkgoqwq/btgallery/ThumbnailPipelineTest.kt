package com.ginkgoqwq.btgallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ginkgoqwq.btgallery.data.MediaRepository
import com.ginkgoqwq.btgallery.transfer.PayloadCodec
import com.ginkgoqwq.btgallery.transfer.Protocol
import com.ginkgoqwq.btgallery.transfer.ProtocolCodec
import com.ginkgoqwq.btgallery.transfer.ProtocolMessage
import com.ginkgoqwq.btgallery.transfer.ReceiverSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * 在真机上验证「接收端生成缩略图」这条链路，不依赖蓝牙。
 * 用管道流（PipedStream）模拟一条连接，直接驱动 ReceiverSession。
 */
@RunWith(AndroidJUnit4::class)
class ThumbnailPipelineTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo = MediaRepository(context)

    private fun createTestImage(name: String, w: Int = 1600, h: Int = 1200): File {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (x in 0 until w step 20) {
            for (y in 0 until h step 20) {
                bmp.setPixel(x, y, Color.RED)
            }
        }
        val f = File(repo.saveDir, name)
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()
        return f
    }

    @Test
    fun fileForId_findsSavedFile() {
        val f = createTestImage("t_forid.jpg")
        val found = repo.fileForId("t_forid.jpg")
        assertNotNull("fileForId 应该能找到刚保存的文件", found)
        f.delete()
    }

    @Test
    fun loadThumbnail_producesDecodableJpeg() {
        val f = createTestImage("t_thumb.jpg")
        val bytes = repo.loadThumbnail(f)
        assertNotNull("loadThumbnail 不应返回 null", bytes)
        assertTrue("缩略图不应为空", bytes!!.isNotEmpty())
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("缩略图应能被解码", decoded)
        assertTrue(
            "缩略图最长边应 <= 320，实际 ${decoded!!.width}x${decoded.height}",
            maxOf(decoded.width, decoded.height) <= 320
        )
        f.delete()
    }

    @Test
    fun receiverSession_repliesThumbnail() {
        val f = createTestImage("t_session.jpg")

        val toSession = PipedOutputStream()
        val sessionIn = PipedInputStream(toSession, 256 * 1024)
        val fromSession = PipedOutputStream()
        val testRead = PipedInputStream(fromSession, 256 * 1024)

        val session = ReceiverSession(
            inputStream = sessionIn,
            outputStream = fromSession,
            repository = repo
        )
        Thread { session.run() }.apply { isDaemon = true }.start()

        // 请求缩略图
        ProtocolCodec.writeMessage(
            toSession,
            ProtocolMessage(
                Protocol.Command.GET_THUMBNAIL,
                PayloadCodec.encodeThumbnailRequest("t_session.jpg")
            )
        )

        val reply = ProtocolCodec.readMessage(testRead)
        assertEquals(
            "接收端应回复 THUMBNAIL",
            Protocol.Command.THUMBNAIL,
            reply.command
        )
        val data = PayloadCodec.decodeThumbnail(reply.payload)
        assertEquals("t_session.jpg", data.id)
        assertTrue("缩略图字节应非空", data.bytes.isNotEmpty())

        runCatching { toSession.close() }
        f.delete()
    }
}
