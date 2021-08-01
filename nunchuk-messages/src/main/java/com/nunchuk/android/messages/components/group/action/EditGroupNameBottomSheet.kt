package com.nunchuk.android.messages.components.group.action

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.messages.databinding.DialogUpdateGroupNameBottomSheetBinding
import com.nunchuk.android.widget.util.addTextChangedCallback

class EditGroupNameBottomSheet : BaseBottomSheet<DialogUpdateGroupNameBottomSheetBinding>() {

    private lateinit var listener: (String) -> Unit

    private val args: EditGroupNameBottomSheetArgs by lazy { EditGroupNameBottomSheetArgs.deserializeFrom(arguments) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogUpdateGroupNameBottomSheetBinding {
        return DialogUpdateGroupNameBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.name.text?.append(args.signerName)
        binding.name.addTextChangedCallback {
            binding.btnSave.isVisible = it.isNotEmpty()
        }
        binding.iconClose.setOnClickListener {
            onCloseClicked()
        }
        binding.btnSave.setOnClickListener {
            onSaveClicked()
        }
    }

    private fun onSaveClicked() {
        val newName = binding.name.text.toString()
        if (newName != args.signerName) {
            listener(newName)
        }
        dismiss()
    }

    private fun onCloseClicked() {
        dismiss()
    }

    fun setListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "EditGroupNameBottomSheet"

        private fun newInstance(signerName: String) = EditGroupNameBottomSheet().apply {
            arguments = EditGroupNameBottomSheetArgs(signerName).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, signerName: String): EditGroupNameBottomSheet {
            return newInstance(signerName).apply { show(fragmentManager, TAG) }
        }
    }
}

data class EditGroupNameBottomSheetArgs(val signerName: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_GROUP_NAME, signerName)
    }

    companion object {
        private const val EXTRA_GROUP_NAME = "EXTRA_GROUP_NAME"

        fun deserializeFrom(data: Bundle?) = EditGroupNameBottomSheetArgs(
            data?.getString(EXTRA_GROUP_NAME).orEmpty()
        )
    }
}