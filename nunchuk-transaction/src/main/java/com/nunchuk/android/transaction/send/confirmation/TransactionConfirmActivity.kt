package com.nunchuk.android.transaction.send.confirmation

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.databinding.ActivityTransactionConfirmBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class TransactionConfirmActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: TransactionConfirmArgs by lazy { TransactionConfirmArgs.deserializeFrom(intent) }

    private val viewModel: TransactionConfirmViewModel by lazy {
        ViewModelProviders.of(this, factory).get(TransactionConfirmViewModel::class.java)
    }

    private lateinit var binding: ActivityTransactionConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityTransactionConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()
    }


    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        binding.btnConfirm.setOnClickListener {
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleState(state: TransactionConfirmState) {

    }

    private fun handleEvent(event: TransactionConfirmEvent) {
    }

    companion object {

        fun start(activityContext: Context, walletId: String, amount: Double, feeRate: Double) {
            activityContext.startActivity(TransactionConfirmArgs(walletId, amount, feeRate).buildIntent(activityContext))
        }

    }

}