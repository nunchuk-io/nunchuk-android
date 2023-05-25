package com.nunchuk.android.core.sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.databinding.FragmentSheetTooltipBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener

class BottomSheetTooltip : BaseBottomSheet<FragmentSheetTooltipBinding>() {

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSheetTooltipBinding {
        return FragmentSheetTooltipBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = requireArguments().getString(EXTRA_TITLE).orEmpty()
        val message = requireArguments().getString(EXTRA_MESSAGE).orEmpty()
        binding.title.text = title
        binding.title.isVisible = title.isNotEmpty()
        binding.message.text = message
        binding.ivClose.isVisible = requireArguments().getBoolean(EXTRA_SHOW_CLOSE_ICON, false)
        binding.ivClose.setOnDebounceClickListener {
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_MESSAGE = "extra_message"
        private const val EXTRA_SHOW_CLOSE_ICON = "show_close_icon"

        fun newInstance(
            title: String? = null,
            message: String,
            showClosedIcon: Boolean = false
        ): BottomSheetTooltip {
            return BottomSheetTooltip().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_TITLE, title)
                    putString(EXTRA_MESSAGE, message)
                    putBoolean(EXTRA_SHOW_CLOSE_ICON, showClosedIcon)
                }
            }
        }
    }
}