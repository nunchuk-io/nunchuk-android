package com.nunchuk.android.main.components.tabs.services

import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.keyrecovery.KeyRecoveryActionItem
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import com.nunchuk.android.wallet.components.config.WalletConfigEvent
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
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

        flowObserver(viewModel.event) { event ->
            when (event) {
                is ServicesTabEvent.GetServerKeySuccess -> openServerKeyDetail(event)
                is ServicesTabEvent.ProcessFailure -> showError(message = event.message)
                is ServicesTabEvent.Loading -> showOrHideLoading(loading = event.loading)
                is ServicesTabEvent.CheckPasswordSuccess -> handleCheckPasswordSuccess(event)
            }
        }
    }

    private fun handleCheckPasswordSuccess(event: ServicesTabEvent.CheckPasswordSuccess) {
        when(event.item) {
            ServiceTabRowItem.CoSigningPolicies -> {
                viewModel.getServiceKey(event.token)
            }
            ServiceTabRowItem.EmergencyLockdown -> {
                navigator.openEmergencyLockdownScreen(requireContext())
            }
            else -> {}
        }
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
            ServiceTabRowItem.ClaimInheritance -> {
                navigator.openInheritancePlanningScreen(
                    requireContext(),
                    InheritancePlanFlow.VIEW
                )
            }
            ServiceTabRowItem.CoSigningPolicies, ServiceTabRowItem.EmergencyLockdown -> enterPasswordDialog(item)
            ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(requireContext())
            ServiceTabRowItem.ManageSubscription -> showManageSubscriptionDialog()
            ServiceTabRowItem.OrderNewHardware -> showOrderNewHardwareDialog()
            ServiceTabRowItem.RollOverAssistedWallet -> {}
            ServiceTabRowItem.SetUpInheritancePlan -> {
                navigator.openInheritancePlanningScreen(
                    requireContext(),
                    InheritancePlanFlow.SETUP
                )
            }
        }
    }

    private fun enterPasswordDialog(item: ServiceTabRowItem) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                viewModel.confirmPassword(it, item)
            }
        )
    }

    private fun openServerKeyDetail(event: ServicesTabEvent.GetServerKeySuccess) {
        CosigningPolicyActivity.start(
            activity = requireActivity(),
            keyPolicy = null,
            xfp = event.signer.masterFingerprint,
            token = event.token,
            walletId = event.walletId,
        )
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