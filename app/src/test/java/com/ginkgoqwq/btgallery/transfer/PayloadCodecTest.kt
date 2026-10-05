package com.ginkgoqwq.btgallery.transfer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Random

class PayloadCodecTest {

    @Test
    fun fileList_roundTrip_empty() {
        val encoded = PayloadCodec.encodeFileList(emptyList())
        assertEquals(emptyList<RemoteFileInfo>(), PayloadCodec.decodeFileList(encoded))
    }

    @Test
    fun fileList_roundTrip_multiple() {
        val files = listOf(
            RemoteFileInfo("id1", "a.jpg", 1024L, "image/jpeg", 1700000000000L),
            RemoteFileInfo("id2", "中文名.png", 2048L, "image/png", 1700000001000L)
        )
        val decoded = PayloadCodec.decodeFileList(PayloadCodec.encodeFileList(files))
        assertEquals(files, decoded)
    }

    @Test
    fun deleteFile_roundTrip() {
        val id = "some-file-id"
        assertEquals(id, PayloadCodec.decodeDeleteFile(PayloadCodec.encodeDeleteFile(id)))
    }

    @Test
    fun deleteResult_roundTrip_success() {
        val result = DeleteResult("id", true)
        assertEquals(result, PayloadCodec.decodeDeleteResult(PayloadCodec.encodeDeleteResult(result)))
    }

    @Test
    fun deleteResult_roundTrip_failure() {
        val result = DeleteResult("id", false, "文件不存在")
        assertEquals(result, PayloadCodec.decodeDeleteResult(PayloadCodec.encodeDeleteResult(result)))
    }

    @Test
    fun fileMeta_roundTrip() {
        val meta = FileMeta("img_1.jpg", "img_1.jpg", 12345L, "image/jpeg", 0L)
        assertEquals(meta, PayloadCodec.decodeFileMeta(PayloadCodec.encodeFileMeta(meta)))
    }

    @Test
    fun fileFinish_roundTrip() {
        val info = FileFinishInfo("img_1.jpg", 12345L, 0xDEADBEEFL)
        assertEquals(info, PayloadCodec.decodeFileFinish(PayloadCodec.encodeFileFinish(info)))
    }

    @Test
    fun fileStatus_roundTrip() {
        val status = FileStatus("img_1.jpg", true, 12345L, "ok")
        assertEquals(status, PayloadCodec.decodeFileStatus(PayloadCodec.encodeFileStatus(status)))
    }

    @Test
    fun thumbnailRequest_roundTrip() {
        val id = "img_1.jpg"
        assertEquals(id, PayloadCodec.decodeThumbnailRequest(PayloadCodec.encodeThumbnailRequest(id)))
    }

    @Test
    fun thumbnail_roundTrip() {
        val bytes = ByteArray(4096)
        Random(7).nextBytes(bytes)
        val decoded = PayloadCodec.decodeThumbnail(PayloadCodec.encodeThumbnail(ThumbnailData("id1", bytes)))
        assertEquals("id1", decoded.id)
        assertArrayEquals(bytes, decoded.bytes)
    }

    @Test
    fun thumbnail_emptyBytes_roundTrip() {
        val decoded = PayloadCodec.decodeThumbnail(
            PayloadCodec.encodeThumbnail(ThumbnailData("id2", ByteArray(0)))
        )
        assertEquals("id2", decoded.id)
        assertEquals(0, decoded.bytes.size)
    }
}
