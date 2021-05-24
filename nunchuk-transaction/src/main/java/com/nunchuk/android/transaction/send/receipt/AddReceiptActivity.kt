package com.nunchuk.android.transaction.send.receipt

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.databinding.ActivityTransactionAddReceiptBinding
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmEvent
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmState
import com.nunchuk.android.transaction.send.confirmation.TransactionConfirmViewModel
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AddReceiptActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.deserializeFrom(intent) }

    private val viewModel: TransactionConfirmViewModel by lazy {
        ViewModelProviders.of(this, factory).get(TransactionConfirmViewModel::class.java)
    }

    private lateinit var binding: ActivityTransactionAddReceiptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityTransactionAddReceiptBinding.inflate(layoutInflater)
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
        }
    }

    private fun handleState(state: TransactionConfirmState) {

    }

    private fun handleEvent(event: TransactionConfirmEvent) {
    }

    companion object {

        fun start(activityContext: Context, walletId: String, amount: Double, feeRate: Double) {
            activityContext.startActivity(com.nunchuk.android.transaction.send.confirmation.TransactionConfirmArgs(walletId, amount, feeRate).buildIntent(activityContext))
        }

    }

}