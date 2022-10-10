package com.nunchuk.android.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.EncodeHintType.ERROR_CORRECTION
import com.google.zxing.EncodeHintType.MARGIN
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal const val WIDTH = 400
internal const val DEFAULT_MARGIN = 2

fun String.convertToQRCode(width: Int = WIDTH, height: Int = WIDTH, margin: Int = DEFAULT_MARGIN): Bitmap? {
    val hints = mapOf(ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.CHARACTER_SET to "UTF-8", MARGIN to margin)
    val matrix: BitMatrix = try {
        QRCodeWriter().encode(this, BarcodeFormat.QR_CODE, width, height, hints)
    } catch (e: Exception) {
        return null
    }
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    // val filePath = "${Environment.getExternalStorageDirectory()}/${System.currentTimeMillis()}.png"
    // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(filePath))
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}