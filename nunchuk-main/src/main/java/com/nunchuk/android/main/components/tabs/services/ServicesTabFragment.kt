package com.nunchuk.android.main.components.tabs.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServicesTabFragment : BaseFragment<FragmentServicesTabBinding>() {

    private val viewModel: ServicesTabViewModel by activityViewModels()
    private lateinit var adapter: ServicesTabAdapter
    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentServicesTabBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupData()
        observeEvent()
    }

    private fun setupData() {

    }

    private fun setupViews() {
        adapter = ServicesTabAdapter {
            viewModel.onItemClick(it)
        }
        binding.recyclerView.adapter = adapter
        adapter.submitList(viewModel.getItems())
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleEvent(event: ServicesTabEvent) {
        when (event) {
            is ServicesTabEvent.ItemClick -> when (event.item) {
                ServiceTabRowItem.ClaimInheritance -> TODO()
                ServiceTabRowItem.CoSigningPolicies -> TODO()
                ServiceTabRowItem.EmergencyLockdown -> navigator.openEmergencyLockdownScreen(requireContext())
                ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(requireContext())
                ServiceTabRowItem.ManageSubscription -> TODO()
                ServiceTabRowItem.OrderNewHardware -> TODO()
                ServiceTabRowItem.RollOverAssistedWallet -> TODO()
                ServiceTabRowItem.SetUpInheritancePlan -> navigator.openInheritancePlanningScreen(requireContext())
            }
            is ServicesTabEvent.Loading -> TODO()
        }
    }
}