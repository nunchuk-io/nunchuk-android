package com.nunchuk.android.wallet.personal.components.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.wallet.personal.databinding.BottomSheetWalletRecoveryActionBinding

internal class RecoverWalletActionBottomSheet : BaseBottomSheet<BottomSheetWalletRecoveryActionBinding>() {

    lateinit var listener: (RecoverWalletOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetWalletRecoveryActionBinding {
        return BottomSheetWalletRecoveryActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.btnUsingQrCode.setOnClickListener { onActionClicked(RecoverWalletOption.QrCode) }
        binding.btnUsingBSMSFile.setOnClickListener { onActionClicked(RecoverWalletOption.BSMSFile) }
        binding.btnRecoverFromColdcard.setOnClickListener { onActionClicked(RecoverWalletOption.ColdCard) }
    }

    private fun onActionClicked(option: RecoverWalletOption) {
        listener(option)
        dismiss()
    }

    companion object {
        private const val TAG = "RecoverWalletActionBottomSheet"

        fun show(fragmentManager: FragmentManager): RecoverWalletActionBottomSheet {
            return RecoverWalletActionBottomSheet().apply { show(fragmentManager, TAG) }
        }
    }
}

sealed class RecoverWalletOption {
    object QrCode : RecoverWalletOption()
    object BSMSFile : RecoverWalletOption()
    object ColdCard : RecoverWalletOption()
}


