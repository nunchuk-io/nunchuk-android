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

package com.nunchuk.android.wallet.components.coin.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.core.coin.TagFlow
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.CoinNavigationDirections
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListEvent
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import kotlinx.coroutines.launch

abstract class BaseCoinListFragment : Fragment(), BottomSheetOptionListener {
    protected val coinListViewModel: CoinListViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            coinListViewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is CoinListEvent.Loading -> Unit // it already handled in CoinActivity
                        CoinListEvent.CoinLocked -> {
                            showSuccess(getString(R.string.nc_coin_locked))
                            resetSelect()
                        }

                        is CoinListEvent.CoinUnlocked -> {
                            if (!event.isCreateTransaction) {
                                showSuccess(getString(R.string.nc_coin_unlocked))
                                resetSelect()
                            }
                        }

                        is CoinListEvent.Error -> showError(event.message)
                        CoinListEvent.RemoveCoinFromTagSuccess -> {
                            coinListViewModel.refresh()
                            showSuccess(getString(R.string.nc_tag_updated))
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }

                        CoinListEvent.RemoveCoinFromCollectionSuccess -> {
                            coinListViewModel.refresh()
                            showSuccess(getString(R.string.nc_collection_updated))
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        when (option.type) {
            SheetOptionType.TYPE_LOCK_COIN -> coinListViewModel.onLockCoin(
                walletId,
                getSelectedCoins()
            )

            SheetOptionType.TYPE_UNLOCK_COIN -> coinListViewModel.onUnlockCoin(
                walletId = walletId,
                selectedCoins = getSelectedCoins(),
                isCreateTransaction = false
            )

            SheetOptionType.TYPE_ADD_COLLECTION -> findNavController().navigate(
                CoinNavigationDirections.actionGlobalCoinCollectionListFragment(
                    walletId = walletId,
                    collectionFlow = CollectionFlow.ADD,
                    coins = getSelectedCoins().toTypedArray()
                )
            )

            SheetOptionType.TYPE_ADD_TAG -> findNavController().navigate(
                CoinNavigationDirections.actionGlobalCoinTagListFragment(
                    walletId = walletId,
                    tagFlow = TagFlow.ADD,
                    coins = getSelectedCoins().toTypedArray()
                )
            )

            SheetOptionType.TYPE_CONSOLIDATE_COIN -> {
                findNavController().navigate(
                    CoinNavigationDirections.actionGlobalConsolidateCoinFragment(
                        walletId = walletId,
                        selectedCoins = getSelectedCoins().toTypedArray()
                    )
                )
            }
        }
    }

    open fun showSelectCoinOptions() {
        val options = mutableListOf(
            SheetOption(
                type = SheetOptionType.TYPE_LOCK_COIN,
                label = getString(R.string.nc_lock_coin)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_UNLOCK_COIN,
                label = getString(R.string.nc_unlock_coin)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_COLLECTION,
                label = getString(R.string.nc_add_to_a_collection)
            ),
            SheetOption(
                type = SheetOptionType.TYPE_ADD_TAG,
                label = getString(R.string.nc_add_tags)
            ),
        ).apply {
            if (getSelectedCoins().size > 1) {
                add(
                    index = 0,
                    element = SheetOption(
                        type = SheetOptionType.TYPE_CONSOLIDATE_COIN,
                        label = getString(R.string.nc_consolidate_coins)
                    )
                )
            }
        }
        BottomSheetOption.newInstance(options).show(childFragmentManager, "BottomSheetOption")
    }

    abstract val walletId: String
    abstract fun getSelectedCoins(): List<UnspentOutput>
    abstract fun resetSelect()
}