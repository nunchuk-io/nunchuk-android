package com.nunchuk.android.signer.satscard.wallets

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentSelectWalletSweepBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SelectWalletFragment : BaseFragment<FragmentSelectWalletSweepBinding>() {
    private val viewModel by viewModels<SelectWalletViewModel>()
    private val nfcViewModel by activityViewModels<NfcViewModel>()
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

        binding.btnContinue.setOnClickListener {
            if (viewModel.selectedWalletId.isNotEmpty()) {
                (activity as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_SATSCARD_SWEEP_SLOT)
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
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_SATSCARD_SWEEP_SLOT }) {
            viewModel.handleSweepBalance(IsoDep.get(it.tag), nfcViewModel.inputCvc.orEmpty(), args.slots.toList(), args.type)
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleEvent(event: SelectWalletEvent) {
        when (event) {
            is SelectWalletEvent.ShowError -> showError(event.message)
            is SelectWalletEvent.Loading -> showOrHideLoading(event.isLoading)
            is SelectWalletEvent.NfcLoading -> showOrHideLoading(event.isLoading, getString(R.string.nc_keep_holding_nfc))
            SelectWalletEvent.SweepSuccess -> navigator.openWalletDetailsScreen(requireActivity(), viewModel.selectedWalletId)
        }
    }

    private fun handleState(state: SelectWalletState) {
        adapter.submitList(state.selectWallets)
    }

    companion object {
        const val TYPE_UNSEAL_SWEEP_ACTIVE_SLOT = 1
        const val TYPE_SWEEP_UNSEAL_SLOT = 2
    }
}