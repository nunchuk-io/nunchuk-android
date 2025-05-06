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

package com.nunchuk.android.signer.satscard.wallets

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.data.model.isInheritanceClaimFlow
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.InheritanceClaimTxDetailInfo
import com.nunchuk.android.core.util.SelectWalletType.TYPE_INHERITANCE_WALLET
import com.nunchuk.android.core.util.SelectWalletType.TYPE_UNSEAL_SWEEP_ACTIVE_SLOT
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSelectWalletSweepBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SelectWalletFragment : BaseFragment<FragmentSelectWalletSweepBinding>() {
    private val viewModel by viewModels<SelectWalletViewModel>()
    private val sweepSatscardViewModel by viewModels<SweepSatscardViewModel>()
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val args: SelectWalletFragmentArgs by navArgs()
    private val adapter = SelectWalletAdapter {
        viewModel.setWalletSelected(it)
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSelectWalletSweepBinding {
        return FragmentSelectWalletSweepBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observer()
        registerEvents()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        binding.btnContinueSweep.setOnDebounceClickListener {
            if (viewModel.selectedWalletId.isNotEmpty()) {
                viewModel.getWalletAddress(true)
            } else {
                NCToastMessage(requireActivity()).showWarning(getString(R.string.nc_select_wallet_first))
            }
        }
        binding.btnCustomFee.setOnDebounceClickListener {
            if (viewModel.selectedWalletId.isNotEmpty()) {
                viewModel.getWalletAddress(false)
            } else {
                NCToastMessage(requireActivity()).showWarning(getString(R.string.nc_select_wallet_first))
            }
        }
    }

    private fun initViews() {
        binding.toolbarTitle.text = if (args.claimParam != null) getString(R.string.nc_withdraw_nunchuk_wallet) else getString(R.string.nc_withdraw_to_a_wallet)
        binding.tvQuestion.text = if (args.claimParam != null) getString(R.string.nc_which_wallet_do_you_want_to_withdraw_bitcoin) else getString(R.string.nc_which_wallet_do_you_want_to_deposit_into)
        binding.btnContinueSweep.text = getString(R.string.nc_continue_to_withdraw)
        binding.recyclerView.adapter = adapter
    }

    private fun observer() {
        flowObserver(viewModel.event, ::handleEvent)
        flowObserver(viewModel.state, ::handleState)
        if (isInheritanceWalletFlow().not()) {
            (activity as BaseNfcActivity<*>).observerSweepSatscard(
                sweepSatscardViewModel,
                nfcViewModel
            ) { viewModel.selectedWalletId }
            flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_SATSCARD_SWEEP_SLOT }) {
                val type = if (args.type == TYPE_UNSEAL_SWEEP_ACTIVE_SLOT) {
                    SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET
                } else {
                    SweepType.SWEEP_TO_NUNCHUK_WALLET
                }
                sweepSatscardViewModel.init(viewModel.selectWalletAddress, viewModel.manualFeeRate)
                sweepSatscardViewModel.handleSweepBalance(
                    IsoDep.get(it.tag),
                    nfcViewModel.inputCvc.orEmpty(),
                    args.slots.toList(),
                    type
                )
                nfcViewModel.clearScanInfo()
            }
        }
    }

    private fun handleEvent(event: SelectWalletEvent) {
        when (event) {
            is SelectWalletEvent.Error -> showError(event.e?.message.orUnknownError())
            is SelectWalletEvent.Loading -> {
                showOrHideLoading(
                    event.isLoading,
                    title = if (event.isClaimInheritance) getString(R.string.nc_please_wait) else getString(R.string.nc_sweeping_is_progress),
                    message = if (event.isClaimInheritance) getString(R.string.nc_withdrawal_in_progress) else getString(R.string.nc_make_sure_internet)
                )
            }
            is SelectWalletEvent.GetAddressSuccess -> if (event.isCreateTransaction) {
                viewModel.getEstimateFeeRates()
            } else {
                navigateToEstimateFee(event.address)
            }
            is SelectWalletEvent.GetFeeRateSuccess -> {
                if (isInheritanceWalletFlow()) {
                    viewModel.createInheritanceTransaction()
                } else {
                    (activity as NfcActionListener).startNfcFlow(
                        BaseNfcActivity.REQUEST_SATSCARD_SWEEP_SLOT
                    )
                }
            }
            is SelectWalletEvent.CreateTransactionSuccessEvent -> {
                navigateToTransactionDetail(event.transaction)
            }
        }
    }

    private fun navigateToTransactionDetail(transaction: Transaction) {
        ActivityManager.popUntilRoot()
        if (viewModel.selectedWalletId.isNotEmpty()) {
            navigator.openWalletDetailsScreen(requireContext(), viewModel.selectedWalletId)
        }
        navigator.openTransactionDetailsScreen(
            activityContext = requireActivity(),
            walletId = viewModel.selectedWalletId,
            txId = transaction.txId,
            initEventId = "",
            roomId = "",
            transaction = transaction,
            inheritanceClaimTxDetailInfo = if (args.claimParam.isInheritanceClaimFlow()) {
                InheritanceClaimTxDetailInfo(
                    changePos = transaction.changeIndex,
                )
            } else null,
        )
    }

    private fun navigateToEstimateFee(address: String) {
        val totalBalance =
            if (isInheritanceWalletFlow()) {
                args.claimParam!!.customAmount * BTC_SATOSHI_EXCHANGE_RATE
            } else {
                args.slots.sumOf { it.balance.value }
            }
        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
        val type = if (args.type == TYPE_UNSEAL_SWEEP_ACTIVE_SLOT) {
            SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET
        } else {
            SweepType.SWEEP_TO_NUNCHUK_WALLET
        }
        navigator.openEstimatedFeeScreen(
            activityContext = requireActivity(),
            walletId = viewModel.selectedWalletId,
            availableAmount = totalInBtc,
            txReceipts = listOf(TxReceipt(address = address, totalInBtc)),
            privateNote = "",
            subtractFeeFromAmount = true,
            sweepType = type,
            slots = args.slots.toList(),
            claimInheritanceTxParam = args.claimParam,
        )
    }

    private fun isInheritanceWalletFlow() = args.type == TYPE_INHERITANCE_WALLET

    private fun handleState(state: SelectWalletState) {
        adapter.submitList(state.selectWallets)
    }
}