package com.nunchuk.android.main.components.tabs.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServicesTabFragment : BaseFragment<FragmentServicesTabBinding>() {

    private val viewModel: ServicesTabViewModel by viewModels()
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
                is ServicesTabEvent.CreateSupportRoomSuccess -> navigator.openRoomDetailActivity(
                    requireContext(),
                    event.roomId
                )
                is ServicesTabEvent.LoadingEvent -> showOrHideLoading(event.isLoading)
            }
        }
        flowObserver(viewModel.state) { state ->
            adapter.submitList(state.rowItems)
            state.isPremiumUser?.let {
                binding.supportFab.isVisible = state.isPremiumUser
                binding.actionGroup.isVisible = state.isPremiumUser.not()
            }
        }
    }

    private fun handleCheckPasswordSuccess(event: ServicesTabEvent.CheckPasswordSuccess) {
        when (event.item) {
            ServiceTabRowItem.CoSigningPolicies -> {
                viewModel.getServiceKey(event.token)
            }
            ServiceTabRowItem.EmergencyLockdown -> {
                navigator.openEmergencyLockdownScreen(requireContext(), event.token)
            }
            ServiceTabRowItem.ViewInheritancePlan -> {
                navigator.openInheritancePlanningScreen(
                    requireContext(),
                    verifyToken = event.token,
                    inheritance = viewModel.getInheritance(),
                    flowInfo = InheritancePlanFlow.VIEW
                )
            }
            else -> {}
        }
    }

    private fun setupViews() {
        adapter = ServicesTabAdapter {
            onTabItemClick(it)
        }
        binding.recyclerView.adapter = adapter
        binding.supportFab.setOnDebounceClickListener {
            viewModel.getOrCreateSupportRom()
        }
    }

    private fun onTabItemClick(item: ServiceTabRowItem) {
        if (item is ServiceTabRowItem.CoSigningPolicies ||
            item is ServiceTabRowItem.EmergencyLockdown ||
            item is ServiceTabRowItem.RollOverAssistedWallet ||
            item is ServiceTabRowItem.KeyRecovery
        ) {
            val textAction =
                if (viewModel.getGroupStage() == MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS) {
                    getString(R.string.nc_continue_setting_up_wallet)
                } else if (viewModel.getGroupStage() == MembershipStage.NONE) {
                    getString(R.string.nc_start_wizard)
                } else {
                    ""
                }
            if (textAction.isNotBlank()) {
                showFeatureAssistedWalletInformDialog(textAction)
                return
            }
        }
        when (item) {
            ServiceTabRowItem.ClaimInheritance -> {
                navigator.openInheritancePlanningScreen(
                    requireContext(),
                    flowInfo = InheritancePlanFlow.CLAIM
                )
            }
            ServiceTabRowItem.CoSigningPolicies, ServiceTabRowItem.EmergencyLockdown -> enterPasswordDialog(
                item
            )
            ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(requireContext())
            ServiceTabRowItem.ManageSubscription -> showManageSubscriptionDialog()
            ServiceTabRowItem.OrderNewHardware -> showOrderNewHardwareDialog()
            ServiceTabRowItem.RollOverAssistedWallet -> {}
            ServiceTabRowItem.SetUpInheritancePlan -> {
                navigator.openInheritancePlanningScreen(
                    requireContext(),
                    flowInfo = InheritancePlanFlow.SETUP
                )
            }
            ServiceTabRowItem.ViewInheritancePlan -> enterPasswordDialog(item)
        }
    }

    private fun showFeatureAssistedWalletInformDialog(textAction: String) {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_feature_assisted_wallet_inform_desc),
            btnYes = textAction,
            btnInfo = getString(R.string.nc_text_got_it),
            onInfoClick = {

            },
            onYesClick = {

            }
        )
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