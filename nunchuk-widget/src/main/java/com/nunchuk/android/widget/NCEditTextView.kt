package com.nunchuk.android.widget

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleObserver
import kotlinx.android.synthetic.main.nc_edit_text_view.view.*

const val DEFAULT_VALUE = 0
const val NORMAL_THEME: Int = 0
const val TEXT_NO_SUGGESTION: Int = 4
const val TEXT_READ_ONLY_TYPE: Int = 3
const val TEXT_MULTI_LINE_TYPE: Int = 2
const val NUMBER_TYPE: Int = 1
const val TEXT_TYPE: Int = 0
const val GRAVITY_CENTER_VERTICAL = 10
const val GRAVITY_TOP = 11

class NCEditTextView : ConstraintLayout, LifecycleObserver {

    private var titleId: Int = DEFAULT_VALUE
    private var title: String? = null
    private var titleColor: Int = ContextCompat.getColor(context, R.color.nc_primary_color)
    private var hintText: String? = null
    private var titleSize: Float = resources.getDimension(R.dimen.nc_text_size_12)
    private var hintTextColor: Int = ContextCompat.getColor(context, R.color.nc_third_color)
    private var hintTextId: Int = DEFAULT_VALUE
    private var editTextColor: Int = ContextCompat.getColor(context, R.color.nc_black_color)
    private var inputType: Int = TEXT_TYPE
    private var editTextSize: Float = resources.getDimension(R.dimen.nc_text_size_14)
    private var editTextBackground: Int = R.drawable.nc_edit_text_bg
    private var editHigh: Float = resources.getDimension(R.dimen.nc_height_48)
    private var editTheme: Int = NORMAL_THEME
    private var editGravity: Int = GRAVITY_CENTER_VERTICAL

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr) {
        retrieveAttributes(context, attributeSet)
        initView(context)
    }

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
        editTextSize = attr.getDimension(R.styleable.NCEditTextView_edit_text_size, resources.getDimension(R.dimen.nc_text_size_14))
        editTextBackground = attr.getResourceId(R.styleable.NCEditTextView_edit_background, R.drawable.nc_edit_text_bg)
        editHigh = attr.getDimension(R.styleable.NCEditTextView_edit_height, resources.getDimension(R.dimen.nc_height_44))
        editTheme = attr.getInteger(R.styleable.NCEditTextView_edit_theme, NORMAL_THEME)
        editGravity = attr.getInteger(R.styleable.NCEditTextView_edit_gravity, GRAVITY_CENTER_VERTICAL)
        attr.recycle()
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.nc_edit_text_view, this)
        onBindView()
    }

    private fun onBindView() {
        if (titleId == DEFAULT_VALUE) {
            if (title.isNullOrEmpty()) {
                textView.visibility = View.GONE
            } else {
                textView.visibility = View.VISIBLE
                textView.text = title
            }
        } else {
            textView.visibility = View.VISIBLE
            textView.setText(titleId)
        }

        if (hintTextId == DEFAULT_VALUE) {
            editText.hint = hintText
        } else {
            editText.setHint(hintTextId)
        }

        textView.setTextColor(titleColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
        editText.setHintTextColor(hintTextColor)
        editText.setTextColor(editTextColor)
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextSize)

        when (inputType) {
            TEXT_TYPE -> {
                editText.inputType = EditorInfo.TYPE_CLASS_TEXT
                editText.setSingleLine()
            }
            NUMBER_TYPE -> {
                editText.inputType = EditorInfo.TYPE_CLASS_PHONE
                editText.setSingleLine()
            }
            TEXT_MULTI_LINE_TYPE -> {
                editText.inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                editText.isSingleLine = false
            }
            TEXT_READ_ONLY_TYPE -> {
                editText.inputType = InputType.TYPE_NULL
                editText.setTextIsSelectable(false)
            }
            TEXT_NO_SUGGESTION -> {
                editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
        }

        val padding12 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_12)
        val padding10 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_10)

        if (editGravity == GRAVITY_TOP) {
            editText.gravity = Gravity.TOP
            editText.setPadding(padding12, padding10, padding12, padding10)
        } else {
            editText.gravity = Gravity.CENTER_VERTICAL
            editText.setPadding(padding12, 0, padding12, 0)
        }

        editText.setBackgroundResource(editTextBackground)
        editText.layoutParams.height = editHigh.toInt()
    }

    fun getEditText(): String = editText.text.toString()

    fun getEditTextView(): EditText = editText

    fun hideError() {
        editText.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_bg, null)
        errorText.visibility = View.GONE
        errorText.text = ""
    }

    fun setError(message: String) {
        editText.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_error_bg, null)
        errorText.visibility = View.VISIBLE
        errorText.text = message
    }
}