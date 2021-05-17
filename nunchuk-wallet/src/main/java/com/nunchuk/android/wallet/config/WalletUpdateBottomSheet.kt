package com.nunchuk.android.wallet.config

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
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.DialogUpdateWalletBottomSheetBinding

class WalletUpdateBottomSheet : BottomSheetDialogFragment() {

    private lateinit var listener: (String) -> Unit

    private var _binding: DialogUpdateWalletBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val args: WalletUpdateBottomSheetArgs by lazy { WalletUpdateBottomSheetArgs.deserializeFrom(arguments) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogUpdateWalletBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener(::expandDialog)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val value: AppCompatEditText = binding.value
        value.text?.append(args.walletName)
        binding.iconClose.setOnClickListener {
            onCloseClicked()
        }
        binding.btnSave.setOnClickListener {
            onSaveClicked()
        }
    }

    private fun onSaveClicked() {
        listener(binding.value.text.toString())
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
            this?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }
    }

    companion object {
        private const val TAG = "UpdateSignerBottomSheet"

        private fun newInstance(signerName: String) = WalletUpdateBottomSheet().apply {
            arguments = WalletUpdateBottomSheetArgs(signerName).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, walletName: String): WalletUpdateBottomSheet {
            return newInstance(walletName).apply { show(fragmentManager, TAG) }
        }
    }
}

data class WalletUpdateBottomSheetArgs(val walletName: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_NAME, walletName)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"

        fun deserializeFrom(data: Bundle?) = WalletUpdateBottomSheetArgs(
            data?.getString(EXTRA_WALLET_NAME).orEmpty()
        )
    }
}