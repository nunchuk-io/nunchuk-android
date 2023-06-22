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

package com.nunchuk.android.wallet.components.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragment
import com.nunchuk.android.wallet.components.coin.filter.CoinFilterFragmentArgs
import com.nunchuk.android.wallet.components.details.TransactionAdapter
import com.nunchuk.android.wallet.databinding.FragmentSearchTransactionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchTransactionFragment : BaseFragment<FragmentSearchTransactionBinding>() {

    private val viewModel: SearchTransactionViewModel by viewModels()
    private val args: SearchTransactionFragmentArgs by navArgs()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSearchTransactionBinding {
        return FragmentSearchTransactionBinding.inflate(inflater, container, false)
    }

    private val adapter: TransactionAdapter = TransactionAdapter {
        navigator.openTransactionDetailsScreen(
            activityContext = requireActivity(),
            walletId = args.walletId,
            txId = it.txId,
            roomId = args.roomId
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        setFragmentResultListener(CoinFilterFragment.REQUEST_KEY) { _, bundle ->
            val filter = CoinFilterFragmentArgs.fromBundle(bundle)
            viewModel.updateFilter(filter.filter)
            clearFragmentResult(CoinFilterFragment.REQUEST_KEY)
        }
    }

    private fun setupViews() {
        binding.input.requestFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.input, InputMethodManager.SHOW_IMPLICIT)

        binding.transactionList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.transactionList.isNestedScrollingEnabled = false
        binding.transactionList.setHasFixedSize(false)
        binding.transactionList.adapter = adapter
        binding.input.doAfterTextChanged {
            viewModel.search(it.toString())
            binding.ivClear.isVisible = it.toString().isNotBlank()
        }
        binding.ivClear.setOnClickListener {
            binding.input.setText("")
        }
        binding.ivFilter.setOnClickListener {
            findNavController().navigate(
                SearchTransactionFragmentDirections.actionSearchTransactionFragmentToCoinFilterFragment(
                    filter = viewModel.filter.value,
                    isSearchTransaction = true
                )
            )
        }
        binding.ivBack.setOnClickListener {
            requireActivity().finish()
        }

        flowObserver(viewModel.state) {
            binding.viewBadge.isVisible = viewModel.isFiltering
            binding.emptyView.isVisible = viewModel.isFilteringOrSearch && it.transactions.isEmpty()
            binding.tvNumResult.isVisible = it.transactions.isEmpty().not()
            binding.tvNumResult.text = resources.getQuantityString(R.plurals.nc_results_found, it.transactions.size, it.transactions.size)
            adapter.submitData(PagingData.from(it.transactions))
        }
    }
}