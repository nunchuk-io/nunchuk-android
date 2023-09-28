/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.AssistedWalletBottomSheet
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import com.nunchuk.android.main.nonsubscriber.NonSubscriberActivity
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.isByzantine
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.components.cosigning.CosigningPolicyActivity
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCVerticalInputDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServicesTabFragment : BaseFragment<FragmentServicesTabBinding>() {

    private val viewModel: ServicesTabViewModel by activityViewModels()
    private lateinit var adapter: ServicesTabAdapter
    private var currentSelectedItem: ServiceTabRowItem? = null

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentServicesTabBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { currentSelectedItem = it.parcelable(EXTRA_SELECTED_ITEM) }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        childFragmentManager.setFragmentResultListener(AssistedWalletBottomSheet.TAG, viewLifecycleOwner) { _, bundle ->
            val walletId = bundle.getString(GlobalResultKey.WALLET_ID).orEmpty()
            val item = currentSelectedItem
            if (item == ServiceTabRowItem.SetUpInheritancePlan) {
                viewModel.openSetupInheritancePlan(walletId)
            } else {
                if (walletId.isNotEmpty() && item != null) {
                    enterPasswordDialog(item, walletId)
                }
            }
        }

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
                is ServicesTabEvent.CheckInheritance -> {
                    if (event.inheritanceCheck.isPaid) {
                        navigator.openInheritancePlanningScreen(
                            activityContext = requireContext(),
                            flowInfo = InheritancePlanFlow.CLAIM
                        )
                    } else {
                        showUnPaid()
                    }
                }
                is ServicesTabEvent.EmailInvalid -> showError(getString(R.string.nc_text_email_invalid))
                is ServicesTabEvent.OnSubmitEmailSuccess -> showSuccess(
                    message = getString(
                        R.string.nc_we_sent_an_email,
                        event.email
                    )
                )
                is ServicesTabEvent.GetInheritanceSuccess -> navigator.openInheritancePlanningScreen(
                    walletId = event.walletId,
                    requireContext(),
                    verifyToken = event.token,
                    inheritance = event.inheritance,
                    flowInfo = InheritancePlanFlow.VIEW,
                    groupId = event.groupId
                )

                is ServicesTabEvent.OpenSetupInheritancePlan -> navigator.openInheritancePlanningScreen(
                    walletId = event.walletId,
                    activityContext = requireContext(),
                    flowInfo = InheritancePlanFlow.SETUP,
                    groupId = event.groupId
                )
            }
        }
        flowObserver(viewModel.state) { state ->
            adapter.submitList(viewModel.getRowItems())
            state.isPremiumUser?.let {
                binding.supportFab.isVisible = state.isPremiumUser
                binding.actionGroup.isVisible = state.isPremiumUser.not()
                binding.claimLayout.isVisible = state.isPremiumUser.not()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentSelectedItem?.let { outState.putParcelable(EXTRA_SELECTED_ITEM, it) }
    }

    private fun handleGoOurWebsite() {
        requireActivity().openExternalLink("https://nunchuk.io")
    }

    private fun showTellMeMoreDialog() {
        NCVerticalInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_email),
            positiveText = getString(R.string.nc_send_me_the_info),
            negativeText = getString(R.string.nc_visit_our_website),
            neutralText = getString(R.string.nc_text_do_this_later),
            defaultInput = viewModel.getEmail(),
            cancellable = true,
            onPositiveClicked = {
                viewModel.submitEmail(it)
            },
            onNegativeClicked = ::handleGoOurWebsite
        )
    }

    private fun handleCheckPasswordSuccess(event: ServicesTabEvent.CheckPasswordSuccess) {
        when (event.item) {
            ServiceTabRowItem.CoSigningPolicies -> {
                viewModel.getServiceKey(event.token, event.walletId)
            }
            ServiceTabRowItem.EmergencyLockdown -> {
                navigator.openEmergencyLockdownScreen(requireContext(), event.token, event.groupId, event.walletId)
            }
            ServiceTabRowItem.ViewInheritancePlan -> viewModel.getInheritance(
                event.walletId,
                event.token,
                event.groupId
            )
            else -> Unit
        }
    }

    private fun setupViews() {
        adapter = ServicesTabAdapter(itemClick = {
            onTabItemClick(it)
        }, bannerClick = {
            NonSubscriberActivity.start(requireActivity(), it)
        })
        binding.recyclerView.adapter = adapter
        binding.supportFab.setOnDebounceClickListener {
            viewModel.getOrCreateSupportRom()
        }
        binding.claimLayout.setOnDebounceClickListener {
            if (viewModel.isLoggedIn()) {
                viewModel.checkInheritance()
            } else {
                navigator.openSignInScreen(requireActivity(), isNeedNewTask = false)
            }
        }
        binding.btnTellMore.setOnDebounceClickListener {
            showTellMeMoreDialog()
        }
        binding.btnVisitWebsite.setOnDebounceClickListener {
            handleGoOurWebsite()
        }
    }

    private fun onTabItemClick(item: ServiceTabRowItem) {
        currentSelectedItem = item
        if (isCheckWalletCreationState(item)) {
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
            ServiceTabRowItem.ClaimInheritance -> viewModel.checkInheritance()
            ServiceTabRowItem.EmergencyLockdown -> {
                if (viewModel.state.value.plan.isByzantine()) {
                    val wallets = viewModel.getAllowEmergencyLockdownWallets()
                    if (wallets.isEmpty()) return
                    if (wallets.size == 1) {
                        enterPasswordDialog(item = item, walletId = wallets.first().localId)
                    } else {
                        AssistedWalletBottomSheet.show(childFragmentManager, wallets.map { it.localId })
                    }
                } else {
                    enterPasswordDialog(item)
                }
            }
            ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(requireContext(), viewModel.state.value.userRole)
            ServiceTabRowItem.ManageSubscription -> showManageSubscriptionDialog()
            ServiceTabRowItem.OrderNewHardware -> showOrderNewHardwareDialog()
            ServiceTabRowItem.RollOverAssistedWallet -> {}
            ServiceTabRowItem.SetUpInheritancePlan -> {
                val wallets = viewModel.getUnSetupInheritanceWallets()
                if (wallets.size == 1) {
                    navigator.openInheritancePlanningScreen(
                        walletId = wallets.first().localId,
                        activityContext = requireContext(),
                        flowInfo = InheritancePlanFlow.SETUP,
                        groupId = wallets.first().groupId
                    )
                } else {
                    AssistedWalletBottomSheet.show(childFragmentManager, wallets.map { it.localId })
                }
            }
            ServiceTabRowItem.CoSigningPolicies,
            ServiceTabRowItem.ViewInheritancePlan -> {
                val wallets = viewModel.getWallets(ignoreSetupInheritance = item != ServiceTabRowItem.ViewInheritancePlan)
                if (wallets.isEmpty()) return
                if (wallets.size == 1) {
                    enterPasswordDialog(item = item, walletId = wallets.first().localId)
                } else {
                    AssistedWalletBottomSheet.show(childFragmentManager, wallets.map { it.localId })
                }
            }

            ServiceTabRowItem.GetAdditionalWallets -> {}
        }
    }

    private fun isCheckWalletCreationState(item: ServiceTabRowItem): Boolean {
        return item is ServiceTabRowItem.CoSigningPolicies ||
                item is ServiceTabRowItem.EmergencyLockdown ||
                item is ServiceTabRowItem.RollOverAssistedWallet ||
                item is ServiceTabRowItem.KeyRecovery ||
                item is ServiceTabRowItem.SetUpInheritancePlan
    }

    private fun showUnPaid() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_unpaid_security_deposit),
            btnYes = getString(R.string.nc_take_me_there),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                val link = if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/claim" else "https://www.nunchuk.io/claim"
                requireActivity().openExternalLink(link)
            }
        )
    }

    private fun showFeatureAssistedWalletInformDialog(textAction: String) {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_feature_assisted_wallet_inform_desc),
            btnYes = textAction,
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                navigator.openMembershipActivity(
                    requireActivity(),
                    viewModel.getGroupStage()
                )
            }
        )
    }

    private fun enterPasswordDialog(item: ServiceTabRowItem, walletId: String = "") {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                viewModel.confirmPassword(walletId, it, item)
            }
        )
    }

    private fun openServerKeyDetail(event: ServicesTabEvent.GetServerKeySuccess) {
        val groupId = viewModel.getGroupId(event.walletId)
        if (!groupId.isNullOrEmpty()) {
            CosigningPolicyActivity.start(
                activity = requireActivity(),
                signer = event.signer.toModel(),
                token = event.token,
                walletId = event.walletId,
                groupId = groupId,
            )
        } else {
            CosigningPolicyActivity.start(
                activity = requireActivity(),
                keyPolicy = null,
                signer = event.signer.toModel(),
                token = event.token,
                walletId = event.walletId,
            )
        }
    }

    private fun showManageSubscriptionDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_manage_subscription_desc),
            onInfoClick = {
                val link = if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/my-plan" else "https://www.nunchuk.io/my-plan"
                requireActivity().openExternalLink(link)
            })
    }

    private fun showOrderNewHardwareDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_order_new_hardware_desc),
            onInfoClick = {
                val link = if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/hardware-replacement" else "https://www.nunchuk.io/hardware-replacement"
                requireActivity().openExternalLink(link)
            })
    }

    companion object {
        private const val EXTRA_SELECTED_ITEM = "selected_item"
    }
}