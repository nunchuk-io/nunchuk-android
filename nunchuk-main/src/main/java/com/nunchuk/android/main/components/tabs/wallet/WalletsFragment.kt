/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.MainActivityViewModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.GetConnectionStatusSuccessEvent
import com.nunchuk.android.main.di.MainAppEvent.SyncCompleted
import com.nunchuk.android.main.intro.UniversalNfcIntroActivity
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.signer.satscard.SatsCardActivity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.util.handleTapSignerStatus
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.widget.NCInfoDialog
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
        binding.tvIntroAction.setOnDebounceClickListener {
            if (walletsViewModel.isPremiumUser() == true) {
                navigator.openMembershipActivity(requireActivity(), walletsViewModel.getGroupStage())
            } else {
                // TODO Hai
            }
        }
    }

    private fun openAddWalletScreen() {
        navigator.openWalletIntermediaryScreen(requireActivity(), walletsViewModel.hasSigner())
    }

    private fun openSignerIntroScreen() {
        navigator.openSignerIntroScreen(requireActivity())
    }

    private fun showAssistedWalletStart(remainingTime: Int, hasCreatedWallet: Boolean) {
        val stage = walletsViewModel.getGroupStage()
        val isVisible = stage != MembershipStage.DONE && hasCreatedWallet.not()
        binding.introContainer.isVisible = isVisible
        if (isVisible.not()) return
        binding.introContainer.setBackgroundResource(R.drawable.nc_rounded_denim_background)
        binding.ivIntro.setImageResource(R.drawable.ic_assisted_wallet_intro)
        if (stage == MembershipStage.NONE) {
            binding.tvIntroTitle.text = getString(R.string.nc_let_s_get_you_started)
            binding.tvIntroDesc.text =
                getString(R.string.nc_assisted_wallet_intro_desc, remainingTime)
            binding.tvIntroAction.text = getString(R.string.nc_start_wizard)
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
        if (event is GetConnectionStatusSuccessEvent) {
            walletsViewModel.getAppSettings()
        } else if (event == SyncCompleted) {
            walletsViewModel.retrieveData()
        }
    }

    private fun handleLoading(event: Loading) {
        binding.walletLoading.root.isVisible = event.loading
    }

    private fun openWalletIntroScreen() {
        activity?.let(navigator::openWalletEmptySignerScreen)
    }

    private fun showWalletState(state: WalletsState) {
        val wallets = state.wallets
        val signers =
            walletsViewModel.mapSigners()
        showWallets(wallets, state.assistedWalletIds)
        showSigners(signers)
        showConnectionBlockchainStatus(state)
        showIntro(state)
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
        binding.introContainer.isVisible = state.isPremiumUser != null
        if (state.isPremiumUser != null) {
            when {
                state.isPremiumUser -> showAssistedWalletStart(
                    state.remainingTime,
                    state.hasCreatedWallet
                )
                else -> showNonSubscriberIntro()
            }
        }
    }

    private fun showNonSubscriberIntro() {
        binding.introContainer.setBackgroundResource(R.drawable.nc_rounded_beeswax_tint_radius_small)
        binding.ivIntro.setImageResource(R.drawable.ic_protect_bitcoin)
        binding.tvIntroTitle.text = getString(R.string.nc_inheritance_plan_title)
        binding.tvIntroDesc.text = "" // TODO Hai
        binding.tvIntroAction.text = getString(R.string.nc_check_out_assisted_wallet)
    }

    private fun showWallets(wallets: List<WalletExtended>, assistedWalletIds: Set<String>) {
        if (wallets.isEmpty()) {
            showWalletsEmptyView()
        } else {
            showWalletsListView(wallets, assistedWalletIds)
        }
    }

    private fun showWalletsEmptyView() {
        binding.walletEmpty.isVisible = true
        binding.walletList.isVisible = false
    }

    private fun showWalletsListView(wallets: List<WalletExtended>, assistedWalletIds: Set<String>) {
        binding.walletEmpty.isVisible = false
        binding.walletList.isVisible = true
        WalletsViewBinder(
            container = binding.walletList,
            wallets = wallets,
            assistedWalletIds = assistedWalletIds,
            callback = ::openWalletDetailsScreen
        ).bindItems()
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
        walletsViewModel.getAppSettings()
    }
}