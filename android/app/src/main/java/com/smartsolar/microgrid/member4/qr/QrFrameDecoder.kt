package com.smartsolar.microgrid.member4.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer

class QrFrameDecoder {
    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")

    fun decode(width: Int, height: Int, buffer: ByteBuffer, rowStride: Int, pixelStride: Int): String? {
        // Copy the camera's Y plane respecting device-specific row/pixel padding; no bitmap or stored photo is needed.
        val plane = buffer.duplicate()
        val origin = plane.position()
        val luminance = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) luminance[y * width + x] = plane.get(origin + y * rowStride + x * pixelStride)
        }
        val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (_: ReaderException) { null }
        finally { reader.reset() }
    }
}
