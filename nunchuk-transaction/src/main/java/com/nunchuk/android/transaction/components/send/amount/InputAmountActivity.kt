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
import android.util.TypedValue
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.LOCAL_CURRENCY
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.USD_CURRENCY
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatCurrencyDecimal
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getTextBtcUnit
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.AcceptAmountEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.InsufficientFundsEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.InsufficientFundsLockedCoinEvent
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.Loading
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.ParseBtcUriSuccess
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.ShowError
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.SwapCurrencyEvent
import com.nunchuk.android.transaction.databinding.ActivityTransactionInputAmountBinding
import com.nunchuk.android.utils.textChanges
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs

@AndroidEntryPoint
class InputAmountActivity : BaseActivity<ActivityTransactionInputAmountBinding>(),
    BottomSheetOptionListener {

    private val args: InputAmountArgs by lazy { InputAmountArgs.deserializeFrom(intent) }

    private val viewModel: InputAmountViewModel by viewModels()

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            viewModel.parseBtcUri(content)
        }
    }

    override fun initializeBinding() = ActivityTransactionInputAmountBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
        viewModel.init(args.availableAmount, args.walletId, args.inputs.isNotEmpty())
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        if (isClaimInheritanceFlow()) binding.toolbarTitle.text =
            getString(R.string.nc_withdraw_a_custom_amount)
        binding.btnSendAll.setUnderline()
        binding.btnSwitch.setUnderline()
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_scan_qr) {
                startQRCodeScan(launcher)
            } else if (it.itemId == R.id.menu_batch_transaction) {
                navigator.openBatchTransactionScreen(
                    this,
                    roomId = args.roomId,
                    walletId = args.walletId,
                    availableAmount = args.availableAmount,
                    inputs = args.inputs
                )
            }
            true
        }
        binding.mainCurrency.setText("")
        binding.mainCurrency.requestFocus()
        binding.btnSendAll.setOnClickListener {
            if ((args.inputs.isNotEmpty() && args.inputs.any { it.isLocked }) || (args.inputs.isEmpty() && viewModel.isHasLockedCoin())) {
                showUnlockCoinBeforeSend()
            } else {
                showAmount(if (viewModel.getUseBTC()) args.availableAmount else args.availableAmount.fromBTCToCurrency())
            }
        }
        binding.btnSwitch.setOnClickListener { viewModel.switchCurrency() }
        binding.btnContinue.setOnDebounceClickListener {
            viewModel.handleContinueEvent()
        }

        if (args.inputs.isNotEmpty()) {
            binding.balanceLabel.text = getString(R.string.nc_total_amount_selected)
            binding.amountBTC.setTextColor(ContextCompat.getColor(this, R.color.nc_slime_dark))
            binding.amountUSD.setTextColor(ContextCompat.getColor(this, R.color.nc_slime_dark))
        }
        binding.amountBTC.text = args.availableAmount.getBTCAmount()
        binding.amountUSD.text = "(${args.availableAmount.getCurrencyAmount()})"
        binding.mainCurrencyLabel.text = getTextBtcUnit()

        val originalTextSize = binding.mainCurrency.textSize
        binding.mainCurrencyLabel.doOnPreDraw {
            val tvWidth =
                resources.displayMetrics.widthPixels - resources.getDimensionPixelSize(R.dimen.nc_padding_16) * 3 - it.measuredWidth
            binding.tvMainCurrency.maxWidth = tvWidth
        }
        flowObserver(
            binding.mainCurrency.textChanges().stateIn(lifecycleScope, SharingStarted.Eagerly, "")
        ) { text ->
            binding.tvMainCurrency.text = text
            viewModel.handleAmountChanged(text)
            binding.mainCurrency.post {
                if (text.isBlank()) {
                    binding.mainCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
                    binding.mainCurrencyLabel.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        originalTextSize
                    )
                    binding.tvMainCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
                } else {
                    val optimalSize = binding.tvMainCurrency.textSize
                    binding.mainCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, optimalSize)
                    binding.mainCurrencyLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, optimalSize)
                }
            }
        }
        if (args.inputs.isNotEmpty()) {
            binding.btnSendAll.text = getString(R.string.nc_send_all_selected)
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_SWEEP_TO_WALLET) {
            viewModel.checkWallet()
        } else if (option.type == SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS) {
            openSweepRecipeScreen()
        }
    }

    private fun openSweepRecipeScreen() {
        val amount = viewModel.getAmountBtc()
        val sweepType = SweepType.SWEEP_TO_EXTERNAL_ADDRESS
        val totalBalance = amount * BTC_SATOSHI_EXCHANGE_RATE
        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
        navigator.openAddReceiptScreen(
            activityContext = this,
            walletId = "",
            outputAmount = totalInBtc,
            availableAmount = totalInBtc,
            subtractFeeFromAmount = true,
            slots = emptyList(),
            sweepType = sweepType,
            claimInheritanceTxParam = args.claimInheritanceTxParam?.copy(customAmount = amount)
        )
    }

    private fun showUnlockCoinBeforeSend() {
        NCInfoDialog(this)
            .showDialog(message = getString(R.string.nc_send_all_locked_coin_msg))
    }

    private fun openAddReceiptScreen(outputAmount: Double) {
        navigator.openAddReceiptScreen(
            this,
            walletId = args.walletId,
            outputAmount = outputAmount,
            availableAmount = args.availableAmount,
            address = viewModel.getAddress(),
            privateNote = viewModel.getPrivateNote(),
            subtractFeeFromAmount = abs(outputAmount - args.availableAmount).toAmount().value <= 0,
            inputs = args.inputs
        )
    }

    private fun handleState(state: InputAmountState) {
        if (state.useBtc) {
            binding.mainCurrency.allowDecimal(CURRENT_DISPLAY_UNIT_TYPE != SAT)
            binding.mainCurrencyLabel.text = getTextBtcUnit()
            binding.btnSwitch.text =
                getString(R.string.nc_transaction_switch_to_currency_data, LOCAL_CURRENCY)

            val secondaryCurrency = if (LOCAL_CURRENCY == USD_CURRENCY) {
                state.amountUSD.formatCurrencyDecimal()
            } else {
                "${state.amountUSD.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)} $LOCAL_CURRENCY"
            }
            binding.secondaryCurrency.text = secondaryCurrency
        } else {
            binding.mainCurrency.allowDecimal(true)
            binding.mainCurrencyLabel.text = LOCAL_CURRENCY
            binding.btnSwitch.text =
                getString(R.string.nc_transaction_switch_to_currency_data, getTextBtcUnit())

            val secondaryCurrency = state.amountBTC.toAmount().getBTCAmount()
            binding.secondaryCurrency.text = secondaryCurrency
        }
    }

    private fun handleEvent(event: InputAmountEvent) {
        when (event) {
            is SwapCurrencyEvent -> showAmount(event.amount)
            is AcceptAmountEvent -> {
                if (isClaimInheritanceFlow()) {
                    showSweepOptions()
                } else {
                    openAddReceiptScreen(event.amount)
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
                        claimInheritanceTxParam = args.claimInheritanceTxParam?.copy(customAmount = viewModel.getAmountBtc())
                    )
                } else {
                    navigator.openWalletIntermediaryScreen(
                        this,
                        quickWalletParam = QuickWalletParam(
                            claimInheritanceTxParam = args.claimInheritanceTxParam?.copy(
                                customAmount = viewModel.getAmountBtc()
                            ),
                            type = SelectWalletType.TYPE_INHERITANCE_WALLET
                        )
                    )
                }
            }
        }
    }

    private fun showSweepOptions() {
        (supportFragmentManager.findFragmentByTag("BottomSheetOption") as? DialogFragment)?.dismiss()
        val dialog = BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_WALLET,
                    R.drawable.ic_wallet_info,
                    R.string.nc_withdraw_nunchuk_wallet
                ),
                SheetOption(
                    SheetOptionType.TYPE_SWEEP_TO_EXTERNAL_ADDRESS,
                    R.drawable.ic_sending_bitcoin,
                    R.string.nc_withdraw_to_an_address
                ),
            )
        )
        dialog.show(supportFragmentManager, "BottomSheetOption")
    }

    private fun showAmount(amount: Double) {
        binding.mainCurrency.setText(
            if (amount > 0) {
                if (viewModel.getUseBTC()) {
                    if (CURRENT_DISPLAY_UNIT_TYPE == SAT) amount.toAmount().value.formatDecimalWithoutZero() else amount.formatDecimal()
                } else if (LOCAL_CURRENCY == USD_CURRENCY) {
                    amount.formatDecimal()
                } else {
                    amount.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)
                }
            } else ""
        )
    }

    private fun isClaimInheritanceFlow(): Boolean {
        return args.claimInheritanceTxParam != null
    }

    companion object {

        fun start(
            activityContext: Context,
            roomId: String = "",
            walletId: String,
            availableAmount: Double,
            inputs: List<UnspentOutput> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null
        ) {
            activityContext.startActivity(
                InputAmountArgs(
                    roomId = roomId,
                    walletId = walletId,
                    availableAmount = availableAmount,
                    inputs = inputs,
                    claimInheritanceTxParam = claimInheritanceTxParam
                ).buildIntent(activityContext)
            )
        }

    }

}