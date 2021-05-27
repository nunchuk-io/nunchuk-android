package com.nunchuk.android.transaction.send.fee

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getUSDAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.transaction.databinding.ActivityTransactionEstimateFeeBinding
import com.nunchuk.android.transaction.send.fee.EstimatedFeeEvent.EstimatedFeeErrorEvent
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class EstimatedFeeActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: EstimatedFeeArgs by lazy { EstimatedFeeArgs.deserializeFrom(intent) }

    private val viewModel: EstimatedFeeViewModel by lazy {
        ViewModelProviders.of(this, factory).get(EstimatedFeeViewModel::class.java)
    }

    private lateinit var binding: ActivityTransactionEstimateFeeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityTransactionEstimateFeeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()
    }


    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        binding.customizeFeeSwitch.setOnCheckedChangeListener { _, isChecked -> viewModel.handleCustomizeFeeSwitch(isChecked) }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
        }

        bindSubtotal(args.outputAmount)
    }

    private fun bindSubtotal(subtotal: Double) {
        binding.totalAmountBTC.text = subtotal.getBTCAmount()
        binding.totalAmountUSD.text = subtotal.getUSDAmount()
    }

    private fun handleState(state: EstimatedFeeState) {
        binding.estimatedFeeBTC.text = state.estimatedFee.formattedValue
        binding.estimatedFeeUSD.text = state.estimatedFee.getUSDAmount()

        if (state.subtractFeeFromSendMoney) {
            bindSubtotal(args.outputAmount - state.estimatedFee.pureBTC())
        } else {
            bindSubtotal(args.outputAmount)
        }

        binding.customizeFeeDetails.isVisible = state.customizeFeeDetails
        binding.manualFeeDetails.isVisible = state.manualFeeDetails
    }

    private fun handleEvent(event: EstimatedFeeEvent) {
        when (event) {
            is EstimatedFeeErrorEvent -> NCToastMessage(this).show(event.message)
        }
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