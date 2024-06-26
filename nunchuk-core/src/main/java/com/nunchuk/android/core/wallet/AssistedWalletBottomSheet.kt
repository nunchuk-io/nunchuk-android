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

package com.nunchuk.android.core.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.databinding.BottomSheetAssistedWalletBinding
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.share.result.GlobalResultKey
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AssistedWalletBottomSheet : BaseBottomSheet<BottomSheetAssistedWalletBinding>() {
    private val viewModel by viewModels<AssistedWalletViewModel>()

    @Inject
    lateinit var assistedWalletManager: AssistedWalletManager

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetAssistedWalletBinding {
        return BottomSheetAssistedWalletBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = BottomSheetAssistedWalletBinding.bind(view)
        val assistedWalletIds = requireArguments().getStringArrayList(EXTRA_WALLET_IDS).orEmpty()
        val lockdownWalletIds =
            requireArguments().getStringArrayList(EXTRA_LOCKDOWN_WALLET_IDS).orEmpty()
        val title = requireArguments().getString(EXTRA_TITLE)
        if (!title.isNullOrEmpty()) {
            binding.tvTitle.text = title
        }
        viewModel.loadWallets(assistedWalletIds)
        flowObserver(viewModel.state) { wallets ->
            WalletsViewBinder(
                container = binding.walletList,
                wallets = wallets,
                isAssistedWallet = assistedWalletManager::isActiveAssistedWallet,
                isLockedWallet = { assistedWalletManager.getBriefWallet(it)?.status == WalletStatus.LOCKED.name },
                lockdownWalletIds = lockdownWalletIds.toSet(),
                callback = {
                    if (lockdownWalletIds.isEmpty() || lockdownWalletIds.contains(it).not()) {
                        setFragmentResult(
                            TAG, bundleOf(GlobalResultKey.WALLET_ID to it)
                        )
                    }
                    dismissAllowingStateLoss()
                }
            ).bindItems()
        }
    }

    companion object {
        const val TAG = "AddContactsBottomSheet"
        private const val EXTRA_WALLET_IDS = "wallet_ids"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LOCKDOWN_WALLET_IDS = "lockdown_wallet_ids"

        fun show(
            fragmentManager: FragmentManager,
            assistedWalletIds: List<String>,
            title: String? = null,
            lockdownWalletIds: List<String> = emptyList()
        ) = AssistedWalletBottomSheet().apply {
            arguments = Bundle().apply {
                putStringArrayList(EXTRA_WALLET_IDS, ArrayList(assistedWalletIds))
                putStringArrayList(EXTRA_LOCKDOWN_WALLET_IDS, ArrayList(lockdownWalletIds))
                putString(EXTRA_TITLE, title)
            }
            show(fragmentManager, TAG)
        }
    }
}