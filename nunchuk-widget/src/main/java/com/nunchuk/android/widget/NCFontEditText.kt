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
        inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        transformationMethod = PasswordTransformationMethod.getInstance()
        setSelection(length())
    }

    private fun passwordDisabled() {
       transformationMethod = HideReturnsTransformationMethod.getInstance()
        setSelection(length())
    }
}