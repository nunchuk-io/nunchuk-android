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

package com.nunchuk.android.main.components.tabs.wallet

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.contact.components.contacts.ContactsViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.referral.ReferralArgs
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.BLOCKCHAIN_STATUS
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.formatMMMddyyyyDate
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.openExternalLink
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.MainActivityViewModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.AddWalletEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GetTapSignerStatusSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GoToSatsCardScreen
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.Loading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NeedSetupSatsCard
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NfcLoading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.None
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.SatsCardUsedUp
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowErrorEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.WalletEmptySignerEvent
import com.nunchuk.android.main.components.tabs.wallet.emptystate.WalletEmptyStateView
import com.nunchuk.android.main.components.tabs.wallet.totalbalance.TotalBalanceScrollHandler
import com.nunchuk.android.main.components.tabs.wallet.totalbalance.TotalBalanceView
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.SyncCompleted
import com.nunchuk.android.main.groupwallet.FreeGroupWalletActivity
import com.nunchuk.android.main.intro.UniversalNfcIntroActivity
import com.nunchuk.android.main.membership.byzantine.views.PendingWalletView
import com.nunchuk.android.main.nonsubscriber.NonSubscriberActivity
import com.nunchuk.android.messages.components.freegroup.FreeGroupWalletChatActivity
import com.nunchuk.android.messages.components.list.RoomMessage
import com.nunchuk.android.messages.components.list.RoomsViewModel
import com.nunchuk.android.messages.util.SUBSCRIPTION_SUBSCRIPTION_ACTIVE
import com.nunchuk.android.messages.util.SUBSCRIPTION_SUBSCRIPTION_PENDING
import com.nunchuk.android.messages.util.getMsgType
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.campaigns.Campaign
import com.nunchuk.android.model.campaigns.CampaignStatus
import com.nunchuk.android.model.campaigns.CampaignType
import com.nunchuk.android.model.campaigns.ReferrerCode
import com.nunchuk.android.model.isByzantineOrFinney
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.signer.satscard.SatsCardActivity
import com.nunchuk.android.signer.signer.SignersViewModel
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.util.handleTapSignerStatus
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.utils.consumeEdgeToEdge
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
internal class WalletsFragment : BaseFragment<FragmentWalletsBinding>() {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val walletsViewModel: WalletsViewModel by activityViewModels()

    private val roomViewModel: RoomsViewModel by activityViewModels()

    private val contactViewModel: ContactsViewModel by activityViewModels()

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private val signersViewModel: SignersViewModel by activityViewModels()

