package com.nunchuk.android.signer.satscard.wallets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.Amount
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSelectWalletSweepBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectWalletFragment : BaseFragment<FragmentSelectWalletSweepBinding>() {
    private val viewModel by viewModels<SelectWalletViewModel>()
    private val args: SelectWalletFragmentArgs by navArgs()
    private val adapter = SelectWalletAdapter {
        viewModel.setWalletSelected(it)
    }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSelectWalletSweepBinding {
        return FragmentSelectWalletSweepBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        observer()
        registerEvents()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        binding.btnContinue.setOnDebounceClickListener {
            if (viewModel.selectedWalletId.isNotEmpty()) {
                viewModel.getWalletAddress()
            } else {
                NCToastMessage(requireActivity()).showWarning(getString(R.string.nc_select_wallet_first))
            }
        }
    }

    private fun initViews() {
        binding.recyclerView.adapter = adapter
    }

    private fun observer() {
        flowObserver(viewModel.event, ::handleEvent)
        flowObserver(viewModel.state, ::handleState)
    }

    private fun handleEvent(event: SelectWalletEvent) {
        when (event) {
            is SelectWalletEvent.Error -> showError(event.e?.message.orUnknownError())
            is SelectWalletEvent.Loading -> showOrHideLoading(
                event.isLoading,
                title = getString(R.string.nc_sweeping_is_progress),
                message = getString(R.string.nc_make_sure_internet)
            )
            is SelectWalletEvent.GetAddressSuccess -> navigateToEstimateFee(event.address)
        }
    }

    private fun navigateToEstimateFee(address: String) {
        val totalBalance = args.slots.sumOf { it.balance.value }
        val totalInBtc = Amount(value = totalBalance).pureBTC()
        val type = if (args.type == TYPE_UNSEAL_SWEEP_ACTIVE_SLOT) {
            SweepType.UNSEAL_SWEEP_TO_NUNCHUK_WALLET
        } else {
            SweepType.SWEEP_TO_NUNCHUK_WALLET
        }
        navigator.openEstimatedFeeScreen(
            activityContext = requireActivity(),
            walletId = viewModel.selectedWalletId,
            outputAmount = totalInBtc,
            availableAmount = totalInBtc,
            address = address,
            "",
            subtractFeeFromAmount = true,
            sweepType = type,
            slots = args.slots.toList()
        )
    }

    private fun handleState(state: SelectWalletState) {
        adapter.submitList(state.selectWallets)
    }

    companion object {
        const val TYPE_UNSEAL_SWEEP_ACTIVE_SLOT = 1
        const val TYPE_SWEEP_UNSEAL_SLOT = 2
    }
}