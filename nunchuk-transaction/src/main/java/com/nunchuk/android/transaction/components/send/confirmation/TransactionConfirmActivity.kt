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

package com.nunchuk.android.transaction.components.send.confirmation

import android.app.Activity
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.*
import com.nunchuk.android.transaction.components.utils.openTransactionDetailScreen
import com.nunchuk.android.transaction.components.utils.showCreateTransactionError
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.transaction.databinding.ActivityTransactionConfirmBinding
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
            address = args.address,
            sendAmount = args.outputAmount,
            subtractFeeFromAmount = args.subtractFeeFromAmount,
            privateNote = args.privateNote,
            manualFeeRate = args.manualFeeRate,
            slots = args.slots
        )
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        observerSweepSatscard(sweepSatscardViewModel, nfcViewModel) { args.walletId }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            sweepSatscardViewModel.init(args.address, args.manualFeeRate)
            sweepSatscardViewModel.handleSweepBalance(IsoDep.get(it.tag), nfcViewModel.inputCvc.orEmpty(), args.slots.toList(), args.sweepType)
            nfcViewModel.clearScanInfo()
        }
    }

    private fun setupViews() {
        binding.toolbarTitle.text = args.sweepType.toTitle(this)
        binding.btnConfirm.text = when (args.sweepType) {
            SweepType.NONE -> getString(R.string.nc_transaction_confirm_and_create_transaction)
            else -> getString(R.string.nc_confirm_and_sweep)
        }
        binding.sendAddressLabel.text = args.address
        binding.estimatedFeeBTC.text = args.estimatedFee.getBTCAmount()
        binding.estimatedFeeUSD.text = args.estimatedFee.getUSDAmount()
        val sendAmount: Double
        val totalAmount: Double
        if (args.subtractFeeFromAmount) {
            sendAmount = args.outputAmount - args.estimatedFee
            totalAmount = args.outputAmount
        } else {
            sendAmount = args.outputAmount
            totalAmount = args.outputAmount + args.estimatedFee
        }
        binding.sendAddressBTC.text = sendAmount.getBTCAmount()
        binding.sendAddressUSD.text = sendAmount.getUSDAmount()
        binding.totalAmountBTC.text = totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = totalAmount.getUSDAmount()
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
    }

    private fun handleEvent(event: TransactionConfirmEvent) {
        when (event) {
            is CreateTxErrorEvent -> showCreateTransactionError(event.message)
            is CreateTxSuccessEvent -> openTransactionDetailScreen(event.txId, args.walletId, sessionHolder.getActiveRoomIdSafe())
            is UpdateChangeAddress -> bindChangAddress(event.address, event.amount)
            LoadingEvent -> showLoading()
            is InitRoomTransactionError -> showCreateTransactionError(event.message)
            is InitRoomTransactionSuccess -> returnActiveRoom(event.roomId)
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
            binding.changeAddressUSD.text = amount.getUSDAmount()
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
            outputAmount: Double,
            availableAmount: Double,
            address: String,
            privateNote: String,
            estimatedFee: Double,
            subtractFeeFromAmount: Boolean = false,
            manualFeeRate: Int = 0,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot> = emptyList()
        ) {
            activityContext.startActivity(
                TransactionConfirmArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    address = address,
                    privateNote = privateNote,
                    estimatedFee = estimatedFee,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate,
                    sweepType = sweepType,
                    slots = slots
                ).buildIntent(activityContext)
            )
        }

    }

}