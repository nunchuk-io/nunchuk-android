package com.nunchuk.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.nunchuk.android.widget.util.FontInitializer

class NCFontTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {
    private val fontStyle = FontInitializer(context)

    override fun setupWithViewPager(viewPager: ViewPager?) {
        super.setupWithViewPager(viewPager)
        viewPager?.let { customizeTabs(it) }
    }

    private fun customizeTabs(viewPager: ViewPager) {
        removeAllTabs()
        val slidingTabStrip = getChildAt(0) as ViewGroup
        val adapter = viewPager.adapter
        val space = context.resources.getDimension(R.dimen.nc_padding_8).toInt()
        var i = 0
        val count = adapter!!.count
        while (i < count) {
            val tab = newTab()
            addTab(tab.setText(adapter.getPageTitle(i)))
            val view = (slidingTabStrip.getChildAt(i) as ViewGroup).getChildAt(1) as AppCompatTextView
            view.typeface = fontStyle.getCurrentTypeface(FontInitializer.STYLE_SEMI_BOLD)
            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            view.layoutParams = LinearLayout.LayoutParams(view.measuredWidth + space, LayoutParams.WRAP_CONTENT)
            i++
        }
    }
}