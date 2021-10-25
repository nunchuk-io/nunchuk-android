package com.nunchuk.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.settings.databinding.BottomSheetEditNameBinding

class EditNameUserBottomSheet : BaseBottomSheet<BottomSheetEditNameBinding>() {

    var listener: (EditNameUserOption) -> Unit = {}

    private val userName: String
        get() = arguments?.getString(ARG_NAME).orEmpty()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetEditNameBinding {
        return BottomSheetEditNameBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
    }

    private fun setupViews() {
        binding.edtName.setText(userName)
        binding.closeBtn.setOnClickListener {
            cleanUp()
        }
        binding.saveBtn.setOnClickListener {
            save()
        }
    }

    private fun cleanUp() {
        binding.edtName.text?.clear()
        dismiss()
    }

    private fun save() {
        listener(EditNameUserOption.Save(binding.edtName.text.toString().trim()))
        dismiss()
    }

    companion object {
        private const val TAG = "EditNameUserBottomSheet"
        const val ARG_NAME = "ARG_NAME"
        fun show(name: String, fragmentManager: FragmentManager) = EditNameUserBottomSheet().apply {
            arguments = bundleOf(
                ARG_NAME to name
            )
            show(fragmentManager, TAG)
        }
    }
}

sealed class EditNameUserOption {
    data class Save(val name: String) : EditNameUserOption()
}