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

package com.nunchuk.android.settings.walletsecurity

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.ItemWalletSecuritySettingsBinding

internal class WalletSecuritySettingItemView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding =
        ItemWalletSecuritySettingsBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val ta =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.WalletSecuritySettingItemView,
                defStyleAttr,
                0
            )
        val title = ta.getString(R.styleable.WalletSecuritySettingItemView_wss_title)
        setTitle(title.orEmpty())
        val desc = ta.getString(R.styleable.WalletSecuritySettingItemView_wss_desc)
        setDesc(desc.orEmpty())
        ta.recycle()
    }

    fun setTitle(value: String) {
        binding.tvTitle.text = value
    }

    fun setDesc(value: String) {
        binding.tvDesc.text = value
    }

    fun setOptionChecked(checked: Boolean) {
        binding.switchButton.isChecked = checked
    }

    fun enableSwitchButton(enable: Boolean) {
        binding.switchButton.isEnabled = enable
    }

    fun setOptionChangeListener(onChanged: (Boolean) -> Unit) {
        binding.switchButton.setOnCheckedChangeListener { _, checked ->
            onChanged(checked)
        }
    }
}