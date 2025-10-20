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
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.contact.components.contacts.ContactsViewModel
import com.nunchuk.android.core.account.AccountManager
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
import com.nunchuk.android.core.util.DeeplinkHolder
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
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
import com.nunchuk.android.main.components.tabs.wallet.component.WalletsScreen
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.SyncCompleted
import com.nunchuk.android.main.groupwallet.join.UnableJoinGroupWalletActivity
import com.nunchuk.android.main.intro.UniversalNfcIntroActivity
import com.nunchuk.android.main.nonsubscriber.NonSubscriberActivity
import com.nunchuk.android.messages.components.list.RoomMessage
import com.nunchuk.android.messages.components.list.RoomsViewModel
import com.nunchuk.android.messages.util.SUBSCRIPTION_SUBSCRIPTION_ACTIVE
import com.nunchuk.android.messages.util.SUBSCRIPTION_SUBSCRIPTION_PENDING
import com.nunchuk.android.messages.util.getMsgType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.isByzantineOrFinney
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.MainComposeArgs
import com.nunchuk.android.signer.satscard.SatsCardActivity
import com.nunchuk.android.signer.signer.SignersViewModel
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.util.handleTapSignerStatus
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
internal class WalletsFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var sessionHolder: SessionHolder

    @Inject
    lateinit var deeplinkHolder: DeeplinkHolder

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val state by walletsViewModel.state.collectAsStateWithLifecycle()
        val hasSigner by signersViewModel.uiState.map { it.signers.orEmpty().isNotEmpty() }.collectAsStateWithLifecycle(false)
        NunchukTheme {
            WalletsScreen(
                activity = requireActivity(),
                hasSigner = hasSigner,
                navigator = navigator,
                state = state,
                onNfcClick = {
                    if (walletsViewModel.isShownNfcUniversal.value) {
                        UniversalNfcIntroActivity.navigate(launcher, requireActivity())
                    } else {
                        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_AUTO_CARD_STATUS)
                    }
                },
                onCampaignClick = {
                    walletsViewModel.getCurrentCampaign()?.let {
                        navigator.openReferralScreen(
                            activityContext = requireActivity(),
                            args = ReferralArgs(
                                campaign = it,
                                localReferrerCode = walletsViewModel.getLocalReferrerCode()
                            )
                        )
                    }
                },
                onIntroContainerClick = { stage ->
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
                        val isPersonalWallet =
                            walletType != null || plans.none { it.isByzantineOrFinney() }
                        navigator.openMembershipActivity(
                            activityContext = requireActivity(),
                            groupStep = stage,
                            walletId = walletsViewModel.getAssistedWalletId(),
                            isPersonalWallet = isPersonalWallet,
                            walletType = walletType
                        )
                    }
                },
                onBannerClick = { banner ->
                    if (banner.type == Banner.Type.TYPE_REFERRAL_01) {
                        requireActivity().openExternalLink(banner.content.action.target)
                    } else {
                        NonSubscriberActivity.start(requireActivity(), banner.id)
                    }
                },
                onAccept = { groupWalletUi ->
                    groupWalletUi.group?.id?.let { groupId ->
                        walletsViewModel.acceptInviteMember(groupId, groupWalletUi.role)
                    }
                },
                denyInviteMember = { groupId ->
                    walletsViewModel.denyInviteMember(groupId)
                },
                showWalletReplacedDialog = ::showWalletReplacedDialog,
                getWalletDetail = walletsViewModel::getWalletDetail,
                openWalletDetailsScreen = ::openWalletDetailsScreen,
                openArchivedWalletsScreen = {
                    navigator.openMainComposeScreen(
                        activity = requireActivity(),
                        args = MainComposeArgs(MainComposeArgs.TYPE_ARCHIVE)
                    )
                },
                onMove = walletsViewModel::onMove
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeEvent()
    }

    private fun openAddWalletScreen() {
        navigator.openWalletIntermediaryScreen(requireActivity())
    }

    private fun observeEvent() {
        flowObserver(walletsViewModel.event, ::handleEvent)
        mainActivityViewModel.event.observe(viewLifecycleOwner, ::handleMainActivityEvent)
        roomViewModel.state.observe(viewLifecycleOwner) {
            if (walletsViewModel.isPremiumUser().not()) {
                it.rooms.filterIsInstance<RoomMessage.MatrixRoom>().map { it.data }
                    .forEach { room ->
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

            WalletsEvent.JoinFreeGroupWalletFailed -> {
                UnableJoinGroupWalletActivity.start(
                    requireActivity(),
                    link = deeplinkHolder.groupLinkInfo.value?.referringLink.orEmpty()
                )
            }

            is WalletsEvent.JoinFreeGroupWalletSuccess -> {
                navigator.openFreeGroupWalletScreen(
                    activityContext = requireActivity(),
                    groupId = event.groupId,
                )
            }
        }
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

    private fun openInputAmountScreen(walletExtended: WalletExtended, isLeaveRoom: Boolean) {
        if (walletExtended.isShared) {
            val roomWallet = walletExtended.roomWallet!!
            if (isLeaveRoom) {
                sessionHolder.setActiveRoom(roomWallet.roomId, true)
                navigator.openInputAmountScreen(
                    activityContext = requireActivity(),
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