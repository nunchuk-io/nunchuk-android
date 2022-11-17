/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.transaction.components.details.fee

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.databinding.FragmentTransactionConfirmBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmReplaceTransactionFragment : BaseFragment<FragmentTransactionConfirmBinding>() {
    private val viewModel by viewModels<ConfirmReplaceTransactionViewModel>()
    private val activityArgs: ReplaceFeeArgs by lazy { ReplaceFeeArgs.deserializeFrom(requireActivity().intent) }
    private val args by navArgs<ConfirmReplaceTransactionFragmentArgs>()
    
    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionConfirmBinding {
        return FragmentTransactionConfirmBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        updateTransaction(activityArgs.transaction)
        registerEvents()
        viewModel.draftTransaction(activityArgs.walletId, activityArgs.transaction, args.newFee)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnConfirm.setOnDebounceClickListener {
            viewModel.replaceTransaction(activityArgs.walletId, activityArgs.transaction.txId, args.newFee)
        }
    }

    private fun updateTransaction(transaction: Transaction) {
        binding.sendAddressLabel.text = transaction.outputs.firstOrNull()?.first
        binding.estimatedFeeBTC.text = transaction.fee.pureBTC().getBTCAmount()
        binding.estimatedFeeUSD.text = transaction.fee.pureBTC().getUSDAmount()
        binding.sendAddressBTC.text = transaction.subAmount.pureBTC().getBTCAmount()
        binding.sendAddressUSD.text = transaction.subAmount.pureBTC().getUSDAmount()
        binding.totalAmountBTC.text = transaction.totalAmount.pureBTC().getBTCAmount()
        binding.totalAmountUSD.text = transaction.totalAmount.pureBTC().getUSDAmount()
        binding.noteContent.text = transaction.memo

        val txOutput = transaction.outputs.getOrNull(transaction.changeIndex)
        val changeAddress = txOutput?.first.orEmpty()
        if (changeAddress.isNotBlank() && txOutput != null) {
            val amount = txOutput.second
            binding.changeAddressLabel.text = changeAddress
            binding.changeAddressBTC.text = amount.getBTCAmount()
            binding.changeAddressUSD.text = amount.getUSDAmount()
        } else {
            binding.changeAddress.visibility = View.GONE
            binding.changeAddressLabel.visibility = View.GONE
            binding.changeAddressBTC.visibility = View.GONE
            binding.changeAddressUSD.visibility = View.GONE
        }
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            when (it) {
                is ReplaceFeeEvent.Loading -> showOrHideLoading(it.isLoading)
                is ReplaceFeeEvent.ReplaceTransactionSuccess -> {
                    requireActivity().setResult(Activity.RESULT_OK, activityArgs.copy(transaction = activityArgs.transaction.copy(txId = it.newTxId)).buildIntent(requireActivity()))
                    requireActivity().finish()
                }
                is ReplaceFeeEvent.ShowError -> NCToastMessage(requireActivity()).showError(it.e?.message.orUnknownError())
            }
        }
        flowObserver(viewModel.state) {
            updateTransaction(it.transaction)
        }
    }
}