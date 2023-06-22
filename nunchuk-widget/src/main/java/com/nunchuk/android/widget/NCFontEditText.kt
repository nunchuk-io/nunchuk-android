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

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import com.nunchuk.android.widget.util.FontInitializer

class NCFontEditText : AppCompatEditText {
    private var isMasked = true

    private val initializer: FontInitializer by lazy { FontInitializer(context) }

    constructor(context: Context) : super(context) {
        initializer.initTypeface(this)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializer.initTypeface(this, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializer.initTypeface(this, attrs)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun makeMaskedInput() {
        applyMasked()
        setOnTouchListener { _, event ->
            if(event.action == MotionEvent.ACTION_UP) {
                if(event.rawX >= (right - compoundDrawables[2].bounds.width())) {
                    isMasked = isMasked.not()
                    applyMasked()
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener  false
        }
    }

    private fun applyMasked() {
        if (isMasked) {
            setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_hide_pass,0)
            passwordEnabled()
        } else {
            setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_show_pass,0)
            passwordDisabled()
        }
    }

    private fun passwordEnabled() {
        inputType = inputType or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        transformationMethod = PasswordTransformationMethod.getInstance()
        setSelection(length())
    }

    private fun passwordDisabled() {
       transformationMethod = HideReturnsTransformationMethod.getInstance()
        setSelection(length())
    }
}