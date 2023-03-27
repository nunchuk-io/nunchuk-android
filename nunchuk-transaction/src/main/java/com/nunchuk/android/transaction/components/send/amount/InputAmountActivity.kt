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

package com.nunchuk.android.transaction.components.send.amount

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.viewModels
import androidx.core.view.doOnPreDraw
import androidx.core.widget.doOnTextChanged
import com.journeyapps.barcodescanner.ScanContract
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.core.util.*
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.*
import com.nunchuk.android.transaction.databinding.ActivityTransactionInputAmountBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputAmountActivity : BaseActivity<ActivityTransactionInputAmountBinding>() {

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
        viewModel.init(args.availableAmount)
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        binding.btnSendAll.setUnderline()
        binding.btnSwitch.setUnderline()
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.setOnMenuItemClickListener {
            startQRCodeScan(launcher)
            true
        }
        binding.mainCurrency.setText("")
        binding.mainCurrency.requestFocus()
        binding.btnSendAll.setOnClickListener { openAddReceiptScreen(args.availableAmount, true) }
        binding.btnSwitch.setOnClickListener { viewModel.switchCurrency() }
        binding.btnContinue.setOnDebounceClickListener {
            viewModel.handleContinueEvent()
        }
        binding.amountBTC.text = args.availableAmount.getBTCAmount()
        binding.amountUSD.text = "(${args.availableAmount.getCurrencyAmount()})"
        binding.mainCurrencyLabel.text = handleTextCurrency()

        val originalTextSize = binding.mainCurrency.textSize
        binding.tvMainCurrency.doOnPreDraw {
            val tvWidth = resources.displayMetrics.widthPixels - resources.getDimensionPixelSize(R.dimen.nc_padding_16) * 3 - it.measuredWidth
            binding.tvMainCurrency.width = tvWidth
        }
        binding.mainCurrency.doOnTextChanged { text, _, _, _ ->
            binding.tvMainCurrency.text = text
            viewModel.handleAmountChanged(text.toString())
            binding.mainCurrency.post {
                if (text.isNullOrBlank()) {
                    binding.mainCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
                    binding.mainCurrencyLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
                    binding.tvMainCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
                } else {
                    val optimalSize = binding.tvMainCurrency.textSize
                    binding.mainCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, optimalSize)
                    binding.mainCurrencyLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, optimalSize)
                }
            }
        }
    }

    private fun handleTextCurrency() = when (CURRENT_DISPLAY_UNIT_TYPE) {
        SAT -> getString(R.string.nc_currency_sat)
        else -> getString(R.string.nc_currency_btc)
    }

    private fun openAddReceiptScreen(outputAmount: Double, subtractFeeFromAmount: Boolean = false) {
        navigator.openAddReceiptScreen(
            this,
            walletId = args.walletId,
            outputAmount = outputAmount,
            availableAmount = args.availableAmount,
            address = viewModel.getAddress(),
            privateNote = viewModel.getPrivateNote(),
            subtractFeeFromAmount = subtractFeeFromAmount
        )
    }

    private fun handleState(state: InputAmountState) {
        if (state.useBtc) {
            binding.mainCurrencyLabel.text = handleTextCurrency()
            binding.btnSwitch.text = getString(R.string.nc_transaction_switch_to_currency_data, LOCAL_CURRENCY)

            val secondaryCurrency = if (LOCAL_CURRENCY == USD_CURRENCY) {
                state.amountUSD.formatCurrencyDecimal()
            } else {
                "${state.amountUSD.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)} $LOCAL_CURRENCY"
            }
            binding.secondaryCurrency.text = secondaryCurrency
        } else {
            binding.mainCurrencyLabel.text = LOCAL_CURRENCY
            binding.btnSwitch.text = getString(R.string.nc_transaction_switch_to_btc)

            val secondaryCurrency = "${state.amountBTC.formatDecimal()} ${getString(R.string.nc_currency_btc)}"
            binding.secondaryCurrency.text = secondaryCurrency
        }
    }

    private fun handleEvent(event: InputAmountEvent) {
        when (event) {
            is SwapCurrencyEvent -> {
                binding.mainCurrency.setText(if (event.amount > 0) {
                    if (LOCAL_CURRENCY == USD_CURRENCY || viewModel.getUseBTC()) {
                        "${event.amount.formatDecimal()}"
                    } else {
                        "${event.amount.formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"
                    }
                } else "")
            }
            is AcceptAmountEvent -> openAddReceiptScreen(event.amount)
            InsufficientFundsEvent -> NCToastMessage(this).showError(getString(R.string.nc_transaction_insufficient_funds))
            is ParseBtcUriSuccess -> {
                if (event.btcUri.amount.value > 0 || viewModel.getAmountBtc() > 0.0) {
                    viewModel.handleContinueEvent()
                } else {
                    NCToastMessage(this).show(getString(R.string.nc_address_detected_please_enter_amount))
                }
            }
            is ShowError -> NCToastMessage(this).showError(event.message)
        }
    }

    companion object {

        fun start(activityContext: Context, roomId: String = "", walletId: String, availableAmount: Double) {
            activityContext.startActivity(
                InputAmountArgs(
                    roomId = roomId,
                    walletId = walletId,
                    availableAmount = availableAmount
                ).buildIntent(activityContext)
            )
        }

    }

}