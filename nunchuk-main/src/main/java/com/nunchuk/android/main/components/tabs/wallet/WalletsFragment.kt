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
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.contact.components.contacts.ContactsViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.BLOCKCHAIN_STATUS
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.main.MainActivityViewModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.AddWalletEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.CheckWalletPin
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GetTapSignerStatusSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.GoToSatsCardScreen
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.Loading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NeedSetupSatsCard
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.NfcLoading
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.None
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.SatsCardUsedUp
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowErrorEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.ShowSignerIntroEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.VerifyPassphraseSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.VerifyPasswordSuccess
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.WalletEmptySignerEvent
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.SyncCompleted
import com.nunchuk.android.main.intro.UniversalNfcIntroActivity
import com.nunchuk.android.main.membership.byzantine.views.PendingWalletView
import com.nunchuk.android.main.nonsubscriber.NonSubscriberActivity
import com.nunchuk.android.messages.components.list.RoomsViewModel
import com.nunchuk.android.messages.util.SUBSCRIPTION_SUBSCRIPTION_ACTIVE
import com.nunchuk.android.messages.util.SUBSCRIPTION_SUBSCRIPTION_PENDING
import com.nunchuk.android.messages.util.getMsgType
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.signer.satscard.SatsCardActivity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.util.handleTapSignerStatus
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@AndroidEntryPoint
internal class WalletsFragment : BaseFragment<FragmentWalletsBinding>() {

    @Inject
    lateinit var accountManager: AccountManager

    private val walletsViewModel: WalletsViewModel by activityViewModels()

    private val roomViewModel: RoomsViewModel by activityViewModels()

    private val contactViewModel: ContactsViewModel by activityViewModels()

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private val signerAdapter = SignerAdapter(::openSignerInfoScreen)

