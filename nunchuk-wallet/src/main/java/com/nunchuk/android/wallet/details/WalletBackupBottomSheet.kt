package com.nunchuk.android.wallet.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheetDialogFragment
import com.nunchuk.android.wallet.databinding.DialogBackupWalletBottomSheetBinding
import com.nunchuk.android.wallet.details.WalletDetailsOption.*

internal class WalletUpdateBottomSheet : BaseBottomSheetDialogFragment<DialogBackupWalletBottomSheetBinding>() {

    private lateinit var listener: (WalletDetailsOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogBackupWalletBottomSheetBinding {
        return DialogBackupWalletBottomSheetBinding.inflate(inflater, container, false)
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

