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

package com.nunchuk.android.main.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.components.tabs.AssistedWalletViewModel
import com.nunchuk.android.main.databinding.BottomSheetAssistedWalletBinding
import com.nunchuk.android.share.result.GlobalResultKey
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssistedWalletBottomSheet : BaseBottomSheet<BottomSheetAssistedWalletBinding>() {
    private val viewModel by viewModels<AssistedWalletViewModel>()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetAssistedWalletBinding {
        return BottomSheetAssistedWalletBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val assistedWalletIds = requireArguments().getStringArrayList(EXTRA_WALLET_IDS).orEmpty()
        viewModel.loadWallets(assistedWalletIds)
        flowObserver(viewModel.state) { wallets ->
            WalletsViewBinder(
                container = binding.walletList,
                wallets = wallets,
                assistedWalletIds = assistedWalletIds.toSet(),
                callback = {
                    setFragmentResult(
                        TAG, bundleOf(GlobalResultKey.WALLET_ID to it)
                    )
                    dismissAllowingStateLoss()
                }
            ).bindItems()
        }
    }

    companion object {
        const val TAG = "AddContactsBottomSheet"
        private const val EXTRA_WALLET_IDS = "wallet_ids"

        fun show(fragmentManager: FragmentManager, assistedWalletIds: List<String>) = AssistedWalletBottomSheet().apply {
            arguments = Bundle().apply {
                putStringArrayList(EXTRA_WALLET_IDS, ArrayList(assistedWalletIds))
            }
            show(fragmentManager, TAG)
        }
    }
}