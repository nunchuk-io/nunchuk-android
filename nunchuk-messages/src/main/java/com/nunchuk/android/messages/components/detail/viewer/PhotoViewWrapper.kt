/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.messages.components.detail.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class PhotoViewWrapper(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private var isParentInterceptionDisallowed = false

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        isParentInterceptionDisallowed = disallowIntercept
        if (disallowIntercept) {
            // PhotoView wants to disallow parent interception, let it be.
            parent.requestDisallowInterceptTouchEvent(true) // don't ban wrapper itself
        }
        else {
            // PhotoView wants to allow parent interception, we need to re-check it.
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // always false when up or cancel event,
        // which will allow parent interception normally.
        val isMultiTouch = ev.pointerCount > 1

        // re-check if it's multi touch
        parent.requestDisallowInterceptTouchEvent(
            isParentInterceptionDisallowed || isMultiTouch
        )
        return false
    }
}