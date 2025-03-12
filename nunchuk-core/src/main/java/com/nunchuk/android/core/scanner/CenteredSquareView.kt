package com.nunchuk.android.core.scanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CenteredSquareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint for the overlay outside the square
    private val overlayPaint: Paint = Paint().apply {
        color = Color.argb(128, 0, 0, 0) // #000000 with 50% opacity
        style = Paint.Style.FILL
    }

    // Paint for the white corner markers
    private val linePaint: Paint = Paint().apply {
        color = Color.WHITE // #FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 10f // 3 pixels wide
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Get view dimensions
        val width = width.toFloat()
        val height = height.toFloat()

        // Calculate square size (60% of width) and position it in the center
        val squareSide = width * 0.6f
        val left = (width - squareSide) / 2
        val top = (height - squareSide) / 2
        val right = left + squareSide
        val bottom = top + squareSide

        // Draw the overlay outside the square
        canvas.drawRect(0f, 0f, width, top, overlayPaint) // Top area
        canvas.drawRect(0f, bottom, width, height, overlayPaint) // Bottom area
        canvas.drawRect(0f, top, left, bottom, overlayPaint) // Left area
        canvas.drawRect(right, top, width, bottom, overlayPaint) // Right area

        // Calculate corner marker arm length (10% of square side)
        val cornerLength = squareSide * 0.1f

        // Draw the corner markers
        // Top-left corner: Opens downward and to the right
        canvas.drawLine(left, top, left + cornerLength, top, linePaint) // Horizontal
        canvas.drawLine(left, top, left, top + cornerLength, linePaint) // Vertical

        // Top-right corner: Opens downward and to the left
        canvas.drawLine(right - cornerLength, top, right, top, linePaint) // Horizontal
        canvas.drawLine(right, top, right, top + cornerLength, linePaint) // Vertical

        // Bottom-left corner: Opens upward and to the right
        canvas.drawLine(left, bottom, left + cornerLength, bottom, linePaint) // Horizontal
        canvas.drawLine(left, bottom - cornerLength, left, bottom, linePaint) // Vertical

        // Bottom-right corner: Opens upward and to the left
        canvas.drawLine(right - cornerLength, bottom, right, bottom, linePaint) // Horizontal
        canvas.drawLine(right, bottom - cornerLength, right, bottom, linePaint) // Vertical
    }
}