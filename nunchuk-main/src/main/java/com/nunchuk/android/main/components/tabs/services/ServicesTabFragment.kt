package com.nunchuk.android.main.components.tabs.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import com.nunchuk.android.widget.NCInfoDialog
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
    }

    private fun setupViews() {
        adapter = ServicesTabAdapter {
            onTabItemClick(it)
        }
        binding.recyclerView.adapter = adapter
        adapter.submitList(viewModel.getItems())
    }

    private fun onTabItemClick(item: ServiceTabRowItem) {
        when (item) {
            ServiceTabRowItem.ClaimInheritance -> {}
            ServiceTabRowItem.CoSigningPolicies -> {}
            ServiceTabRowItem.EmergencyLockdown -> navigator.openEmergencyLockdownScreen(
                requireContext()
            )
            ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(requireContext())
            ServiceTabRowItem.ManageSubscription -> {
                showManageSubscriptionDialog()
            }
            ServiceTabRowItem.OrderNewHardware -> {
                showOrderNewHardwareDialog()
            }
            ServiceTabRowItem.RollOverAssistedWallet -> {}
            ServiceTabRowItem.SetUpInheritancePlan -> navigator.openInheritancePlanningScreen(
                requireContext()
            )
        }
    }

    private fun showManageSubscriptionDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_manage_subscription_desc),
            onInfoClick = {

            })
    }

    private fun showOrderNewHardwareDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_order_new_hardware_desc),
            onInfoClick = {

            })
    }
}