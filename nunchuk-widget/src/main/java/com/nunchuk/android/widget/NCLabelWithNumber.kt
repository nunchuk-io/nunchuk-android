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