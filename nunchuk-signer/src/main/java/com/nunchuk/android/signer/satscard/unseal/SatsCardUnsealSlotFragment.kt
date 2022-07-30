package com.nunchuk.android.signer.satscard.unseal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.SatscardNavigationDirections
import com.nunchuk.android.signer.databinding.FragmentUnsealSlotBinding
import com.nunchuk.android.signer.satscard.unSealBalanceSlots
import com.nunchuk.android.signer.satscard.wallets.SelectWalletFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SatsCardUnsealSlotFragment : BaseFragment<FragmentUnsealSlotBinding>() {
    private val args : SatsCardUnsealSlotFragmentArgs by navArgs()
    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SatsCardUnsealSlotAdapter(args.slots.toList()) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentUnsealSlotBinding {
        return FragmentUnsealSlotBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.recyclerView.adapter = adapter
        binding.btnSweep.isVisible = args.slots.any { it.isConfirmed && it.balance.value > 0L }
        binding.btnSweep.setOnClickListener {
            val action = SatscardNavigationDirections.toSelectWalletFragment(args.slots.unSealBalanceSlots(), SelectWalletFragment.TYPE_SWEEP_UNSEAL_SLOT)
            findNavController().navigate(action)
        }
    }
}