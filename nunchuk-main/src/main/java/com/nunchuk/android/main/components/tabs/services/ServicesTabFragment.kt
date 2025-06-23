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
import com.nunchuk.android.core.constants.Constants
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.core.wallet.AssistedWalletBottomSheet
import com.nunchuk.android.main.BuildConfig
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsViewModel
import com.nunchuk.android.main.databinding.FragmentServicesTabBinding
import com.nunchuk.android.main.nonsubscriber.NonSubscriberActivity
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.isByzantineOrFinney
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.consumeEdgeToEdge
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
    private val walletsViewModel: WalletsViewModel by activityViewModels()

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

        childFragmentManager.setFragmentResultListener(
            AssistedWalletBottomSheet.TAG,
            viewLifecycleOwner
        ) { _, bundle ->
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
                    activityContext = requireContext(),
                    verifyToken = event.token,
                    inheritance = event.inheritance,
                    flowInfo = InheritancePlanFlow.VIEW,
                    groupId = event.groupId,
                    sourceFlow = InheritanceSourceFlow.SERVICE_TAB
                )

                is ServicesTabEvent.OpenSetupInheritancePlan -> navigator.openInheritancePlanningScreen(
                    walletId = event.walletId,
                    activityContext = requireContext(),
                    flowInfo = InheritancePlanFlow.SETUP,
                    groupId = event.groupId,
                    sourceFlow = InheritanceSourceFlow.SERVICE_TAB
                )

                is ServicesTabEvent.CalculateRequiredSignaturesSuccess -> {
                    if (event.type == "NONE") {
                        navigator.openInheritancePlanningScreen(
                            walletId = event.walletId,
                            activityContext = requireContext(),
                            flowInfo = InheritancePlanFlow.REQUEST,
                            groupId = event.groupId,
                            sourceFlow = InheritanceSourceFlow.GROUP_DASHBOARD,
                        )
                    }
                }

                is ServicesTabEvent.RowItems -> {
                    adapter.submitList(event.items)
                    viewModel.isPremiumUser()?.let { it ->
                        binding.supportFab.isVisible = it || viewModel.isByzantine()
                        binding.actionGroup.isVisible =
                            it.not() && event.items.any { it is NonSubHeader }
                        binding.claimLayout.isVisible =
                            viewModel.isShowClaimInheritanceLayout() && event.items.none { it is ServiceTabRowItem.ClaimInheritance }
                    }
                }
            }
        }
        flowObserver(viewModel.state) {
            viewModel.getRowItems()

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentSelectedItem?.let { outState.putParcelable(EXTRA_SELECTED_ITEM, it) }
    }

    private fun handleGoOurWebsite() {
        requireActivity().openExternalLink("https://nunchuk.io/individuals")
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
                navigator.openEmergencyLockdownScreen(
                    requireContext(),
                    event.token,
                    event.groupId,
                    event.walletId
                )
            }

            ServiceTabRowItem.ViewInheritancePlan -> viewModel.getInheritance(
                event.walletId,
                event.token,
                event.groupId
            )

            ServiceTabRowItem.ReplaceKey -> navigator.openMembershipActivity(
                activityContext = requireActivity(),
                groupStep = MembershipStage.REPLACE_KEY,
                walletId = event.walletId,
                groupId = event.groupId,
            )

            else -> Unit
        }
    }

    private fun setupViews() {
        binding.toolbar.consumeEdgeToEdge()
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
                val (numOfLockedWallet, wallets) = viewModel.getAllowEmergencyLockdownWallets()
                if (numOfLockedWallet == wallets.size) {
                    NCInfoDialog(requireActivity()).showDialog(
                        message = getString(
                            R.string.nc_all_wallets_under_lockdown
                        )
                    )
                    return
                }
                showWalletsSheetOrEnterPassword(item, wallets)
            }

            ServiceTabRowItem.KeyRecovery -> navigator.openKeyRecoveryScreen(
                requireContext(),
                viewModel.state.value.userRole
            )

            ServiceTabRowItem.ManageSubscription -> showManageSubscriptionDialog()
            ServiceTabRowItem.OrderNewHardware -> showOrderNewHardwareDialog()
            ServiceTabRowItem.SetUpInheritancePlan -> {
                val wallets = viewModel.getUnSetupInheritanceWallets()
                if (wallets.isEmpty()) return
                if (wallets.size == 1) {
                    viewModel.openSetupInheritancePlan(wallets.first().localId)
                } else {
                    AssistedWalletBottomSheet.show(
                        childFragmentManager,
                        assistedWalletIds = wallets.map { it.localId },
                        lockdownWalletIds = viewModel.getLockdownWalletsIds()
                    )
                }
            }

            ServiceTabRowItem.CoSigningPolicies -> showWalletsSheetOrEnterPassword(
                item,
                viewModel.getConfigServerKeyWallets()
            )

            ServiceTabRowItem.ViewInheritancePlan -> showWalletsSheetOrEnterPassword(
                item,
                viewModel.getViewClaimInheritanceWallets()
            )

            ServiceTabRowItem.GetAdditionalWallets -> {}

            ServiceTabRowItem.ReplaceKey -> showWalletsSheetOrEnterPassword(
                item,
                viewModel.getActiveWalletsAndNoReplaced()
            )
        }
    }

    private fun showWalletsSheetOrEnterPassword(
        item: ServiceTabRowItem,
        wallets: List<AssistedWalletBrief>
    ) {
        if (wallets.isEmpty()) return
        if (wallets.size == 1) {
            enterPasswordDialog(item = item, walletId = wallets.first().localId)
        } else {
            AssistedWalletBottomSheet.show(
                childFragmentManager,
                assistedWalletIds = wallets.map { it.localId },
                lockdownWalletIds = viewModel.getLockdownWalletsIds()
            )
        }
    }

    private fun isCheckWalletCreationState(item: ServiceTabRowItem): Boolean {
        return item is ServiceTabRowItem.CoSigningPolicies ||
                item is ServiceTabRowItem.EmergencyLockdown ||
                item is ServiceTabRowItem.ReplaceKey ||
                item is ServiceTabRowItem.KeyRecovery ||
                item is ServiceTabRowItem.SetUpInheritancePlan
    }

    private fun showUnPaid() {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_unpaid_security_deposit),
            btnYes = getString(R.string.nc_take_me_there),
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                requireActivity().openExternalLink(Constants.CLAIM_URL)
            }
        )
    }

    private fun showFeatureAssistedWalletInformDialog(textAction: String) {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_feature_assisted_wallet_inform_desc),
            btnYes = textAction,
            btnInfo = getString(R.string.nc_text_got_it),
            onYesClick = {
                val personalSteps = walletsViewModel.getPersonalSteps()
                val plans = walletsViewModel.getPlans().orEmpty()
                val walletType = when {
                    personalSteps.any { it.plan == MembershipPlan.IRON_HAND } -> GroupWalletType.TWO_OF_THREE_PLATFORM_KEY
                    personalSteps.any { it.plan == MembershipPlan.HONEY_BADGER } -> GroupWalletType.TWO_OF_FOUR_MULTISIG
                    else -> null
                }
                val isPersonalWallet = walletType != null || plans.none { it.isByzantineOrFinney() }
                navigator.openMembershipActivity(
                    activityContext = requireActivity(),
                    groupStep = walletsViewModel.getGroupStage(),
                    walletId = walletsViewModel.getAssistedWalletId(),
                    isPersonalWallet = isPersonalWallet,
                    walletType = walletType,
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
                val link =
                    if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/my-plan" else "https://www.nunchuk.io/my-plan"
                requireActivity().openExternalLink(link)
            })
    }

    private fun showOrderNewHardwareDialog() {
        NCInfoDialog(requireActivity()).showDialog(
            btnInfo = getString(R.string.nc_take_me_to_the_website),
            message = getString(R.string.nc_order_new_hardware_desc),
            onInfoClick = {
                val link =
                    if (BuildConfig.DEBUG) "https://stg-www.nunchuk.io/hardware-replacement" else "https://www.nunchuk.io/hardware-replacement"
                requireActivity().openExternalLink(link)
            })
    }

    companion object {
        private const val EXTRA_SELECTED_ITEM = "selected_item"
    }
}