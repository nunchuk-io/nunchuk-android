/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.personal.components.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.wallet.personal.databinding.BottomSheetWalletRecoveryActionBinding

internal class RecoverWalletActionBottomSheet :
    BaseBottomSheet<BottomSheetWalletRecoveryActionBinding>() {

     var listener: ((RecoverWalletOption) -> Unit)? = null

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): BottomSheetWalletRecoveryActionBinding {
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
        binding.btnRecoverHotWallet.setOnClickListener { onActionClicked(RecoverWalletOption.HotWallet) }
        binding.btnRecoverPortalWallet.setOnClickListener { onActionClicked(RecoverWalletOption.PortalWallet) }
    }

    private fun onActionClicked(option: RecoverWalletOption) {
        listener?.invoke(option)
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
    data object QrCode : RecoverWalletOption()
    data object BSMSFile : RecoverWalletOption()
    data object ColdCard : RecoverWalletOption()
    data object HotWallet : RecoverWalletOption()
    data object PortalWallet : RecoverWalletOption()
}


