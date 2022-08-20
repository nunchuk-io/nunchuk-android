package com.nunchuk.android.transaction.components.details.fee

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.fee.toFeeRate
import com.nunchuk.android.transaction.databinding.ActivityReplaceByFeeBinding
import com.nunchuk.android.utils.safeManualFee
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceFeeActivity : BaseActivity<ActivityReplaceByFeeBinding>() {
    private val viewModel: ReplaceFeeViewModel by viewModels()
    private val args: ReplaceFeeArgs by lazy { ReplaceFeeArgs.deserializeFrom(intent) }

    override fun initializeBinding(): ActivityReplaceByFeeBinding {
        return ActivityReplaceByFeeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        observer()
        initViews()
    }

    private fun initViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener {
            val newFee = binding.feeRateInput.text.safeManualFee()
            if (viewModel.validateFeeRate(newFee)) {
                viewModel.replaceTransaction(args.walletId, args.txId, newFee)
            } else {
                NCToastMessage(this).showError(getString(R.string.nc_input_fee_invalid_error))
            }
        }
    }

    private fun observer() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    bindEstimateFeeRates(it.estimateFeeRates)
                }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.event.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect {
                    when (it) {
                        is ReplaceFeeEvent.Loading -> showOrHideLoading(it.isLoading)
                        is ReplaceFeeEvent.ReplaceTransactionSuccess -> {
                            setResult(Activity.RESULT_OK, ReplaceFeeArgs(args.walletId, it.newTxId).buildIntent(this@ReplaceFeeActivity))
                            finish()
                        }
                        is ReplaceFeeEvent.ShowError -> NCToastMessage(this@ReplaceFeeActivity).showError(it.e?.message.orUnknownError())
                    }
                }
        }
    }

    private fun bindEstimateFeeRates(estimateFeeRates: EstimateFeeRates) {
        binding.priorityRateValue.text = estimateFeeRates.priorityRate.toFeeRate()
        binding.standardRateValue.text = estimateFeeRates.standardRate.toFeeRate()
        binding.economicalRateValue.text = estimateFeeRates.economicRate.toFeeRate()
    }

    companion object {
        fun start(launcher: ActivityResultLauncher<Intent>, context: Context, walletId: String, txId: String) {
            launcher.launch(ReplaceFeeArgs(walletId, txId).buildIntent(context))
        }
    }
}