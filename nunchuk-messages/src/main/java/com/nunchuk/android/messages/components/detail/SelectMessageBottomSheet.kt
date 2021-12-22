package com.nunchuk.android.messages.components.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.messages.databinding.BottomSheetSelectMessageBinding

internal class EditPhotoUserBottomSheet : BaseBottomSheet<BottomSheetSelectMessageBinding>() {

    lateinit var listener: (SelectMessageOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSelectMessageBinding {
        return BottomSheetSelectMessageBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.btnSelect.setOnClickListener { onActionClicked(SelectMessageOption.Select) }
        binding.btnCopy.setOnClickListener { onActionClicked(SelectMessageOption.Copy) }
    }

    private fun onActionClicked(option: SelectMessageOption) {
        listener(option)
        dismiss()
    }

    companion object {
        private const val TAG = "WalletUpdateBottomSheet"

        fun show(fragmentManager: FragmentManager): EditPhotoUserBottomSheet {
            return EditPhotoUserBottomSheet().apply { show(fragmentManager, TAG) }
        }
    }
}

sealed class SelectMessageOption {
    object Select : SelectMessageOption()
    object Copy : SelectMessageOption()
}


