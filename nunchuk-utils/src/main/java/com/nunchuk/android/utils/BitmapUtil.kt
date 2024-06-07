package com.nunchuk.android.utils

import android.graphics.Bitmap
import android.graphics.Canvas

object BitmapUtil {
    fun combineBitmapsVertically(bitmaps: List<Bitmap>): Bitmap {
        require(bitmaps.isNotEmpty()) {
            "Bitmap list must not be empty"
        }
        val newBitmaps = bitmaps.map { it.copy(Bitmap.Config.ARGB_8888, false) }
        val width = newBitmaps[0].width
        newBitmaps.forEach { bitmap ->
            require(bitmap.width == width) {
                "All bitmaps must have the same width"
            }
        }

        val totalHeight = newBitmaps.sumOf { it.height }
        val resultBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        var currentHeight = 0f
        for (bitmap in newBitmaps) {
            canvas.drawBitmap(bitmap, 0f, currentHeight, null)
            currentHeight += bitmap.height
        }

        return resultBitmap
    }
}