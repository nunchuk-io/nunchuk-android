package com.nunchuk.android.wallet.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.wallet.databinding.DialogUpdateWalletBottomSheetBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.expandDialog

class WalletUpdateBottomSheet : BottomSheetDialogFragment() {

    private lateinit var listener: (String) -> Unit

    private var _binding: DialogUpdateWalletBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val args: WalletUpdateBottomSheetArgs by lazy { WalletUpdateBottomSheetArgs.deserializeFrom(arguments) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogUpdateWalletBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener(DialogInterface::expandDialog)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        val editWalletName: AppCompatEditText = binding.editWalletName
        editWalletName.text?.append(args.walletName)

        editWalletName.addTextChangedCallback {
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
        val newWalletName = binding.editWalletName.text.toString()
        if (newWalletName != args.walletName) {
            listener(newWalletName)
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