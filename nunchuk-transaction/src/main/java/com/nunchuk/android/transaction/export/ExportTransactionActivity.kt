package com.nunchuk.android.transaction.export

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.databinding.ActivityExportTransactionBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class ExportTransactionActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val argsExport: ExportTransactionArgs by lazy { ExportTransactionArgs.deserializeFrom(intent) }

    private val viewModel: ExportTransactionViewModel by lazy {
        ViewModelProviders.of(this, factory).get(ExportTransactionViewModel::class.java)
    }

    private lateinit var binding: ActivityExportTransactionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityExportTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeEvent()
        viewModel.init(walletId = argsExport.walletId, txId = argsExport.txId)
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun handleState(state: ExportTransactionState) {

    }

    private fun handleEvent(event: ExportTransactionEvent) {
    }

    companion object {

        fun start(activityContext: Activity, walletId: String, txId: String) {
            activityContext.startActivity(
                ExportTransactionArgs(
                    walletId = walletId,
                    txId = txId
                ).buildIntent(activityContext)
            )
        }

    }

}

