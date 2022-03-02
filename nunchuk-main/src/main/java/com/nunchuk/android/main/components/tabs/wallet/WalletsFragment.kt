package com.nunchuk.android.main.components.tabs.wallet

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.BLOCKCHAIN_STATUS
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.main.MainActivityViewModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.GetConnectionStatusSuccessEvent
import com.nunchuk.android.main.di.MainAppEvent.SynCompleted
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus

internal class WalletsFragment : BaseFragment<FragmentWalletsBinding>() {

    private val walletsViewModel: WalletsViewModel by viewModels { factory }

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels { factory }

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
        binding.doLater.setOnClickListener { hideIntroContainerView() }
        binding.btnAdd.setOnClickListener { walletsViewModel.handleAddSignerOrWallet() }
        binding.btnAddSigner.setOnClickListener { walletsViewModel.handleAddSigner() }
        binding.ivAddWallet.setOnClickListener { walletsViewModel.handleAddWallet() }
    }

    private fun openAddWalletScreen() {
        navigator.openWalletIntermediaryScreen(requireActivity(), walletsViewModel.hasSigner())
    }

    private fun openSignerIntroScreen() {
        navigator.openSignerIntroScreen(requireActivity())
    }

    private fun hideIntroContainerView() {
        binding.introContainer.isVisible = false
    }

    private fun observeEvent() {
        walletsViewModel.state.observe(viewLifecycleOwner, ::showWalletState)
        walletsViewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        mainActivityViewModel.event.observe(viewLifecycleOwner, ::handleMainActivityEvent)
    }

    private fun handleEvent(event: WalletsEvent) {
        when (event) {
            AddWalletEvent -> openAddWalletScreen()
            ShowSignerIntroEvent -> openSignerIntroScreen()
            WalletEmptySignerEvent -> openWalletIntroScreen()
            is ShowErrorEvent -> requireActivity().showToast(event.message)
            is Loading -> handleLoading(event)
        }
    }

    private fun handleMainActivityEvent(event: MainAppEvent) {
        if (event is GetConnectionStatusSuccessEvent) {
            walletsViewModel.getAppSettings()
        } else if (event == SynCompleted) {
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
        val signers = state.masterSigners.map(MasterSigner::toModel) + state.signers.map(SingleSigner::toModel)
        showWallets(wallets)
        showSigners(signers)
        showConnectionBlockchainStatus(state)
        showIntro(signers, wallets)
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
        }
    }

    private fun showChainText(chain: Chain): String {
        return when (chain) {
            Chain.TESTNET -> getString(R.string.nc_text_home_wallet_chain_testnet)
            else -> ""
        }
    }

    private fun showIntro(signers: List<SignerModel>, wallets: List<WalletExtended>) {
        when {
            signers.isEmpty() -> showAddSignerIntro()
            wallets.isEmpty() -> showAddWalletIntro()
            else -> hideIntroContainerView()
        }
    }

    private fun showAddWalletIntro() {
        binding.introContainer.isVisible = true
        binding.introTitle.text = getString(R.string.nc_wallet_intro_title)
        binding.introSubtitle.text = getString(R.string.nc_wallet_intro_subtitle)
        binding.btnAdd.text = getString(R.string.nc_text_add_a_wallet)
        binding.iconInfo.setImageResource(R.drawable.ic_wallet_info)
    }

    private fun showAddSignerIntro() {
        binding.introContainer.isVisible = true
        binding.introTitle.text = getString(R.string.nc_signer_intro_title)
        binding.introSubtitle.text = getString(R.string.nc_signer_intro_subtitle)
        binding.btnAdd.text = getString(R.string.nc_text_add_signer)
        binding.iconInfo.setImageResource(R.drawable.ic_key_info)
    }

    private fun showWallets(wallets: List<WalletExtended>) {
        if (wallets.isEmpty()) {
            showWalletsEmptyView()
        } else {
            showWalletsListView(wallets)
        }
    }

    private fun showWalletsEmptyView() {
        binding.walletEmpty.isVisible = true
        binding.walletList.isVisible = false
    }

    private fun showWalletsListView(wallets: List<WalletExtended>) {
        binding.walletEmpty.isVisible = false
        binding.walletList.isVisible = true
        WalletsViewBinder(
            container = binding.walletList,
            wallets = wallets,
            callback = ::openWalletDetailsScreen
        ).bindItems()
    }

    private fun openWalletDetailsScreen(walletId: String) {
        navigator.openWalletDetailsScreen(requireActivity(), walletId)
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
        SignersViewBinder(binding.signerList, signers, ::openSignerInfoScreen).bindItems()
    }

    private fun openSignerInfoScreen(signer: SignerModel) {
        navigator.openSignerInfoScreen(
            activityContext = requireActivity(),
            id = signer.id,
            name = signer.name,
            software = signer.software
        )
    }

    override fun onResume() {
        super.onResume()
        binding.walletLoading.root.isVisible = true
        walletsViewModel.retrieveData()
        walletsViewModel.getAppSettings()
    }
}