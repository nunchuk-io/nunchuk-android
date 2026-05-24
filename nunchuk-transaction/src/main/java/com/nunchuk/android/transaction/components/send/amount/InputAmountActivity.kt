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

package com.nunchuk.android.transaction.components.send.amount

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.BtcUri
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.args.AddReceiptType
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.AcceptAmountEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.InsufficientFundsEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.InsufficientFundsLockedCoinEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.InvalidAmountEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.Loading
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.ParseBtcUriSuccess
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.ShowError
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class InputAmountActivity : BaseComposeActivity(), BottomSheetOptionListener {

    private val args: InputAmountArgs by lazy { InputAmountArgs.deserializeFrom(intent) }

    private val viewModel: InputAmountViewModel by viewModels()
    private val stablecoinViewModel: InputStablecoinAmountViewModel by viewModels()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            viewModel.parseBtcUri(content)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            if (args.isStablecoin) {
                val stablecoinState by stablecoinViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    stablecoinViewModel.event.collect(::handleStablecoinEvent)
                }

                InputStablecoinAmountScreen(
                    state = stablecoinState,
                    onClose = { finish() },
                    onScanQrClicked = { startQRCodeScan(launcher) },
                    onBatchTransactionClicked = { /* TODO(stablecoin): batch flow */ },
                    onTokenSelected = stablecoinViewModel::selectToken,
                    onSendAllClicked = stablecoinViewModel::sendAll,
                    onSwitchCurrencyClicked = stablecoinViewModel::switchCurrency,
                    onContinueClicked = stablecoinViewModel::handleContinueEvent,
                    onInputChanged = stablecoinViewModel::handleAmountChanged,
                )
            } else {
                val state by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.event.collect(::handleEvent)
                }

                InputAmountScreen(
                    state = state,
                    isClaimInheritance = isClaimInheritanceFlow(),
                    isFromSelectedCoin = args.inputs.isNotEmpty(),
                    availableAmount = args.availableAmount,
                    onClose = { finish() },
                    onScanQrClicked = { startQRCodeScan(launcher) },
                    onBatchTransactionClicked = {
                        openAddReceiptScreen(
                            outputAmount = viewModel.getAmountBtc(),
                            type = AddReceiptType.BATCH,
                        )
                    },
                    onSendAllClicked = ::onSendAllClicked,
                    onSwitchCurrencyClicked = viewModel::switchCurrency,
                    onContinueClicked = viewModel::handleContinueEvent,
                    onInputChanged = viewModel::handleAmountChanged,
                )
            }
        }
    }

    private fun handleStablecoinEvent(event: InputStablecoinAmountEvent) {
        when (event) {
            is InputStablecoinAmountEvent.AcceptAmountEvent -> {
                navigator.openAddReceiptScreen(
                    activityContext = this,
                    walletId = args.walletId,
                    outputAmount = event.amount,
                    availableAmount = args.availableAmount,
                    inputs = args.inputs,
                    tokenAssetId = event.tokenAssetId,
                )
            }

            InputStablecoinAmountEvent.InsufficientFundsEvent ->
                NCToastMessage(this).showError(getString(R.string.nc_transaction_insufficient_funds))

            InputStablecoinAmountEvent.InvalidAmountEvent ->
                NCToastMessage(this).showError(getString(R.string.nc_amount_must_be_greater_than_0))

            is InputStablecoinAmountEvent.ShowError -> NCToastMessage(this).showError(event.message)
        }
    }

    private fun onSendAllClicked() {
        if ((args.inputs.isNotEmpty() && args.inputs.any { it.isLocked })
            || (args.inputs.isEmpty() && viewModel.isHasLockedCoin())
        ) {
            showUnlockCoinBeforeSend()
        } else {
            val amount = if (viewModel.getUseBTC()) {
                args.availableAmount
            } else {
                args.availableAmount.fromBTCToCurrency()
            }
            viewModel.setInputAmount(amount)
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_SWEEP_TO_WALLET) {
            viewModel.checkWallet(args.claimInheritanceTxParam?.bsms)
        } else if (option.type == SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS) {
            openSweepRecipeScreen()
        }
    }

    private fun openSweepRecipeScreen() {
        val amount = viewModel.getAmountBtc()
        val sweepType = SweepType.SWEEP_TO_EXTERNAL_ADDRESS
        val totalBalance = amount * BTC_SATOSHI_EXCHANGE_RATE
        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
        val subtractFeeFromAmount = if (args.claimInheritanceTxParam != null) {
            args.claimInheritanceTxParam?.totalAmount == totalInBtc
        } else {
            true
        }
        navigator.openAddReceiptScreen(
            activityContext = this,
            walletId = "",
            outputAmount = totalInBtc,
            availableAmount = totalInBtc,
            subtractFeeFromAmount = subtractFeeFromAmount,
            sweepType = sweepType,
            claimInheritanceTxParam = args.claimInheritanceTxParam?.copy(
                customAmount = amount,
                isUseWallet = false,
            ),
        )
    }

    private fun showUnlockCoinBeforeSend() {
        NCInfoDialog(this)
            .showDialog(message = getString(R.string.nc_send_all_locked_coin_msg))
    }

    private fun openAddReceiptScreen(
        outputAmount: Double,
        type: AddReceiptType = AddReceiptType.ADD_RECEIPT,
    ) {
        navigator.openAddReceiptScreen(
            this,
            walletId = args.walletId,
            outputAmount = outputAmount,
            availableAmount = args.availableAmount,
            address = viewModel.getAddress(),
            privateNote = viewModel.getPrivateNote(),
            subtractFeeFromAmount = abs(outputAmount - args.availableAmount).toAmount().value <= 0,
            inputs = args.inputs,
            type = type,
        )
    }

    private fun handleEvent(event: InputAmountEvent) {
        when (event) {
            is InputAmountEvent.SwapCurrencyEvent -> Unit
            is AcceptAmountEvent -> {
                if (isClaimInheritanceFlow()) {
                    showSweepOptions()
                } else {
                    openAddReceiptScreen(outputAmount = event.amount, type = AddReceiptType.ADD_RECEIPT)
                }
            }

            InsufficientFundsEvent -> {
                if (args.inputs.isNotEmpty()) {
                    NCToastMessage(this).showError(getString(R.string.nc_send_amount_too_large))
                } else {
                    NCToastMessage(this).showError(getString(R.string.nc_transaction_insufficient_funds))
                }
            }

            is ParseBtcUriSuccess -> {
                if (event.btcUri.amount.value > 0 || viewModel.getAmountBtc() > 0.0) {
                    viewModel.handleContinueEvent()
                } else {
                    NCToastMessage(this).show(getString(R.string.nc_address_detected_please_enter_amount))
                }
            }

            is ShowError -> NCToastMessage(this).showError(event.message)
            is Loading -> showOrHideLoading(event.isLoading)
            InsufficientFundsLockedCoinEvent -> showUnlockCoinBeforeSend()
            is InputAmountEvent.CheckHasWallet -> {
                if (event.isHasWallet) {
                    navigator.openSelectWalletScreen(
                        activityContext = this,
                        slots = emptyList(),
                        type = SelectWalletType.TYPE_INHERITANCE_WALLET,
                        claimInheritanceTxParam = args.claimInheritanceTxParam?.copy(
                            customAmount = viewModel.getAmountBtc(),
                            isUseWallet = true,
                        ),
                    )
                } else {
                    navigator.openWalletIntermediaryScreen(
                        this,
                        quickWalletParam = QuickWalletParam(
                            claimInheritanceTxParam = args.claimInheritanceTxParam?.copy(
                                customAmount = viewModel.getAmountBtc(),
                                isUseWallet = true,
                            ),
                            type = SelectWalletType.TYPE_INHERITANCE_WALLET,
                        ),
                    )
                }
            }

            is InvalidAmountEvent -> NCToastMessage(this)
                .showError(getString(R.string.nc_amount_must_be_greater_than_0))
        }
    }

    private fun showSweepOptions() {
        (supportFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_WALLET,
                    R.drawable.ic_wallet_info,
                    R.string.nc_withdraw_nunchuk_wallet,
                ),
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS,
                    R.drawable.ic_sending_bitcoin,
                    R.string.nc_withdraw_to_an_address,
                ),
            ),
        )
        dialog.show(supportFragmentManager, "BottomSheetOption")
    }

    private fun isClaimInheritanceFlow(): Boolean {
        return args.claimInheritanceTxParam != null
    }

    private fun Double.toAmount(): Amount =
        Amount(value = (this * BTC_SATOSHI_EXCHANGE_RATE).toLong())

    companion object {

        fun start(
            activityContext: Context,
            walletId: String,
            availableAmount: Double,
            inputs: List<UnspentOutput> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
            btcUri: BtcUri? = null,
            isStablecoin: Boolean = false,
        ) {
            activityContext.startActivity(
                InputAmountArgs(
                    walletId = walletId,
                    availableAmount = availableAmount,
                    inputs = inputs,
                    claimInheritanceTxParam = claimInheritanceTxParam,
                    btcUri = btcUri,
                    isStablecoin = isStablecoin,
                ).buildIntent(activityContext)
            )
        }

    }
}
