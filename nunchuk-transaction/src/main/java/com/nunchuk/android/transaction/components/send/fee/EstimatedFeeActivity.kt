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

package com.nunchuk.android.transaction.components.send.fee

import android.app.Activity
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.sheet.BottomSheetTooltip
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getHtmlText
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.setUnderline
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeCompletedEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeErrorEvent
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.transaction.databinding.ActivityTransactionEstimateFeeBinding
import com.nunchuk.android.utils.parcelableArrayList
import com.nunchuk.android.utils.safeManualFee
import com.nunchuk.android.utils.textChanges
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class EstimatedFeeActivity : BaseActivity<ActivityTransactionEstimateFeeBinding>() {

    private val args: EstimatedFeeArgs by lazy { EstimatedFeeArgs.deserializeFrom(intent) }

    private val viewModel: EstimatedFeeViewModel by viewModels()

    private val coinSelectLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val selectedCoins =
                    data.parcelableArrayList<UnspentOutput>(GlobalResultKey.EXTRA_COINS).orEmpty()
                if (selectedCoins.isNotEmpty()) {
                    NCToastMessage(this).show(getString(R.string.nc_coin_selection_updated))
                    viewModel.updateNewInputs(selectedCoins)
                }
            }
        }

    override fun initializeBinding() = ActivityTransactionEstimateFeeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        viewModel.init(args)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    @OptIn(FlowPreview::class)
    private fun setupViews() {
        binding.tvCustomize.isVisible = !args.isConsolidateFlow
        binding.tvCustomize.setUnderline()
        binding.toolbarTitle.text = args.sweepType.toTitle(
            this,
            args.title.ifEmpty { getString(R.string.nc_customize_transaction) })
        val subtractFeeFromAmount = args.subtractFeeFromAmount
        viewModel.handleSubtractFeeSwitch(subtractFeeFromAmount, !subtractFeeFromAmount)

        binding.subtractFeeCheckBox.setOnClickListener {
            viewModel.handleSubtractFeeSwitch(
                binding.subtractFeeCheckBox.isChecked
            )
        }
        binding.manualFeeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            handleManualFeeSwitch(
                isChecked
            )
        }
        binding.feeRateInput.textChanges()
            .onEach { binding.btnContinue.tag = true }
            .debounce(1000L)
            .onEach { viewModel.updateFeeRate(it.safeManualFee()) }
            .onEach { binding.btnContinue.tag = false }
            .launchIn(lifecycleScope)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
            val isCalculatingFee = it.tag
            if (isCalculatingFee is Boolean && isCalculatingFee) {
                NCToastMessage(this).showWarning(getString(R.string.nc_wait_to_estimate_fee))
                return@setOnClickListener
            }
            if (binding.manualFeeCheckBox.isChecked.not() || viewModel.validateFeeRate(binding.feeRateInput.text.safeManualFee())) {
                viewModel.handleContinueEvent()
            }
        }
        binding.estimatedFeeLabel.setOnClickListener {
            showEstimatedFeeTooltip()
        }

        if (args.rollOverWalletParam != null) {
            bindRollOverTotalAmount(viewModel.getRollOverTotalAmount())
        } else {
            bindSubtotal(viewModel.getOutputAmount())
        }

        binding.tvCustomize.setOnDebounceClickListener {
            navigator.openCoinList(
                launcher = coinSelectLauncher,
                context = this,
                walletId = args.walletId,
                inputs = viewModel.getInputsCoins(),
                amount = binding.totalAmountBTC.tag as Double
            )
        }
    }

    private fun handleManualFeeSwitch(isChecked: Boolean) {
        viewModel.handleManualFeeSwitch(isChecked)
        binding.feeRateInput.setText("${viewModel.defaultRate / 1000}")
    }

    private fun bindSubtotal(subtotal: Double) {
        binding.totalAmountBTC.tag = subtotal
        binding.totalAmountBTC.text = subtotal.getBTCAmount()
        binding.totalAmountUSD.text = subtotal.getCurrencyAmount()
    }

    private fun bindRollOverTotalAmount(totalAmount: Amount) {
        binding.totalAmountBTC.tag = totalAmount.value
        binding.totalAmountBTC.text = totalAmount.getBTCAmount()
        binding.totalAmountUSD.text = totalAmount.getCurrencyAmount()
    }

    private fun handleState(state: EstimatedFeeState) {
        binding.estimatedFeeBTC.text = state.estimatedFee.getBTCAmount()
        binding.estimatedFeeUSD.text = state.estimatedFee.getCurrencyAmount()

        binding.subtractFeeCheckBox.isChecked = state.subtractFeeFromAmount
        binding.subtractFeeCheckBox.isEnabled = state.enableSubtractFeeFromAmount

        binding.tvTaprootEffectiveFee.isVisible = state.scriptPathFee.value > 0 && !state.isValueKeySetDisable
        binding.tvTaprootEffectiveFee.text = getHtmlText(
            R.string.nc_transaction_taproot_effective_fee_rate,
            (state.scriptPathFee.value.toDouble() / 1000.0).formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)
        )

        binding.tvEffectiveFee.isVisible = state.cpfpFee.value > 0
        binding.manualFeeDesc.isVisible = binding.tvEffectiveFee.isVisible
        binding.tvEffectiveFee.text = getHtmlText(
            R.string.nc_transaction_effective_fee_rate,
            (state.cpfpFee.value.toDouble() / 1000.0).formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)
        )

        if (state.subtractFeeFromAmount) {
            if (args.rollOverWalletParam != null) {
                bindRollOverTotalAmount(viewModel.getRollOverTotalAmount())
            } else {
                bindSubtotal(viewModel.getOutputAmount())
            }
        } else {
            bindSubtotal(
                (viewModel.getOutputAmount() + state.estimatedFee.pureBTC()).coerceAtMost(
                    args.availableAmount
                )
            )
        }

        binding.manualFeeDetails.isVisible = state.manualFeeDetails
        bindEstimateFeeRates(state.estimateFeeRates)
        if (args.inputs.isEmpty() && args.subtractFeeFromAmount) {
            binding.composeCoinSelection.isVisible = false
            binding.coinSelectionTitle.isVisible = false
        } else {
            val inputs =
                state.allCoins.filter { coin -> state.inputs.any { input -> input.first == coin.txid && input.second == coin.vout } }
            binding.coinSelectionTitle.isVisible = inputs.isNotEmpty()
            binding.composeCoinSelection.isVisible = inputs.isNotEmpty()
            if (inputs.isNotEmpty()) {
                binding.composeCoinSelection.setContent {
                    TransactionCoinSelection(inputs = inputs, allTags = state.allTags)
                }
            }
        }
    }

    private fun showEstimatedFeeTooltip() {
        BottomSheetTooltip.newInstance(
            title = getString(R.string.nc_text_info),
            message = getString(R.string.nc_estimated_fee_tooltip),
        ).show(supportFragmentManager, "BottomSheetTooltip")
    }

    private fun bindEstimateFeeRates(estimateFeeRates: EstimateFeeRates) {
        binding.priorityRateValue.text = estimateFeeRates.priorityRate.toFeeRate()
        binding.standardRateValue.text = estimateFeeRates.standardRate.toFeeRate()
        binding.economicalRateValue.text = estimateFeeRates.economicRate.toFeeRate()
    }

    private fun handleEvent(event: EstimatedFeeEvent) {
        when (event) {
            is EstimatedFeeErrorEvent -> onEstimatedFeeError(event)
            is EstimatedFeeCompletedEvent -> {
                if (args.rollOverWalletParam != null) {
                    navigator.openRollOverWalletScreen(
                        activityContext = this,
                        oldWalletId = args.walletId,
                        newWalletId = args.rollOverWalletParam!!.newWalletId,
                        startScreen = RollOverWalletFlow.PREVIEW,
                        selectedTagIds = args.rollOverWalletParam!!.tags.map { it.id },
                        selectedCollectionIds = args.rollOverWalletParam!!.collections.map { it.id },
                        feeRate = event.manualFeeRate.toManualFeeRate(),
                        source = args.rollOverWalletParam!!.source
                    )
                } else {
                    openTransactionConfirmScreen(
                        subtractFeeFromAmount = event.subtractFeeFromAmount,
                        manualFeeRate = event.manualFeeRate
                    )
                }
            }

            is EstimatedFeeEvent.Loading -> showOrHideLoading(event.isLoading)
            is EstimatedFeeEvent.InvalidManualFee -> {
                NCWarningDialog(this).showDialog(
                    message = getString(R.string.nc_transaction_smaller_than_minimum_fee_msg),
                    onYesClick = viewModel::handleContinueEvent
                )
            }

            is EstimatedFeeEvent.GetFeeRateSuccess -> {}
            EstimatedFeeEvent.DraftTransactionSuccess -> {}
        }
    }

    private fun onEstimatedFeeError(event: EstimatedFeeErrorEvent) {
        NCToastMessage(this).showError(event.message)
    }

    private fun openTransactionConfirmScreen(
        subtractFeeFromAmount: Boolean,
        manualFeeRate: Int
    ) {
        navigator.openTransactionConfirmScreen(
            activityContext = this,
            walletId = args.walletId,
            availableAmount = args.availableAmount,
            txReceipts = args.txReceipts,
            privateNote = args.privateNote,
            subtractFeeFromAmount = subtractFeeFromAmount,
            manualFeeRate = manualFeeRate,
            sweepType = args.sweepType,
            slots = args.slots,
            inputs = viewModel.getSelectedCoins(),
            claimInheritanceTxParam = args.claimInheritanceTxParam,
            actionButtonText = args.confirmTxActionButtonText
        )
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            availableAmount: Double,
            txReceipts: List<TxReceipt>,
            privateNote: String,
            subtractFeeFromAmount: Boolean = false,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot>,
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
            inputs: List<UnspentOutput> = emptyList(),
            isConsolidateFlow: Boolean = false,
            title: String = "",
            rollOverWalletParam: RollOverWalletParam? = null,
            confirmTxActionButtonText: String = ""
        ) {
            activityContext.startActivity(
                EstimatedFeeArgs(
                    walletId = walletId,
                    txReceipts = txReceipts,
                    availableAmount = availableAmount,
                    privateNote = privateNote,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    sweepType = sweepType,
                    slots = slots,
                    claimInheritanceTxParam = claimInheritanceTxParam,
                    inputs = inputs,
                    isConsolidateFlow = isConsolidateFlow,
                    title = title,
                    rollOverWalletParam = rollOverWalletParam,
                    confirmTxActionButtonText = confirmTxActionButtonText
                ).buildIntent(activityContext)
            )
        }

    }

}

internal fun Int.toFeeRate() = (this / 1000).toString() + " sat/vB"
internal fun Int.toFeeRateInBtc() = (this / 1000).toAmount().getBTCAmount() + "/vB"
