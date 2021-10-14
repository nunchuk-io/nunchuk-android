package com.nunchuk.android.transaction.components.send.fee

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeCompletedEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent.EstimatedFeeErrorEvent
import com.nunchuk.android.transaction.databinding.ActivityTransactionEstimateFeeBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class EstimatedFeeActivity : BaseActivity<ActivityTransactionEstimateFeeBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: EstimatedFeeArgs by lazy { EstimatedFeeArgs.deserializeFrom(intent) }

    private val viewModel: EstimatedFeeViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityTransactionEstimateFeeBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        binding.customizeFeeSwitch.setOnCheckedChangeListener { _, isChecked -> handleCustomizeFeeSwitch(isChecked) }
        binding.subtractFeeCheckBox.setOnCheckedChangeListener { _, isChecked -> viewModel.handleSubtractFeeSwitch(isChecked) }
        binding.manualFeeCheckBox.setOnCheckedChangeListener { _, isChecked -> viewModel.handleManualFeeSwitch(isChecked) }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
            viewModel.handleContinueEvent()
        }

        bindSubtotal(args.outputAmount)
    }

    private fun handleCustomizeFeeSwitch(isChecked: Boolean) {
        viewModel.handleCustomizeFeeSwitch(isChecked)
        if (!isChecked) {
            binding.subtractFeeCheckBox.isChecked = false
            binding.manualFeeCheckBox.isChecked = false
        }
    }

    private fun bindSubtotal(subtotal: Double) {
        binding.totalAmountBTC.text = subtotal.getBTCAmount()
        binding.totalAmountUSD.text = subtotal.getUSDAmount()
    }

    private fun handleState(state: EstimatedFeeState) {
        binding.estimatedFeeBTC.text = state.estimatedFee.getBTCAmount()
        binding.estimatedFeeUSD.text = state.estimatedFee.getUSDAmount()

        if (state.subtractFeeFromSendMoney) {
            bindSubtotal(args.outputAmount)
        } else {
            bindSubtotal((args.outputAmount + state.estimatedFee.pureBTC()).coerceAtMost(args.availableAmount))
        }

        binding.customizeFeeDetails.isVisible = state.customizeFeeDetails
        binding.manualFeeDetails.isVisible = state.manualFeeDetails
    }

    private fun handleEvent(event: EstimatedFeeEvent) {
        when (event) {
            is EstimatedFeeErrorEvent -> NCToastMessage(this).show(event.message)
            is EstimatedFeeCompletedEvent -> openTransactionConfirmScreen(event.estimatedFee, event.subtractFeeFromSendMoney, event.manualFeeRate)
        }
    }

    private fun openTransactionConfirmScreen(estimatedFee: Double, subtractFeeFromAmount: Boolean, manualFeeRate: Int) {
        navigator.openTransactionConfirmScreen(
            activityContext = this,
            walletId = args.walletId,
            outputAmount = args.outputAmount,
            availableAmount = args.availableAmount,
            address = args.address,
            privateNote = args.privateNote,
            estimatedFee = estimatedFee,
            subtractFeeFromAmount = subtractFeeFromAmount,
            manualFeeRate = manualFeeRate
        )
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            outputAmount: Double,
            availableAmount: Double,
            address: String,
            privateNote: String
        ) {
            activityContext.startActivity(
                EstimatedFeeArgs(
                    walletId = walletId,
                    outputAmount = outputAmount,
                    availableAmount = availableAmount,
                    address = address,
                    privateNote = privateNote
                ).buildIntent(activityContext)
            )
        }

    }

}
