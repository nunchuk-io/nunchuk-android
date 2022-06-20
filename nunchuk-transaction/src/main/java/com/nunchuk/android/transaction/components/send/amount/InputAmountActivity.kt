package com.nunchuk.android.transaction.components.send.amount

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.entities.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.entities.SAT
import com.nunchuk.android.core.util.*
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountEvent.*
import com.nunchuk.android.transaction.databinding.ActivityTransactionInputAmountBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputAmountActivity : BaseActivity<ActivityTransactionInputAmountBinding>() {

    private val args: InputAmountArgs by lazy { InputAmountArgs.deserializeFrom(intent) }

    private val viewModel: InputAmountViewModel by viewModels()

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
        binding.mainCurrency.setText("")
        binding.mainCurrency.addTextChangedCallback(viewModel::handleAmountChanged)
        binding.mainCurrency.requestFocus()
        binding.btnSendAll.setOnClickListener { openAddReceiptScreen(args.availableAmount, true) }
        binding.btnSwitch.setOnClickListener { viewModel.switchCurrency() }
        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent()
        }
        binding.amountBTC.text = args.availableAmount.getBTCAmount()
        binding.amountUSD.text = "(${args.availableAmount.getUSDAmount()})"
        binding.mainCurrencyLabel.text = handleTextCurrency()
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
            subtractFeeFromAmount = subtractFeeFromAmount
        )
    }

    private fun handleState(state: InputAmountState) {
        if (state.useBtc) {
            binding.mainCurrencyLabel.text = handleTextCurrency()
            binding.btnSwitch.text = getString(R.string.nc_transaction_switch_to_usd)

            val secondaryCurrency = "${state.amountUSD.formatCurrencyDecimal()} ${getString(R.string.nc_currency_usd)}"
            binding.secondaryCurrency.text = secondaryCurrency
        } else {
            binding.mainCurrencyLabel.text = getString(R.string.nc_currency_usd)
            binding.btnSwitch.text = getString(R.string.nc_transaction_switch_to_btc)

            val secondaryCurrency = "${state.amountBTC.formatDecimal()} ${getString(R.string.nc_currency_btc)}"
            binding.secondaryCurrency.text = secondaryCurrency
        }
    }

    private fun handleEvent(event: InputAmountEvent) {
        when (event) {
            is SwapCurrencyEvent -> binding.mainCurrency.setText(if (event.amount > 0) event.amount.formatCurrencyDecimal() else "")
            is AcceptAmountEvent -> openAddReceiptScreen(event.amount)
            InsufficientFundsEvent -> NCToastMessage(this).showError(getString(R.string.nc_transaction_insufficient_funds))
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