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
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleObserver
import com.nunchuk.android.widget.databinding.NcEditTextViewBinding

const val DEFAULT_VALUE = 0
const val NORMAL_THEME: Int = 0
const val TEXT_NO_SUGGESTION: Int = 4
const val TEXT_READ_ONLY_TYPE: Int = 3
const val TEXT_MULTI_LINE_TYPE: Int = 2
const val NUMBER_TYPE: Int = 1
const val TEXT_TYPE: Int = 0
const val GRAVITY_CENTER_VERTICAL = 10
const val GRAVITY_TOP = 11

class NCEditTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), LifecycleObserver {

    private var titleId: Int = DEFAULT_VALUE
    private var title: String? = null
    private var titleColor: Int = ContextCompat.getColor(context, R.color.nc_primary_color)
    private var hintText: String? = null
    private var titleSize: Float = resources.getDimension(R.dimen.nc_text_size_12)
    private var hintTextColor: Int = ContextCompat.getColor(context, R.color.nc_third_color)
    private var hintTextId: Int = DEFAULT_VALUE
    private var editTextColor: Int = ContextCompat.getColor(context, R.color.nc_black_color)
    private var inputType: Int = TEXT_TYPE
    private var editTextSize: Float = resources.getDimension(R.dimen.nc_text_size_16)
    private var editTextBackground: Int = R.drawable.nc_edit_text_bg
    private var editHigh: Float = resources.getDimension(R.dimen.nc_height_44)
    private var editTheme: Int = NORMAL_THEME
    private var editGravity: Int = GRAVITY_CENTER_VERTICAL
    private var editShowBorder: Boolean = true
    private val binding = NcEditTextViewBinding.inflate(LayoutInflater.from(context), this)

    init {
        retrieveAttributes(context, attrs)
        onBindView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun retrieveAttributes(context: Context, attributeSet: AttributeSet?) {
        val attr = context.obtainStyledAttributes(attributeSet, R.styleable.NCEditTextView, 0, 0)
        titleId = attr.getResourceId(R.styleable.NCEditTextView_edit_title, DEFAULT_VALUE)
        if (titleId == DEFAULT_VALUE) {
            title = attr.getString(R.styleable.NCEditTextView_edit_title)
        }

        hintTextId = attr.getResourceId(R.styleable.NCEditTextView_edit_hint, DEFAULT_VALUE)
        if (hintTextId == DEFAULT_VALUE) {
            hintText = attr.getString(R.styleable.NCEditTextView_edit_hint)
        }

        titleColor = attr.getColor(R.styleable.NCEditTextView_edit_title_color, ContextCompat.getColor(context, R.color.nc_primary_color))
        titleSize = attr.getDimension(R.styleable.NCEditTextView_edit_title_text_size, resources.getDimension(R.dimen.nc_text_size_12))
        hintTextColor = attr.getColor(R.styleable.NCEditTextView_edit_hint_color, ContextCompat.getColor(context, R.color.nc_third_color))
        editTextColor = attr.getColor(R.styleable.NCEditTextView_edit_text_color, ContextCompat.getColor(context, R.color.nc_black_color))
        inputType = attr.getInteger(R.styleable.NCEditTextView_edit_input_type, TEXT_TYPE)
        editTextSize = attr.getDimension(R.styleable.NCEditTextView_edit_text_size, resources.getDimension(R.dimen.nc_text_size_16))
        editTextBackground = attr.getResourceId(R.styleable.NCEditTextView_edit_background, R.drawable.nc_edit_text_bg)
        editHigh = attr.getDimension(R.styleable.NCEditTextView_edit_height, resources.getDimension(R.dimen.nc_height_44))
        editTheme = attr.getInteger(R.styleable.NCEditTextView_edit_theme, NORMAL_THEME)
        editGravity = attr.getInteger(R.styleable.NCEditTextView_edit_gravity, GRAVITY_CENTER_VERTICAL)
        editShowBorder = attr.getBoolean(R.styleable.NCEditTextView_edit_show_border, true)
        attr.recycle()
    }

    private fun onBindView() {
        if (titleId == DEFAULT_VALUE) {
            if (title.isNullOrEmpty()) {
                binding.textView.visibility = View.GONE
            } else {
                binding.textView.visibility = View.VISIBLE
                binding.textView.text = title
            }
        } else {
            binding.textView.visibility = View.VISIBLE
            binding.textView.setText(titleId)
        }

        if (hintTextId == DEFAULT_VALUE) {
            binding.editText.hint = hintText
        } else {
            binding.editText.setHint(hintTextId)
        }

        binding.textView.setTextColor(titleColor)
        binding.textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
        binding.editText.setHintTextColor(hintTextColor)
        binding.editText.setTextColor(editTextColor)
        binding.editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextSize)

        setInputType(inputType)

        val padding12 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_12)
        val padding10 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_10)

        if (editGravity == GRAVITY_TOP) {
            binding.editText.gravity = Gravity.TOP
            binding.editText.setPadding(padding12, padding10, padding12, padding10)
        } else {
            binding.editText.gravity = Gravity.CENTER_VERTICAL
            binding.editText.setPadding(padding12, 0, padding12, 0)
        }

        if (!editShowBorder) {
            binding.editText.setBackgroundResource(0)
            binding.editText.setPadding(0, 0, 0, 0)
        } else {
            binding.editText.setBackgroundResource(editTextBackground)
        }
        binding.editText.layoutParams.height = editHigh.toInt()
    }

    fun setInputType(inputType: Int) {
        when (inputType) {
            TEXT_TYPE -> {
                binding.editText.inputType = InputType.TYPE_CLASS_TEXT
                binding.editText.setSingleLine()
            }
            NUMBER_TYPE -> {
                binding.editText.inputType = InputType.TYPE_CLASS_NUMBER
                binding.editText.setSingleLine()
            }
            TEXT_MULTI_LINE_TYPE -> {
                binding.editText.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                binding.editText.isSingleLine = false
            }
            TEXT_READ_ONLY_TYPE -> {
                binding.editText.inputType = InputType.TYPE_NULL
                binding.editText.setTextIsSelectable(false)
            }
            TEXT_NO_SUGGESTION -> {
                binding.editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.editText.isEnabled = enabled
        if (enabled) {
            binding.editText.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_bg, null)
        } else {
            binding.editText.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_bg_disabled, null)
        }
    }

    fun getTextView(): TextView = binding.textView

    fun getEditText(): String = binding.editText.text.toString()
    fun getEditTextView(): EditText = binding.editText

    fun hideError() {
        binding.editText.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_bg, null)
        binding.errorText.visibility = View.GONE
        binding.errorText.text = ""
    }

    fun setError(message: String) {
        binding.editText.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_error_bg, null)
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    fun makeMaskedInput() {
        binding.editText.makeMaskedInput()
    }
}