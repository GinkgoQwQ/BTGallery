package com.example.bluetoothimagetransfer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/** 验证本机 Coil 能否直接把 ByteArray 解码成图像。 */
@RunWith(AndroidJUnit4::class)
class CoilByteArrayTest {

    @Test
    fun coil_canLoadJpegByteArray() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        for (x in 0 until 400 step 10) {
            for (y in 0 until 300 step 10) {
                bmp.setPixel(x, y, Color.GREEN)
            }
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bmp.recycle()
        val bytes = out.toByteArray()

        val loader = ImageLoader.Builder(context).build()
        val request = ImageRequest.Builder(context).data(bytes).build()
        val result = loader.execute(request)
        assertTrue("Coil 应能把 ByteArray 解码为图像，实际结果：$result", result is SuccessResult)
    }
}
