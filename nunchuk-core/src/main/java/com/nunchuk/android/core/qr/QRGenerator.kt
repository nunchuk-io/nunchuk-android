/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.EncodeHintType.ERROR_CORRECTION
import com.google.zxing.EncodeHintType.MARGIN
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

internal const val WIDTH = 400
internal const val DEFAULT_MARGIN = 2

fun String.convertToQRCode(width: Int = WIDTH, height: Int = WIDTH, margin: Int = DEFAULT_MARGIN): Bitmap? {
    // Sizes come from measured layouts, which can report 0 before the first real measure pass.
    if (width <= 0 || height <= 0) return null
    val hints = mapOf(ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.CHARACTER_SET to "UTF-8", MARGIN to margin)
    val matrix: BitMatrix = try {
        QRCodeWriter().encode(this, BarcodeFormat.QR_CODE, width, height, hints)
    } catch (e: Exception) {
        return null
    }
    // The writer enlarges the output when the requested size cannot hold the QR modules,
    // so render with the matrix dimensions instead of the requested ones.
    val matrixWidth = matrix.width
    val matrixHeight = matrix.height
    if (matrixWidth <= 0 || matrixHeight <= 0) return null
    val pixels = IntArray(matrixWidth * matrixHeight)
    for (y in 0 until matrixHeight) {
        val offset = y * matrixWidth
        for (x in 0 until matrixWidth) {
            pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return createBitmap(matrixWidth, matrixHeight, Bitmap.Config.RGB_565).apply {
        setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
    }
}

fun shareBitmap(context: Context, bitmap: Bitmap): String? {
    try {
        val fileName = "shared_image_${System.currentTimeMillis()}.png"
        val cachePath = File(context.externalCacheDir, "shared_images")
        if (!cachePath.exists()) cachePath.mkdirs()

        val file = File(cachePath, fileName)
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()

        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}