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
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
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
        setupViews()
        observeEvent()
        viewModel.init(
            walletId = args.walletId,
            txReceipts = args.txReceipts,
            subtractFeeFromAmount = args.subtractFeeFromAmount,
            privateNote = args.privateNote,
            manualFeeRate = args.manualFeeRate,
            slots = args.slots,
            masterSignerId = args.masterSignerId,
            magicalPhrase = args.magicalPhrase,
            inputs = args.inputs
        )
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
            if (args.inputs.isNotEmpty()) {
                binding.composeCoin.setContent {
                    TransactionConfirmCoinList(args.inputs, uiState.allTags)
                }
            }
        }
    }

    private fun setupViews() {
        binding.toolbarTitle.text = args.sweepType.toTitle(this, getString(R.string.nc_transaction_confirm_transaction))
        binding.btnConfirm.text = when (args.sweepType) {
            SweepType.NONE -> getString(R.string.nc_transaction_confirm_and_create_transaction)
            else -> getString(R.string.nc_confirm_and_sweep)
        }
        binding.estimatedFeeBTC.text = args.estimatedFee.getBTCAmount()
        binding.estimatedFeeUSD.text = args.estimatedFee.getCurrencyAmount()
        val totalAmount: Double
        val outputAmount = args.txReceipts.sumOf { it.amount }
        totalAmount = if (args.subtractFeeFromAmount) {
            outputAmount
        } else {
            outputAmount + args.estimatedFee
        }
        binding.totalAmountBTC.text = totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = totalAmount.getCurrencyAmount()
        binding.noteContent.isVisible = args.privateNote.isNotEmpty()
        binding.privateNote.isVisible = args.privateNote.isNotEmpty()
        binding.noteContent.text = args.privateNote

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
                viewModel.isInheritanceClaimingFlow()
            )
            is UpdateChangeAddress -> bindChangAddress(event.address, event.amount)
            LoadingEvent -> showLoading()
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
                val coins = event.transaction.outputs.filter { viewModel.isMyCoin(it) == event.transaction.isReceive }
                TxReceiptViewBinder(binding.receiptList, coins) {
                    handleCopyContent(it)
                }.bindItems()
            }
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
            estimatedFee: Double,
            subtractFeeFromAmount: Boolean = false,
            manualFeeRate: Int = 0,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot> = emptyList(),
            masterSignerId: String,
            magicalPhrase: String,
            inputs: List<UnspentOutput> = emptyList(),
        ) {
            activityContext.startActivity(
                TransactionConfirmArgs(
                    walletId = walletId,
                    availableAmount = availableAmount,
                    txReceipts = txReceipts,
                    privateNote = privateNote,
                    estimatedFee = estimatedFee,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate,
                    sweepType = sweepType,
                    slots = slots,
                    masterSignerId = masterSignerId,
                    magicalPhrase = magicalPhrase,
                    inputs = inputs
                ).buildIntent(activityContext)
            )
        }

    }

}