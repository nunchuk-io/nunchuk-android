
package com.nunchuk.android.transaction.send.amount

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.databinding.ActivityTransactionInputAmountBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class InputAmountActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val args: InputAmountArgs by lazy { InputAmountArgs.deserializeFrom(intent) }

    private val viewModel: InputAmountViewModel by lazy {
        ViewModelProviders.of(this, factory).get(InputAmountViewModel::class.java)
    }

    private lateinit var binding: ActivityTransactionInputAmountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityTransactionInputAmountBinding.inflate(layoutInflater)
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
            navigator.openEstimatedFeeScreen(this, walletId = args.walletId, 0.0)
        }
    }

    private fun handleState(state: InputAmountState) {

    }

    private fun handleEvent(event: InputAmountEvent) {
    }


    companion object {

        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(InputAmountArgs(walletId = walletId).buildIntent(activityContext))
        }

    }

}