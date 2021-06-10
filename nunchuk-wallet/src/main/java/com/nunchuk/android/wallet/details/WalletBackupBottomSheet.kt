package com.nunchuk.android.wallet.details

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.wallet.databinding.DialogBackupWalletBottomSheetBinding
import com.nunchuk.android.wallet.details.WalletDetailsOption.*
import com.nunchuk.android.widget.util.expandDialog

internal class WalletUpdateBottomSheet : BottomSheetDialogFragment() {

    private lateinit var listener: (WalletDetailsOption) -> Unit

    private var _binding: DialogBackupWalletBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogBackupWalletBottomSheetBinding.inflate(inflater, container, false)
        dialog?.setOnShowListener(DialogInterface::expandDialog)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.btnBackup.setOnClickListener { onSaveClicked(BACKUP) }
        binding.btnUpload.setOnClickListener { onSaveClicked(UPLOAD) }
        binding.btnQR.setOnClickListener { onSaveClicked(QR) }
        binding.btnDelete.setOnClickListener { onSaveClicked(DELETE) }
    }

    private fun onSaveClicked(option: WalletDetailsOption) {
        listener(option)
        dismiss()
    }


    fun setListener(listener: (WalletDetailsOption) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "WalletUpdateBottomSheet"

        fun show(fragmentManager: FragmentManager): WalletUpdateBottomSheet {
            return WalletUpdateBottomSheet().apply { show(fragmentManager, TAG) }
        }
    }
}

