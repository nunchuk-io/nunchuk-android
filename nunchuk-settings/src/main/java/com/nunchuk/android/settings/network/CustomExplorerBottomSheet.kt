package com.nunchuk.android.settings.network

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.settings.databinding.BottomSheetCustomExplorerBinding

class CustomExplorerBottomSheet : BaseBottomSheet<BottomSheetCustomExplorerBinding>() {
    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): BottomSheetCustomExplorerBinding {
        return BottomSheetCustomExplorerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.customExplorer.setOnClickListener {
            (activity as? OnCustomExplorerClickListener)?.onCustomExplorerClick()
            dismissAllowingStateLoss()
        }
    }

    companion object {
        private const val TAG = "CustomExplorerBottomSheet"
        fun show(fragmentManager: androidx.fragment.app.FragmentManager): CustomExplorerBottomSheet {
            return CustomExplorerBottomSheet().apply { show(fragmentManager, TAG) }
        }
    }
}

interface OnCustomExplorerClickListener {
    fun onCustomExplorerClick()
}