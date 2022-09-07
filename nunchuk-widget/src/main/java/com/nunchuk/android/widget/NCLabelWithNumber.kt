package com.nunchuk.android.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import com.nunchuk.android.widget.databinding.LayoutLabelWithNumberBinding

class NCLabelWithNumber @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val binding = LayoutLabelWithNumberBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val typedArray: TypedArray =
            context.obtainStyledAttributes(attrs, R.styleable.NCLabelWithNumber)
        val pos = typedArray.getInt(R.styleable.NCLabelWithNumber_pos, 0)
        binding.tvNumber.text = pos.toString()
        val textId =
            typedArray.getResourceId(R.styleable.NCLabelWithNumber_text, ResourcesCompat.ID_NULL)
        if (textId == ResourcesCompat.ID_NULL) {
            binding.tvContent.text = typedArray.getString(R.styleable.NCLabelWithNumber_text)
        } else {
            binding.tvContent.setText(textId)
        }
        val bold = typedArray.getBoolean(R.styleable.NCLabelWithNumber_bold, false)
        if (bold) {
            TextViewCompat.setTextAppearance(binding.tvNumber, R.style.NCText_Bold)
            TextViewCompat.setTextAppearance(binding.tvContent, R.style.NCText_Bold)
            binding.tvNumber.setBackgroundResource(R.drawable.nc_bg_black_stroke_circle_bold)
        }
        typedArray.recycle()
    }
}