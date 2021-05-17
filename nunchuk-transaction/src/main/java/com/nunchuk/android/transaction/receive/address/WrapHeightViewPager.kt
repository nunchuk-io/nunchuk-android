package com.nunchuk.android.transaction.receive.address

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager

class WrapHeightViewPager : ViewPager {

    constructor(context: Context) : super(context)

    constructor(context: Context?, attrs: AttributeSet) : super(context!!, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightSpec = heightMeasureSpec
        super.onMeasure(widthMeasureSpec, heightSpec)
        var height = 0
        val childWidthSpec = MeasureSpec.makeMeasureSpec(
            0.coerceAtLeast(MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight),
            MeasureSpec.getMode(widthMeasureSpec)
        )
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            child.measure(childWidthSpec, MeasureSpec.UNSPECIFIED)
            val h: Int = child.measuredHeight
            if (h > height) height = h
        }
        if (height != 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}