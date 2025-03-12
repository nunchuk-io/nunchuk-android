package com.nunchuk.android.core.scanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R

class CenteredSquareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val overlayPaint: Paint = Paint().apply {
        color = Color.argb(128, 0, 0, 0) // #000000 with 50% opacity
        style = Paint.Style.FILL
    }

    private val squareSidePx: Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        250f,
        context.resources.displayMetrics
    )

    private val topLeftDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.nc_camera_top_left)
    private val topRightDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.nc_camera_top_right)
    private val bottomLeftDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.nc_camera_bottom_left)
    private val bottomRightDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.nc_camera_bottom_right)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val left = (width - squareSidePx) / 2
        val top = (height - squareSidePx) / 2
        val right = left + squareSidePx
        val bottom = top + squareSidePx

        canvas.drawRect(0f, 0f, width, top, overlayPaint)
        canvas.drawRect(0f, bottom, width, height, overlayPaint)
        canvas.drawRect(0f, top, left, bottom, overlayPaint)
        canvas.drawRect(right, top, width, bottom, overlayPaint)

        val cornerLength = squareSidePx * 0.2f

        topLeftDrawable?.setBounds(
            left.toInt(),
            top.toInt(),
            (left + cornerLength).toInt(),
            (top + cornerLength).toInt()
        )
        topLeftDrawable?.draw(canvas)

        topRightDrawable?.setBounds(
            (right - cornerLength).toInt(),
            top.toInt(),
            right.toInt(),
            (top + cornerLength).toInt()
        )
        topRightDrawable?.draw(canvas)

        bottomLeftDrawable?.setBounds(
            left.toInt(),
            (bottom - cornerLength).toInt(),
            (left + cornerLength).toInt(),
            bottom.toInt()
        )
        bottomLeftDrawable?.draw(canvas)

        bottomRightDrawable?.setBounds(
            (right - cornerLength).toInt(),
            (bottom - cornerLength).toInt(),
            right.toInt(),
            bottom.toInt()
        )
        bottomRightDrawable?.draw(canvas)
    }
}