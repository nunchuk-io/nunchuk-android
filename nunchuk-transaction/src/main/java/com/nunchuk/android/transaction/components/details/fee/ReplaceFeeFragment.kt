package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.model.EstimateFeeRates
import com.nunchuk.android.transaction.components.send.fee.toFeeRate
import com.nunchuk.android.transaction.components.send.fee.toFeeRateInBtc
import com.nunchuk.android.transaction.databinding.FragmentReplaceByFeeBinding
import com.nunchuk.android.utils.safeManualFee
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceFeeFragment : BaseFragment<FragmentReplaceByFeeBinding>() {
    private val viewModel: ReplaceFeeViewModel by viewModels()
    private val args: ReplaceFeeArgs by lazy { ReplaceFeeArgs.deserializeFrom(requireActivity().intent) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentReplaceByFeeBinding {
        return FragmentReplaceByFeeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
        initViews()
    }

    private fun initViews() {
        val previousFeeRate = args.transaction.feeRate.value.toInt()
        binding.tvOldFeeSat.text = previousFeeRate.toFeeRate()
        binding.tvOldFeeBtc.text = previousFeeRate.toFeeRateInBtc()
        binding.tvNewFeeRateBtc.text = binding.feeRateInput.text.safeManualFee().toFeeRateInBtc()
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.feeRateInput.addTextChangedCallback {
            binding.tvNewFeeRateBtc.text = binding.feeRateInput.text.safeManualFee().toFeeRateInBtc()
        }
        binding.btnContinue.setOnClickListener {
            val newFee = binding.feeRateInput.text.safeManualFee()
            binding.tvError.isVisible = newFee <= previousFeeRate
            if (binding.tvError.isVisible.not()) {
                findNavController().navigate(ReplaceFeeFragmentDirections.actionReplaceFeeFragmentToConfirmReplaceTransactionFragment(newFee))
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
    }

    private fun bindEstimateFeeRates(estimateFeeRates: EstimateFeeRates) {
        binding.priorityRateValue.text = estimateFeeRates.priorityRate.toFeeRate()
        binding.standardRateValue.text = estimateFeeRates.standardRate.toFeeRate()
        binding.economicalRateValue.text = estimateFeeRates.economicRate.toFeeRate()
    }

    companion object {
        fun start(launcher: ActivityResultLauncher<Intent>, context: Context, args: ReplaceFeeArgs) {
            launcher.launch(args.buildIntent(context))
        }
    }
}