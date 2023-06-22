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

package com.nunchuk.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
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
        var i = 0
        val count = adapter!!.count
        while (i < count) {
            val tab = newTab()
            addTab(tab.setText(adapter.getPageTitle(i)))
            val view = (slidingTabStrip.getChildAt(i) as ViewGroup).getChildAt(1) as AppCompatTextView
            view.typeface = fontStyle.getCurrentTypeface(FontInitializer.STYLE_SEMI_BOLD)
            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            i++
        }
    }
}