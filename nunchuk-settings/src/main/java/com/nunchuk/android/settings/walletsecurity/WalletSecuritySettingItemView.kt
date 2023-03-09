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
            context.obtainStyledAttributes(attrs, R.styleable.WalletSecuritySettingItemView, defStyleAttr, 0)
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

    fun setOptionChangeListener(onChanged: (Boolean) -> Unit) {
        binding.switchButton.setOnCheckedChangeListener { _, checked ->
            onChanged(checked)
        }
    }
}