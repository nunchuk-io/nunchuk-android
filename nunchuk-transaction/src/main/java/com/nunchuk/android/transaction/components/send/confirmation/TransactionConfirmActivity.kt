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

package com.nunchuk.android.transaction.components.send.confirmation

import android.app.Activity
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.sheet.BottomSheetTooltip
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.TxReceiptViewBinder
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.AssignTagEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxErrorEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxSuccessEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionError
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionSuccess
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.LoadingEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.UpdateChangeAddress
import com.nunchuk.android.transaction.components.send.confirmation.tag.AssignTagFragment
import com.nunchuk.android.transaction.components.utils.openTransactionDetailScreen
import com.nunchuk.android.transaction.components.utils.showCreateTransactionError
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.transaction.databinding.ActivityTransactionConfirmBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
class TransactionConfirmActivity : BaseNfcActivity<ActivityTransactionConfirmBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: TransactionConfirmArgs by lazy { TransactionConfirmArgs.deserializeFrom(intent) }

    private val viewModel: TransactionConfirmViewModel by viewModels()

    private val sweepSatscardViewModel: SweepSatscardViewModel by viewModels()

    override fun initializeBinding() = ActivityTransactionConfirmBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        observeEvent()
        viewModel.init(
            walletId = args.walletId,
            txReceipts = args.txReceipts,
            subtractFeeFromAmount = args.subtractFeeFromAmount,
            privateNote = args.privateNote,
            manualFeeRate = args.manualFeeRate,
            slots = args.slots,
            claimInheritanceTxParam = args.claimInheritanceTxParam,
            inputs = args.inputs,
            antiFeeSniping = args.antiFeeSniping
        )
        setupViews()
        viewModel.draftTransaction()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        observerSweepSatscard(sweepSatscardViewModel, nfcViewModel) { args.walletId }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            sweepSatscardViewModel.init(args.txReceipts.first().address, args.manualFeeRate)
            sweepSatscardViewModel.handleSweepBalance(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
                args.slots.toList(),
                args.sweepType
            )
            nfcViewModel.clearScanInfo()
        }
        flowObserver(viewModel.uiState) { uiState ->
            binding.estimatedFeeBTC.text = uiState.transaction.fee.getBTCAmount()
            binding.estimatedFeeUSD.text = uiState.transaction.fee.getCurrencyAmount()
            val totalAmount: Double
            val outputAmount = args.txReceipts.sumOf { it.amount }
            totalAmount = if (args.subtractFeeFromAmount) {
                outputAmount
            } else {
                outputAmount + uiState.transaction.fee.pureBTC()
            }
            binding.totalAmountBTC.text = totalAmount.getBTCAmount()
            binding.totalAmountUSD.text = totalAmount.getCurrencyAmount()
            if (args.inputs.isNotEmpty()) {
                binding.composeCoin.setContent {
                    TransactionConfirmCoinList(args.inputs, uiState.allTags)
                }
            }
        }
    }

    private fun setupViews() {
        binding.toolbarTitle.text =
            args.sweepType.toTitle(
                this,
                getString(R.string.nc_transaction_confirm_transaction),
                true
            )
        binding.btnConfirm.text = if (viewModel.isInheritanceClaimingFlow()) {
            getString(R.string.nc_confirm_withdraw_balance)
        } else if (args.sweepType == SweepType.NONE) {
            getString(R.string.nc_transaction_confirm_and_create_transaction)
        } else {
            getString(R.string.nc_confirm_and_sweep)
        }
        binding.noteContent.isVisible = args.privateNote.isNotEmpty()
        binding.privateNote.isVisible = args.privateNote.isNotEmpty()
        binding.noteContent.text = args.privateNote
        binding.btnConfirm.text = args.actionButtonText.ifEmpty { binding.btnConfirm.text }

        binding.btnConfirm.setOnClickListener {
            if (args.slots.isNotEmpty()) {
                startNfcFlow(REQUEST_SATSCARD_SWEEP_SLOT)
            } else {
                viewModel.handleConfirmEvent()
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.inputCoin.isVisible = args.inputs.isNotEmpty()
        binding.composeCoin.isVisible = args.inputs.isNotEmpty()

        binding.estimatedFeeLabel.setOnClickListener {
            BottomSheetTooltip.newInstance(
                title = getString(R.string.nc_text_info),
                message = getString(R.string.nc_estimated_fee_tooltip),
            ).show(supportFragmentManager, "BottomSheetTooltip")
        }
    }

    private fun handleCopyContent(content: String) {
        copyToClipboard(label = "Nunchuk", text = content)
        NCToastMessage(this).showMessage(getString(R.string.nc_copied_to_clipboard))
    }

    private fun handleEvent(event: TransactionConfirmEvent) {
        when (event) {
            is CreateTxErrorEvent -> showCreateTransactionError(event.message)
            is CreateTxSuccessEvent -> openTransactionDetailScreen(
                event.transaction.txId,
                args.walletId,
                sessionHolder.getActiveRoomIdSafe(),
                viewModel.isInheritanceClaimingFlow(),
                transaction = if (viewModel.isInheritanceClaimingFlow()) event.transaction else null
            )

            is UpdateChangeAddress -> bindChangAddress(event.address, event.amount)
            is LoadingEvent -> showLoading(message = if (event.isClaimInheritance) getString(R.string.nc_withdrawal_in_progress) else null)
            is InitRoomTransactionError -> showCreateTransactionError(event.message)
            is InitRoomTransactionSuccess -> returnActiveRoom(event.roomId)
            is AssignTagEvent -> {
                hideLoading()
                AssignTagFragment.newInstance(event.walletId, event.output, event.tags)
                    .apply {
                        lifecycle.addObserver(object : DefaultLifecycleObserver {
                            override fun onDestroy(owner: LifecycleOwner) {
                                openTransactionDetailScreen(
                                    event.txId,
                                    args.walletId,
                                    sessionHolder.getActiveRoomIdSafe(),
                                    viewModel.isInheritanceClaimingFlow()
                                )
                            }
                        })
                    }
                    .show(supportFragmentManager, "AssignTagFragment")
            }

            is TransactionConfirmEvent.DraftTransactionSuccess -> {
                val coins = if (event.transaction.outputs.size == 1) {
                    event.transaction.outputs
                } else {
                    val outputs =
                        if (viewModel.isInheritanceClaimingFlow() && event.transaction.hasChangeIndex()) {
                            event.transaction.outputs.filterIndexed { index, _ -> index != event.transaction.changeIndex }
                        } else {
                            event.transaction.outputs
                        }
                    outputs.filter { viewModel.isMyCoin(it) == event.transaction.isReceive }
                }
                TxReceiptViewBinder(binding.receiptList, coins) {
                    handleCopyContent(it)
                }.bindItems()
            }

            is TransactionConfirmEvent.AssignTagError -> {}
            is TransactionConfirmEvent.AssignTagSuccess -> {}
        }
    }

    private fun returnActiveRoom(roomId: String) {
        hideLoading()
        ActivityManager.popUntil(InputAmountActivity::class.java, true)
        if (sessionHolder.isLeaveRoom().not()) {
            navigator.openRoomDetailActivity(this, roomId)
        }
    }

    private fun bindChangAddress(changeAddress: String, amount: Amount) {
        hideLoading()
        if (viewModel.isInheritanceClaimingFlow()) {
            binding.changeAddressGroup.visibility = View.GONE
            return
        }
        if (changeAddress.isNotBlank()) {
            binding.changeAddressLabel.text = changeAddress
            binding.changeAddressBTC.text = amount.getBTCAmount()
            binding.changeAddressUSD.text = amount.getCurrencyAmount()
        } else {
            binding.changeAddress.visibility = View.GONE
            binding.changeAddressLabel.visibility = View.GONE
            binding.changeAddressBTC.visibility = View.GONE
            binding.changeAddressUSD.visibility = View.GONE
        }
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            availableAmount: Double,
            txReceipts: List<TxReceipt>,
            privateNote: String,
            subtractFeeFromAmount: Boolean = false,
            manualFeeRate: Int = 0,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
            inputs: List<UnspentOutput> = emptyList(),
            actionButtonText: String,
            antiFeeSniping: Boolean
        ) {
            activityContext.startActivity(
                TransactionConfirmArgs(
                    walletId = walletId,
                    availableAmount = availableAmount,
                    txReceipts = txReceipts,
                    privateNote = privateNote,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate,
                    sweepType = sweepType,
                    slots = slots,
                    claimInheritanceTxParam = claimInheritanceTxParam,
                    inputs = inputs,
                    actionButtonText = actionButtonText,
                    antiFeeSniping = antiFeeSniping
                ).buildIntent(activityContext)
            )
        }

    }

}