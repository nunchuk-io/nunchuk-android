package com.nunchuk.android.signer.details

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.DialogUpdateSignerBottomSheetBinding

class SignerUpdateBottomSheet : BottomSheetDialogFragment() {

    private lateinit var listener: (String) -> Unit

    private var _binding: DialogUpdateSignerBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val args: SignerUpdateBottomSheetArgs by lazy { SignerUpdateBottomSheetArgs.deserializeFrom(arguments) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogUpdateSignerBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener(::expandDialog)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val signerNameValue: AppCompatEditText = binding.signerNameValue
        signerNameValue.text?.append(args.signerName)
        binding.iconClose.setOnClickListener {
            onCloseClicked()
        }
        binding.btnSave.setOnClickListener {
            onSaveClicked()
        }
    }

    private fun onSaveClicked() {
        listener(binding.signerNameValue.text.toString())
        dismiss()
    }

    private fun onCloseClicked() {
        dismiss()
    }

    fun setListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    private fun expandDialog(dialog: DialogInterface) {
        val bottomSheetDialog = dialog as BottomSheetDialog
        val designBottomSheet: View? = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)
        designBottomSheet.run {
            this?.let {
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    companion object {
        private const val TAG = "UpdateSignerBottomSheet"

        private fun newInstance(signerName: String) = SignerUpdateBottomSheet().apply {
            arguments = SignerUpdateBottomSheetArgs(signerName).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, signerName: String): SignerUpdateBottomSheet {
            return newInstance(signerName).apply { show(fragmentManager, TAG) }
        }
    }
}

data class SignerUpdateBottomSheetArgs(val signerName: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_SIGNER_NAME, signerName)
    }

    companion object {
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"

        fun deserializeFrom(data: Bundle?) = SignerUpdateBottomSheetArgs(
            data?.getString(EXTRA_SIGNER_NAME).orEmpty()
        )
    }
}