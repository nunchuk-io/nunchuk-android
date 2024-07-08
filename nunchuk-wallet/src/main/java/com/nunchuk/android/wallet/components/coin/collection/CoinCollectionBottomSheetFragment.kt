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

package com.nunchuk.android.wallet.components.coin.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.coin.CollectionFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.core.util.shorten
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.databinding.BottomSheetCoinCollectionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinCollectionBottomSheetFragment : BaseBottomSheet<BottomSheetCoinCollectionBinding>() {

    private val viewModel: CoinCollectionBottomSheetViewModel by viewModels()
    private val args: CoinCollectionBottomSheetFragmentArgs by navArgs()
    private val coinListViewModel: CoinListViewModel by activityViewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetCoinCollectionBinding {
        return BottomSheetCoinCollectionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()

        flowObserver(coinListViewModel.state) { coinListState ->
            viewModel.setCollections(coinListState.collections.values.toList())
        }
    }

    private fun setupViews() {
        binding.collectionNameInput.getEditTextView().doAfterTextChanged {
            binding.createCollectionButton.isEnabled = it.toString().isNotBlank()
        }
        binding.collectionNameInput.getEditTextView().setText(args.coinCollection?.name.orEmpty())
        binding.createCollectionButton.setOnClickListener {
            viewModel.createCoinCollection(binding.collectionNameInput.getEditText().trim())
        }
        binding.switchButtonAutoLock.isChecked = args.coinCollection?.isAutoLock.orFalse()
        binding.switchButtonMoveNewCoins.isChecked = args.coinCollection?.isAddNewCoin.orFalse()

        binding.switchButtonAutoLock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoLock(isChecked)
        }
        binding.switchButtonMoveNewCoins.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAddNewCoin(isChecked)
        }
        val buttonText =
            if (args.flow == CollectionFlow.ADD) getString(R.string.nc_create_collection) else getString(
                R.string.nc_update_collection_setting
            )
        binding.createCollectionButton.text = buttonText
        binding.avatarHolder.text = args.coinCollection?.name?.shorten()
        flowObserver(viewModel.event) { event ->
            when (event) {
                is CoinCollectionBottomSheetEvent.Error -> showError(message = event.message)
                CoinCollectionBottomSheetEvent.CreateOrUpdateCollectionSuccess -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_COIN_COLLECTION to viewModel.getCoinCollection())
                    )
                    dismissAllowingStateLoss()
                }

                CoinCollectionBottomSheetEvent.ExistedCollectionError -> showError(message = getString(R.string.nc_collection_name_already_exists))
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "CoinCollectionBottomSheetFragment"
        const val EXTRA_COIN_COLLECTION = "EXTRA_COIN_COLLECTION"
    }
}