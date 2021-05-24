package com.nunchuk.android.transaction.send.fee

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.databinding.ActivityTransactionEstimateFeeBinding
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
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
            navigator.openAddReceiptScreen(this, args.walletId, args.amount, 0.0)
        }
    }

    private fun handleState(state: EstimatedFeeState) {

    }

    private fun handleEvent(event: EstimatedFeeEvent) {
    }

    companion object {

        fun start(activityContext: Context, walletId: String, amount: Double) {
            activityContext.startActivity(EstimatedFeeArgs(walletId, amount).buildIntent(activityContext))
        }

    }

}