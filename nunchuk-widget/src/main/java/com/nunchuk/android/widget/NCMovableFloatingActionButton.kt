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
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NCMovableFloatingActionButton : FloatingActionButton, OnTouchListener {

    private var downRawX = 0f
    private var downRawY = 0f

    private var dX = 0f
    private var dY = 0f

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        setOnTouchListener(this)
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val layoutParams = view.layoutParams as MarginLayoutParams
        return if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            downRawX = motionEvent.rawX
            downRawY = motionEvent.rawY
            dX = view.x - downRawX
            dY = view.y - downRawY
            true
        } else if (motionEvent.action == MotionEvent.ACTION_MOVE) {
            val viewParent = view.parent as View
            var newX = motionEvent.rawX + dX
            newX = max(layoutParams.leftMargin.toFloat(), newX)
            newX = min((viewParent.width - view.width - layoutParams.rightMargin).toFloat(), newX)
            var newY = motionEvent.rawY + dY
            newY = max(layoutParams.topMargin.toFloat(), newY)
            newY = min((viewParent.height - view.height - layoutParams.bottomMargin).toFloat(), newY)
            view.animate()
                .x(newX)
                .y(newY)
                .setDuration(0)
                .start()
            true
        } else if (motionEvent.action == MotionEvent.ACTION_UP) {
            val upDX = motionEvent.rawX - downRawX
            val upDY = motionEvent.rawY - downRawY
            if (abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE) {
                performClick()
            } else {
                true
            }
        } else {
            super.onTouchEvent(motionEvent)
        }
    }

    companion object {
        private const val CLICK_DRAG_TOLERANCE = 10f
    }
}