    private val nfcViewModel: NfcViewModel by activityViewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_AUTO_CARD_STATUS)
            }
        }

    private var existingKeyDialog: Dialog? = null

    private val totalBalanceScrollHandler: TotalBalanceScrollHandler by lazy {
        TotalBalanceScrollHandler(binding.totalBalanceFrame)
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ) = FragmentWalletsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        observeEvent()
    }

    private fun setupViews() {
        binding.toolbar.consumeEdgeToEdge()
        binding.ivAddWallet.setOnClickListener { walletsViewModel.handleAddWallet() }
        binding.ivNfc.setOnClickListener {
            if (walletsViewModel.isShownNfcUniversal.value) {
                UniversalNfcIntroActivity.navigate(launcher, requireActivity())
            } else {
                (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_AUTO_CARD_STATUS)
            }
        }
        binding.introContainer.setOnDebounceClickListener {
            val stage = walletsViewModel.getGroupStage()
            if (stage == MembershipStage.SETUP_INHERITANCE) {
                navigator.openInheritancePlanningScreen(
                    walletId = walletsViewModel.getAssistedWalletId().orEmpty(),
                    activityContext = requireContext(),
                    flowInfo = InheritancePlanFlow.SETUP,
                )
            } else {
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
                    groupStep = stage,
                    walletId = walletsViewModel.getAssistedWalletId(),
                    isPersonalWallet = isPersonalWallet,
                    walletType = walletType
                )
            }
        }
        binding.containerNonSubscriber.setOnDebounceClickListener {
            val banner = walletsViewModel.getBanner() ?: return@setOnDebounceClickListener
            if (banner.type == Banner.Type.TYPE_REFERRAL_01) {
                requireActivity().openExternalLink(banner.content.action.target)
            } else {
                NonSubscriberActivity.start(requireActivity(), it.tag as String)
            }
        }
        binding.llCampaigns.setOnDebounceClickListener {
            walletsViewModel.getCurrentCampaign()?.let {
                navigator.openReferralScreen(
                    activityContext = requireActivity(),
                    args = ReferralArgs(
                        campaign = it,
                        localReferrerCode = walletsViewModel.getLocalReferrerCode()
                    )
                )
            }
        }
        binding.content.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            totalBalanceScrollHandler.handleScrollChange(scrollY, oldScrollY)
        })
    }

    private fun openAddWalletScreen() {
        navigator.openWalletIntermediaryScreen(requireActivity(), signersViewModel.hasSigner())
    }

    private fun showAssistedWalletStart(
        remainingTime: Int,
        walletName: String?,
    ) {
        val stage = walletsViewModel.getGroupStage()
        if (binding.introContainer.isVisible.not()) return
        if (stage == MembershipStage.NONE) {
            binding.tvIntroTitle.text = getString(R.string.nc_let_s_get_you_started)
            binding.tvIntroDesc.text =
                getString(R.string.nc_assisted_wallet_intro_desc)
            binding.tvIntroAction.text = getString(R.string.nc_start_wizard)
        } else if (stage == MembershipStage.SETUP_INHERITANCE) {
            binding.tvIntroTitle.text =
                getString(R.string.nc_setup_inheritance_for, walletName.orEmpty())
            binding.tvIntroDesc.text =
                getString(R.string.nc_estimate_remain_time, 21)
            binding.tvIntroAction.text = getString(R.string.nc_do_it_now)
        } else if (stage != MembershipStage.DONE) {
            binding.tvIntroTitle.text = getString(R.string.nc_you_almost_done)
            binding.tvIntroDesc.text =
                getString(R.string.nc_estimate_remain_time, remainingTime)
            binding.tvIntroAction.text =
                getString(R.string.nc_continue_setting_your_wallet)
        }
    }

    private fun observeEvent() {
        walletsViewModel.state.observe(viewLifecycleOwner, ::showWalletState)
        walletsViewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        mainActivityViewModel.event.observe(viewLifecycleOwner, ::handleMainActivityEvent)
        roomViewModel.state.observe(viewLifecycleOwner) {
            if (walletsViewModel.isPremiumUser().not()) {
                it.rooms.filterIsInstance<RoomMessage.MatrixRoom>().map { it.data }.forEach { room ->
                    room.latestPreviewableEvent?.takeIf { event ->
                        event.getMsgType() == SUBSCRIPTION_SUBSCRIPTION_PENDING
                                || event.getMsgType() == SUBSCRIPTION_SUBSCRIPTION_ACTIVE
                    }?.let {
                        walletsViewModel.reloadMembership()
                    }
                }
            }
        }
        if (walletsViewModel.isPremiumUser().not()) {
            flowObserver(contactViewModel.noticeRoomEvent()) {
                it.forEach { event ->
                    if ((event.getMsgType() == SUBSCRIPTION_SUBSCRIPTION_PENDING || event.getMsgType() == SUBSCRIPTION_SUBSCRIPTION_ACTIVE)
                        && walletsViewModel.isPremiumUser().not()
                    ) {
                        walletsViewModel.reloadMembership()
                    }
                }
            }
        }
        flowObserver(
            nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_AUTO_CARD_STATUS }) {
            if (it.tag.techList.contains(IsoDep::class.java.name)) {
                walletsViewModel.getSatsCardStatus(IsoDep.get(it.tag))
            } else {
                navigator.openPortalScreen(
                    activity = requireActivity(),
                    args = PortalDeviceArgs(
                        PortalDeviceFlow.SETUP
                    )
                )
            }

            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleEvent(event: WalletsEvent) {
        when (event) {
            AddWalletEvent -> openAddWalletScreen()
            WalletEmptySignerEvent -> openWalletIntroScreen()
            is ShowErrorEvent -> {
                if (nfcViewModel.handleNfcError(event.e).not()) {
                    showError(event.e?.message.orUnknownError())
                }
            }

            is GetTapSignerStatusSuccess -> requireActivity().handleTapSignerStatus(
                event.status,
                onCreateSigner = {
                    NfcSetupActivity.navigate(
                        requireActivity(),
                        NfcSetupActivity.ADD_KEY
                    )
                },
                onSetupNfc = {
                    NfcSetupActivity.navigate(
                        requireActivity(),
                        NfcSetupActivity.SETUP_TAP_SIGNER
                    )
                }
            )

            is GoToSatsCardScreen -> openSatsCardActiveSlotScreen(event)
            is NeedSetupSatsCard -> handleNeedSetupSatsCard(event)
            is SatsCardUsedUp -> handleSatsCardUsedUp(event.numberOfSlot)
            is Loading -> handleLoading(event)
            is NfcLoading -> showOrHideNfcLoading(event.loading)

            WalletsEvent.DenyWalletInvitationSuccess -> showSuccess(message = getString(R.string.nc_deny_wallet_invitation_msg))
            is WalletsEvent.AcceptWalletInvitationSuccess -> {
                if (event.role == AssistedWalletRole.OBSERVER.name) {
                    if (event.isPendingWallet) return
                    navigator.openWalletDetailsScreen(
                        walletId = event.walletId.orEmpty(),
                        activityContext = requireActivity()
                    )
                } else {
                    navigator.openGroupDashboardScreen(
                        groupId = event.groupId,
                        walletId = event.walletId,
                        activityContext = requireActivity(),
                    )
                }
            }

            None -> {}
            is WalletsEvent.ShowExistingKeyDialog -> {
                existingKeyDialog = NCInfoDialog(requireActivity()).showDialog(
                    message = getString(
                        R.string.nc_software_key_upgrade_to_hardware_key,
                        event.key.localSigner.masterFingerprint.uppercase(Locale.getDefault())
                    ),
                    btnYes = getString(R.string.nc_text_yes),
                    btnInfo = getString(R.string.nc_text_no),
                    onYesClick = {
                        openSignerInfoScreen(event.key.localSigner.toModel(), event.key)
                    },
                    onInfoClick = {
                        walletsViewModel.updateExistingKey(event.key)
                    }
                )
            }

            is WalletsEvent.CheckLeaveRoom -> openInputAmountScreen(
                event.walletExtended,
                event.isLeaveRoom
            )
        }
        walletsViewModel.clearEvent()
    }

    private fun openSatsCardActiveSlotScreen(event: GoToSatsCardScreen) {
        SatsCardActivity.navigate(requireActivity(), event.status, walletsViewModel.hasWallet())
    }

    private fun handleNeedSetupSatsCard(event: NeedSetupSatsCard) {
        NCWarningVerticalDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_setup_satscard),
            message = getString(R.string.nc_setup_satscard_msg),
            btnNeutral = getString(R.string.nc_view_unsealed_slots),
            onYesClick = {
                NfcSetupActivity.navigate(
                    requireActivity(),
                    NfcSetupActivity.SETUP_SATSCARD,
                    hasWallet = walletsViewModel.hasWallet()
                )
            },
            onNeutralClick = {
                SatsCardActivity.navigate(
                    requireActivity(),
                    event.status,
                    walletsViewModel.hasWallet(),
                    true
                )
            }
        )
    }

    private fun handleSatsCardUsedUp(numberOfSlot: Int) {
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(
                R.string.nc_all_slot_used_up,
                numberOfSlot
            )
        )
    }

    private fun handleMainActivityEvent(event: MainAppEvent) {
        if (event == SyncCompleted) {
            walletsViewModel.retrieveData()
        }
    }

    private fun handleLoading(event: Loading) {
        showOrHideLoading(event.loading)
    }

    private fun openWalletIntroScreen() {
        activity?.let(navigator::openWalletEmptySignerScreen)
    }

    private fun showWalletState(state: WalletsState) {
        showConnectionBlockchainStatus(state)
        showIntro(state)
        showPendingWallet(state)
        showCampaign(state.campaign, state.wallets.isNotEmpty(), state.localReferrerCode)
        showTotalBalance(state)
        binding.contentContainer.isVisible = isShowEmptyState(state).not()
        binding.emptyStateView.isVisible = isShowEmptyState(state)
        if (isShowEmptyState(state)) showEmptyState(state)
    }

    private fun isShowEmptyState(state: WalletsState): Boolean {
        return state.wallets.isEmpty() && state.groupWalletUis.isEmpty()
    }

    private fun showEmptyState(state: WalletsState) {
        binding.emptyStateView.setContent {
            WalletEmptyStateView(
                activityContext = requireActivity(),
                navigator = navigator,
                groupStage = walletsViewModel.getGroupStage(),
                assistedWalletId = walletsViewModel.getAssistedWalletId().orEmpty(),
                signers = signersViewModel.getSigners(),
                state = state
            )
        }
    }

    private fun showTotalBalance(state: WalletsState) {
        binding.totalBalanceFrame.isVisible = state.wallets.isNotEmpty()
        val totalBalance = state.wallets.sumOf { it.wallet.balance.value }
        val totalInCurrency = Amount(value = totalBalance).getCurrencyAmount()
        val totalInBtc = Amount(value = totalBalance).getBTCAmount()
        binding.totalBalanceView.isVisible = state.homeDisplaySetting.showTotalBalance
        binding.totalBalanceView.setContent {
            NunchukTheme {
                TotalBalanceView(state.homeDisplaySetting.useLargeFont, totalInBtc, totalInCurrency)
            }
        }
    }

    private fun showCampaign(
        campaign: Campaign?,
        isHasWallet: Boolean = false,
        localReferrerCode: ReferrerCode?
    ) {
        binding.llCampaigns.isVisible =
            campaign?.isValid() == true && campaign.isDismissed.not() && (campaign.type == CampaignType.DOWNLOAD || isHasWallet) && localReferrerCode?.status != CampaignStatus.COMPLETED
        binding.tvCampaigns.text = campaign?.cta
    }

    private fun showConnectionBlockchainStatus(state: WalletsState) {
        binding.tvConnectionStatus.isVisible = BLOCKCHAIN_STATUS != null
        when (BLOCKCHAIN_STATUS) {
            ConnectionStatus.OFFLINE -> {
                binding.tvConnectionStatus.text = getString(
                    R.string.nc_text_home_wallet_connection,
                    getString(R.string.nc_text_connection_status_offline),
                    showChainText(state.chain)
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.tvConnectionStatus,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.nc_color_connection_offline
                        )
                    )
                )
            }

            ConnectionStatus.SYNCING -> {
                binding.tvConnectionStatus.text = getString(
                    R.string.nc_text_home_wallet_connection,
                    getString(R.string.nc_text_connection_status_syncing),
                    showChainText(state.chain)
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.tvConnectionStatus,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.nc_color_connection_syncing
                        )
                    )
                )
            }

            ConnectionStatus.ONLINE -> {
                binding.tvConnectionStatus.text = getString(
                    R.string.nc_text_home_wallet_connection,
                    getString(R.string.nc_text_connection_status_online),
                    showChainText(state.chain)
                )
                TextViewCompat.setCompoundDrawableTintList(
                    binding.tvConnectionStatus,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.nc_color_connection_online
                        )
                    )
                )
            }

            else -> {}
        }
    }

    private fun showChainText(chain: Chain): String {
        return when (chain) {
            Chain.TESTNET -> getString(R.string.nc_text_home_wallet_chain_testnet)
            Chain.SIGNET -> getString(R.string.nc_text_home_wallet_chain_signet)
            else -> ""
        }
    }

    private fun showIntro(state: WalletsState) {
        binding.introContainer.isGone = walletsViewModel.getGroupStage() == MembershipStage.DONE
                || state.allGroups.isNotEmpty() || state.wallets.isEmpty()
        if (state.plans != null && state.plans.isEmpty() && state.banner != null) {
            if (state.banner.type == Banner.Type.TYPE_01 && state.isHideUpsellBanner.not() && state.allGroups.isEmpty()) {
                binding.containerNonSubscriber.isVisible = true
            } else if (state.banner.type == Banner.Type.TYPE_REFERRAL_01) {
                binding.containerNonSubscriber.isVisible = true
                binding.tvNonSubscriberExpired.isVisible = true
            } else {
                binding.containerNonSubscriber.isVisible = false
            }
        } else {
            binding.containerNonSubscriber.isVisible = false
        }

        if (state.plans != null) {
            val walletName = state.assistedWallets.firstOrNull()
                ?.let { wallet -> state.wallets.find { wallet.localId == it.wallet.id }?.wallet?.name.orEmpty() }
            when {
                state.plans.isNotEmpty() -> showAssistedWalletStart(
                    state.remainingTime,
                    walletName
                )

                else -> showNonSubscriberIntro(state.banner)
            }
        }
    }

    private fun showNonSubscriberIntro(banner: Banner?) {
        if (banner != null) {
            binding.containerNonSubscriber.tag = banner.id
            Glide.with(binding.ivNonSubscriber)
                .load(banner.content.imageUrl)
                .override(binding.ivNonSubscriber.width)
                .into(binding.ivNonSubscriber)
            binding.tvNonSubscriber.text = banner.content.title
            binding.tvNonSubscriberExpired.text = String.format(
                getString(R.string.nc_banner_expired_time),
                banner.payload.expiryAtMillis.formatMMMddyyyyDate
            )
        }
    }

    private fun showPendingWallet(state: WalletsState) {
        val groupWalletUis = state.groupWalletUis
        val useLargeFont = state.homeDisplaySetting.useLargeFont
        val hideWalletDetail = state.walletSecuritySetting.hideWalletDetail
        val assistedWallets = state.assistedWallets.associateBy { it.localId }
        binding.walletEmpty.isVisible = groupWalletUis.isEmpty()
        binding.pendingWallet.setContent {
            NunchukTheme {
                Column {
                    groupWalletUis.forEach {
                        val briefWallet = assistedWallets[it.wallet?.wallet?.id.orEmpty()]
                        PendingWalletView(
                            group = it.group,
                            sandbox = it.sandbox,
                            walletsExtended = it.wallet,
                            inviterName = it.inviterName,
                            isAssistedWallet = briefWallet?.status == WalletStatus.ACTIVE.name || it.isPendingPersonalWallet,
                            hideWalletDetail = hideWalletDetail,
                            badgeCount = it.badgeCount,
                            primaryOwnerMember = it.primaryOwnerMember,
                            role = it.role,
                            status = it.keyStatus,
                            signers = it.signers,
                            useLargeFont = useLargeFont,
                            walletStatus = briefWallet?.status,
                            showShortcuts = state.homeDisplaySetting.showWalletShortcuts,
                            onAccept = {
                                it.group?.id?.let { groupId ->
                                    walletsViewModel.acceptInviteMember(groupId, it.role)
                                }
                            },
                            onDeny = {
                                showDenyWalletDialog {
                                    it.group?.id?.let {
                                        walletsViewModel.denyInviteMember(it)
                                    }
                                }
                            },
                            onGroupClick = {
                                if (it.group?.id != null && it.role.toRole.isKeyHolderLimited && it.badgeCount == 0) return@PendingWalletView
                                navigator.openGroupDashboardScreen(
                                    groupId = it.group?.id,
                                    walletId = it.wallet?.wallet?.id,
                                    activityContext = requireActivity()
                                )
                            },
                            onWalletClick = {
                                if (it.role == AssistedWalletRole.KEYHOLDER_LIMITED.name || it.group?.isLocked == true) return@PendingWalletView
                                val walletId = it.wallet?.wallet?.id ?: return@PendingWalletView
                                if (briefWallet?.status == WalletStatus.REPLACED.name && briefWallet.replaceByWalletId.isNotEmpty()
                                    && groupWalletUis.any { ui -> ui.wallet?.wallet?.id == briefWallet.replaceByWalletId }
                                ) {
                                    showWalletReplacedDialog(
                                        oldWalletId = walletId,
                                        replaceByWalletId = briefWallet.replaceByWalletId
                                    )
                                } else {
                                    openWalletDetailsScreen(walletId)
                                }
                            },
                            onSendClick = {
                                it.wallet?.let { wallet ->
                                    walletsViewModel.getWalletDetail(wallet.wallet.id)
                                }
                            },
                            onReceiveClick = {
                                val walletId = it.wallet?.wallet?.id ?: return@PendingWalletView
                                navigator.openReceiveTransactionScreen(requireActivity(), walletId)
                            },
                            onOpenFreeGroupWallet = {
                                FreeGroupWalletActivity.start(requireActivity(), it.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    private fun openInputAmountScreen(walletExtended: WalletExtended, isLeaveRoom: Boolean) {
        if (walletExtended.isShared) {
            val roomWallet = walletExtended.roomWallet!!
            if (isLeaveRoom) {
                sessionHolder.setActiveRoom(roomWallet.roomId, true)
                navigator.openInputAmountScreen(
                    activityContext = requireActivity(),
                    roomId = roomWallet.roomId,
                    walletId = roomWallet.walletId,
                    availableAmount = walletExtended.wallet.balance.pureBTC(),
                )
            } else {
                navigator.openRoomDetailActivity(
                    activityContext = requireActivity(),
                    roomId = walletExtended.roomWallet!!.roomId,
                    roomAction = RoomAction.SEND
                )
            }
        } else {
            navigator.openInputAmountScreen(
                activityContext = requireActivity(),
                walletId = walletExtended.wallet.id,
                availableAmount = walletExtended.wallet.balance.pureBTC(),
            )
        }
    }

    private fun showWalletReplacedDialog(oldWalletId: String, replaceByWalletId: String) {
        val wallet = walletsViewModel.getWallet(replaceByWalletId)
        NCInfoDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_wallet_replaced_desc, wallet?.wallet?.name.orEmpty()),
            btnInfo = getString(R.string.nc_go_to_the_new_wallet),
            onInfoClick = {
                openWalletDetailsScreen(replaceByWalletId)
            },
            onYesClick = {
                openWalletDetailsScreen(oldWalletId)
            }
        )
    }

    private fun showDenyWalletDialog(action: () -> Unit) {
        NCWarningDialog(requireActivity()).showDialog(
            title = getString(R.string.nc_text_confirmation),
            message = getString(R.string.nc_deny_wallet_invitation_dialog),
            onYesClick = action
        )
    }

    private fun openWalletDetailsScreen(walletId: String) {
        runCatching {
            findNavController().navigate(
                WalletsFragmentDirections.actionNavigationWalletsToWalletDetailsFragment(
                    walletId = walletId,
                    keyPolicy = walletsViewModel.getKeyPolicy(walletId)
                )
            )
        }
    }

    private fun openSignerInfoScreen(
        signer: SignerModel,
        existingKey: WalletsExistingKey? = null
    ) {
        navigator.openSignerInfoScreen(
            activityContext = requireActivity(),
            isMasterSigner = signer.isMasterSigner,
            id = signer.id,
            masterFingerprint = signer.fingerPrint,
            name = signer.name,
            type = signer.type,
            derivationPath = signer.derivationPath,
            existingKey = existingKey
        )
    }

    override fun onResume() {
        super.onResume()
        with(walletsViewModel) {
            getGroupsSandbox()
            retrieveData()
            updateBadge()
            getKeyHealthStatus()
        }
    }

    override fun onDestroy() {
        existingKeyDialog?.dismiss()
        super.onDestroy()
    }
}