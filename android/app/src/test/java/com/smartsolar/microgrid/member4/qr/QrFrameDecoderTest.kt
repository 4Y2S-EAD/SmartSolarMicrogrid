package com.smartsolar.microgrid.member4.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class QrFrameDecoderTest {
    @Test fun decodesPaddedCameraLuminanceInEveryOrientation() {
        // Test camera row/pixel strides and QR orientation, not a production fake-scanning path.
        val payload = "{\"data\":{\"reference\":\"decoder-test-only\"}}"
        val size = 320
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        for (rotation in 0..3) for (pixelStride in 1..2) {
            val rowStride = size * pixelStride + 32
            val bytes = ByteArray(rowStride * size + 8) { 0xff.toByte() }
            for (y in 0 until size) for (x in 0 until size) {
                val (mx, my) = when (rotation) {
                    1 -> y to size - x - 1
                    2 -> size - x - 1 to size - y - 1
                    3 -> size - y - 1 to x
                    else -> x to y
                }
                bytes[8 + y * rowStride + x * pixelStride] = if (matrix[mx, my]) 0 else 0xff.toByte()
            }
            val buffer = ByteBuffer.wrap(bytes).apply { position(8) }
            assertEquals(payload, QrFrameDecoder().decode(size, size, buffer, rowStride, pixelStride))
            assertEquals(8, buffer.position())
        }
    }

    @Test fun unreadableFramesDoNotPreventTheNextDecode() {
        val decoder = QrFrameDecoder()
        assertNull(decoder.decode(160, 160, ByteBuffer.wrap(ByteArray(160 * 160)), 160, 1))
        val matrix = QRCodeWriter().encode("next-frame", BarcodeFormat.QR_CODE, 160, 160)
        val pixels = ByteArray(160 * 160) { i -> if (matrix[i % 160, i / 160]) 0 else 0xff.toByte() }
        assertEquals("next-frame", decoder.decode(160, 160, ByteBuffer.wrap(pixels), 160, 1))
    }
}
