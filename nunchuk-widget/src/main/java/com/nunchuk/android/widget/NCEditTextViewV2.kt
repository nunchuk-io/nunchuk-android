package com.nunchuk.android.widget

import android.content.Context
import android.graphics.Color
import android.text.*
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleObserver
import com.nunchuk.android.widget.databinding.NcEditTextViewBinding

class NCEditTextViewV2 : ConstraintLayout, LifecycleObserver {

    private var titleId: Int = DEFAULT_VALUE
    private var title: String? = null
    private var titleColor: Int = ContextCompat.getColor(context, R.color.nc_second_color)
    private var hintText: String? = null
    private var titleSize: Float = resources.getDimension(R.dimen.nc_text_size_14)
    private var hintTextColor: Int = ContextCompat.getColor(context, R.color.nc_third_color)
    private var hintTextId: Int = DEFAULT_VALUE
    private var editTextColor: Int = ContextCompat.getColor(context, R.color.nc_black_color)
    private var inputType: Int = TEXT_TYPE
    private var editTextSize: Float = resources.getDimension(R.dimen.nc_text_size_14)
    private var editTextBackground: Int = R.drawable.nc_edit_text_bg
    private var editHigh: Float = resources.getDimension(R.dimen.nc_height_44)
    private var editTheme: Int = NORMAL_THEME
    private var editGravity: Int = GRAVITY_CENTER_VERTICAL
    private var binding: NcEditTextViewBinding = NcEditTextViewBinding.inflate(LayoutInflater.from(context), this)

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

        titleColor = attr.getColor(R.styleable.NCEditTextView_edit_title_color, ContextCompat.getColor(context, R.color.nc_second_color))
        titleSize = attr.getDimension(R.styleable.NCEditTextView_edit_title_text_size, resources.getDimension(R.dimen.nc_text_size_14))
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
        val padding8 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_8)
        if (titleId == DEFAULT_VALUE) {
            if (title.isNullOrEmpty()) {
                binding.textView.visibility = View.GONE
            } else {
                binding.textView.visibility = View.VISIBLE
                (binding.editTextWrapper.layoutParams as LayoutParams).topMargin = padding8
                binding.textView.text = title
            }
        } else {
            binding.textView.visibility = View.VISIBLE
            (binding.editTextWrapper.layoutParams as LayoutParams).topMargin = padding8
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

        when (inputType) {
            TEXT_TYPE -> {
                binding.editText.inputType = EditorInfo.TYPE_CLASS_TEXT
                binding.editText.setSingleLine()
            }
            NUMBER_TYPE -> {
                binding.editText.inputType = EditorInfo.TYPE_CLASS_PHONE
                binding.editText.setSingleLine()
            }
            TEXT_MULTI_LINE_TYPE -> {
                binding.editText.inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                binding.editText.isSingleLine = false
            }
            TEXT_READ_ONLY_TYPE -> {
                binding.editText.inputType = InputType.TYPE_NULL
                binding.editText.setTextIsSelectable(false)
            }
        }

        val padding12 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_12)
        val padding10 = context.resources.getDimensionPixelOffset(R.dimen.nc_padding_10)

        if (editGravity == GRAVITY_TOP) {
            binding.editText.gravity = Gravity.TOP
            binding.editText.setPadding(padding12, padding10, padding12, padding10)
        } else {
            binding.editText.gravity = Gravity.CENTER_VERTICAL
            binding.editText.setPadding(padding12, 0, padding12, 0)
        }

        binding.editTextWrapper.setBackgroundResource(editTextBackground)
        binding.editTextWrapper.layoutParams.height = editHigh.toInt()
    }

    fun setTitle(title: String?) {
        if (title.isNullOrEmpty()) {
            binding.textView.visibility = View.GONE
        } else {
            binding.textView.visibility = View.VISIBLE
            binding.textView.text = title
        }
    }

    fun setMandatory() {
        val currentTitle = binding.textView.text.toString()
        val wordToSpan: Spannable = SpannableString("$currentTitle *")
        wordToSpan.setSpan(ForegroundColorSpan(Color.RED), currentTitle.length, currentTitle.length + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.textView.text = wordToSpan
    }

    fun setEditText(text: String?) {
        binding.editText.setText(text)
    }

    fun setHintText(hintText: String) {
        binding.editText.hint = hintText
    }

    fun setEditTextBackground(resId: Int) {
        binding.editText.setBackgroundResource(resId)
    }

    fun getEditText(): String = binding.editText.text.toString()

    fun getTitle(): String = binding.textView.text.toString()

    fun getEditTextView(): EditText = binding.editText

    fun setEditTextInputType(type: Int) {
        binding.editText.inputType = type
    }

    fun addTextChangedListener(listener: TextWatcher) {
        binding.editText.addTextChangedListener(listener)
    }

    fun removeTextChangedListener(listener: TextWatcher) {
        binding.editText.removeTextChangedListener(listener)
    }

    fun setFilters(filters: Array<InputFilter>) {
        binding.editText.filters = filters
    }

    fun setImeOptions(imeOptions: Int) {
        binding.editText.imeOptions = imeOptions
    }

    fun setOnEditorActionListener(listener: TextView.OnEditorActionListener) {
        binding.editText.setOnEditorActionListener(listener)
    }

    fun hideError() {
        binding.editTextWrapper.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_bg, null)
        binding.errorText.visibility = View.GONE
        binding.errorText.text = ""
    }

    fun setError(message: String) {
        binding.editTextWrapper.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_error_bg, null)
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }
}