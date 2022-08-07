package com.nunchuk.android.signer.satscard.unseal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.SatscardNavigationDirections
import com.nunchuk.android.signer.databinding.FragmentUnsealSlotBinding
import com.nunchuk.android.signer.satscard.SatsCardSlotEvent
import com.nunchuk.android.signer.satscard.SatsCardSlotViewModel
import com.nunchuk.android.signer.satscard.unSealBalanceSlots
import com.nunchuk.android.signer.satscard.wallets.SelectWalletFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SatsCardUnsealSlotFragment : BaseFragment<FragmentUnsealSlotBinding>() {
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SatsCardUnsealSlotAdapter() }
    private val viewModel by activityViewModels<SatsCardSlotViewModel>()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUnsealSlotBinding {
        return FragmentUnsealSlotBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
        observer()
    }

    private fun observer() {
        flowObserver(viewModel.state) {
            val unsealSlots = viewModel.getUnsealSlots()
            adapter.submitList(unsealSlots)
            binding.btnSweep.isVisible = unsealSlots.any { it.isConfirmed && it.balance.value > 0L }
        }
        flowObserver(viewModel.event) {
            showOrHideLoading(it is SatsCardSlotEvent.Loading)
        }
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.recyclerView.adapter = adapter
        binding.btnSweep.setOnClickListener {
            val action = SatscardNavigationDirections.toSelectWalletFragment(
                viewModel.getUnsealSlots().unSealBalanceSlots().toTypedArray(),
                SelectWalletFragment.TYPE_SWEEP_UNSEAL_SLOT
            )
            findNavController().navigate(action)
        }
    }
}