    private val nfcViewModel: NfcViewModel by activityViewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_AUTO_CARD_STATUS)
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentWalletsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        observeEvent()
    }

    private fun setupViews() {
        binding.signerList.addItemDecoration(SimpleItemDecoration(requireContext()))
        binding.signerList.adapter = signerAdapter
        binding.btnAddSigner.setOnClickListener { walletsViewModel.handleAddSigner() }
        binding.ivAddWallet.setOnClickListener { walletsViewModel.handleAddWallet() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.menu_nfc) {
                if (walletsViewModel.isShownNfcUniversal.value) {
                    UniversalNfcIntroActivity.navigate(launcher, requireActivity())
                } else {
                    (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_AUTO_CARD_STATUS)
                }
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
        binding.introContainer.setOnDebounceClickListener {
            navigator.openMembershipActivity(
                activityContext = requireActivity(),
                groupStep = walletsViewModel.getGroupStage(),
                walletId = walletsViewModel.getAssistedWalletId(),
            )
        }
        binding.containerNonSubscriber.setOnDebounceClickListener {
            NonSubscriberActivity.start(requireActivity(), it.tag as String)
        }
    }

    private fun openAddWalletScreen() {
        navigator.openWalletIntermediaryScreen(requireActivity(), walletsViewModel.hasSigner())
    }

    private fun openSignerIntroScreen() {
        navigator.openSignerIntroScreen(requireActivity())
    }

    private fun showAssistedWalletStart(
        remainingTime: Int,
        walletName: String?,
    ) {
        val stage = walletsViewModel.getGroupStage()
        val isGone = stage == MembershipStage.DONE
        binding.introContainer.isGone = isGone
        if (isVisible.not()) return
        if (stage == MembershipStage.NONE) {
            binding.tvIntroTitle.text = getString(R.string.nc_let_s_get_you_started)
            binding.tvIntroDesc.text =
                getString(R.string.nc_assisted_wallet_intro_desc)
            binding.tvIntroAction.text = getString(R.string.nc_start_wizard)
        } else if (stage == MembershipStage.SETUP_INHERITANCE) {
            binding.tvIntroTitle.text =
                getString(R.string.nc_setup_inheritance_for, walletName.orEmpty())
            binding.tvIntroDesc.text =
                getString(R.string.nc_estimate_remain_time, remainingTime)
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
                it.rooms.forEach { room ->
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
            walletsViewModel.getSatsCardStatus(IsoDep.get(it.tag))
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleEvent(event: WalletsEvent) {
        when (event) {
            AddWalletEvent -> openAddWalletScreen()
            ShowSignerIntroEvent -> openSignerIntroScreen()
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
            is CheckWalletPin -> {
                if (event.match) {
                    openWalletDetailsScreen(event.walletId)
                } else {
                    showError(message = getString(R.string.nc_incorrect_current_pin))
                }
            }

            is VerifyPasswordSuccess -> actionAfterCheckingPasswordOrPassphrase(event.walletId)
            is VerifyPassphraseSuccess -> actionAfterCheckingPasswordOrPassphrase(event.walletId)
            WalletsEvent.DenyWalletInvitationSuccess -> showSuccess(message = getString(R.string.nc_deny_wallet_invitation_msg))
            None -> {}
        }
        walletsViewModel.clearEvent()
    }

    private fun actionAfterCheckingPasswordOrPassphrase(walletId: String) {
        if (walletsViewModel.isWalletPinEnable()) {
            showInputPinDialog(walletId)
        } else {
            openWalletDetailsScreen(walletId)
        }
    }

    private fun showInputPinDialog(walletId: String) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(com.nunchuk.android.settings.R.string.nc_enter_your_pin),
            onConfirmed = {
                walletsViewModel.checkWalletPin(it, walletId)
            }
        )
    }

    private fun enterPasswordDialog(walletId: String) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                walletsViewModel.confirmPassword(it, walletId)
            }
        )
    }

    private fun enterPassphraseDialog(walletId: String) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_passphrase),
            descMessage = getString(R.string.nc_re_enter_your_passphrase_dialog_desc),
            onConfirmed = {
                walletsViewModel.confirmPassphrase(it, walletId)
            }
        )
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
        showSigners(state.signers)
        showConnectionBlockchainStatus(state)
        showIntro(state)
        showPendingWallet(state.groupWalletUis)
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
        binding.introContainer.isVisible = state.plan != null && state.plan != MembershipPlan.NONE
        binding.containerNonSubscriber.isVisible =
            state.plan != null && state.plan == MembershipPlan.NONE
        if (state.plan != null) {
            val walletName = state.assistedWallets.firstOrNull()
                ?.let { wallet -> state.wallets.find { wallet.localId == it.wallet.id }?.wallet?.name.orEmpty() }
            when {
                state.plan != MembershipPlan.NONE -> showAssistedWalletStart(
                    state.remainingTime,
                    walletName
                )
                else -> showNonSubscriberIntro(state.banner, state.isHideUpsellBanner)
            }
        }
    }

    private fun showNonSubscriberIntro(banner: Banner?, isHideUpsellBanner: Boolean) {
        binding.containerNonSubscriber.isVisible = banner != null && isHideUpsellBanner.not()
        if (banner != null) {
            binding.containerNonSubscriber.tag = banner.id
            Glide.with(binding.ivNonSubscriber)
                .load(banner.url)
                .override(binding.ivNonSubscriber.width)
                .into(binding.ivNonSubscriber)
            binding.tvNonSubscriber.text = banner.title
        }
    }

    private fun showPendingWallet(groupWalletUis: List<GroupWalletUi>) {
        binding.walletEmpty.isVisible = groupWalletUis.isEmpty()
        binding.walletList.isVisible = groupWalletUis.isNotEmpty()
        binding.pendingWallet.setContent {
            NunchukTheme(false) {
                Column {
                    groupWalletUis.forEach {
                        PendingWalletView(
                            group = it.group,
                            walletsExtended = it.wallet,
                            inviterName = it.inviterName,
                            isAssistedWallet = it.isAssistedWallet,
                            badgeCount = it.badgeCount,
                            role = it.role,
                            onAccept = {
                                it.group?.groupId?.let {
                                    walletsViewModel.acceptInviteMember(it)
                                }
                            },
                            onDeny = {
                                showDenyWalletDialog {
                                    it.group?.groupId?.let {
                                        walletsViewModel.denyInviteMember(it)
                                    }
                                }
                            },
                            onGroupClick = {
                                if (it.group?.groupId != null) {
                                    navigator.openGroupDashboardScreen(
                                        groupId = it.group.groupId,
                                        walletId = it.wallet?.wallet?.id,
                                        activityContext = requireActivity()
                                    )
                                }
                            },
                            onWalletClick = {
                                if (it.role == AssistedWalletRole.KEYHOLDER_LIMITED.name) return@PendingWalletView
                                val walletId = it.wallet?.wallet?.id ?: return@PendingWalletView
                                checkWalletSecurity(walletId)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    private fun showDenyWalletDialog(action: () -> Unit) {
        NCWarningDialog(requireActivity()).showDialog(title = getString(R.string.nc_text_confirmation),
            message = getString(R.string.nc_deny_wallet_invitation_dialog),
            onYesClick = action)
    }

    private fun checkWalletSecurity(walletId: String) {
        if (walletsViewModel.isWalletPasswordEnable()) {
            enterPasswordDialog(walletId)
        } else if (walletsViewModel.isWalletPassphraseEnable()) {
            enterPassphraseDialog(walletId)
        } else if (walletsViewModel.isWalletPinEnable()) {
            showInputPinDialog(walletId)
        } else {
            openWalletDetailsScreen(walletId)
        }
    }

    private fun openWalletDetailsScreen(walletId: String) {
        findNavController().navigate(
            WalletsFragmentDirections.actionNavigationWalletsToWalletDetailsFragment(
                walletId = walletId,
                keyPolicy = walletsViewModel.getKeyPolicy(walletId)
            )
        )
    }

    private fun showSigners(signers: List<SignerModel>) {
        if (signers.isEmpty()) {
            showSignersEmptyView()
        } else {
            showSignersListView(signers)
        }
    }

    private fun showSignersEmptyView() {
        binding.signerEmpty.isVisible = true
        binding.signerList.isVisible = false
    }

    private fun showSignersListView(signers: List<SignerModel>) {
        binding.signerEmpty.isVisible = false
        binding.signerList.isVisible = true
        signerAdapter.submitList(signers)
    }

    private fun openSignerInfoScreen(signer: SignerModel) {
        val isInWallet = walletsViewModel.isInWallet(signer)
        navigator.openSignerInfoScreen(
            activityContext = requireActivity(),
            id = signer.id,
            masterFingerprint = signer.fingerPrint,
            name = signer.name,
            type = signer.type,
            derivationPath = signer.derivationPath,
            isInWallet = isInWallet
        )
    }

    override fun onResume() {
        super.onResume()
        walletsViewModel.retrieveData()
    }
}