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
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus

internal class WalletsFragment : BaseFragment<FragmentWalletsBinding>() {

    private val viewModel: WalletsViewModel by viewModels { factory }
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
        binding.btnAdd.setOnClickListener { viewModel.handleAddSignerOrWallet() }
        binding.signerHeader.setOnClickListener { viewModel.handleAddSigner() }
        binding.ivAddWallet.setOnClickListener { viewModel.handleAddWallet() }
    }

    private fun openAddWalletScreen() {
        navigator.openAddWalletScreen(requireActivity())
    }

    private fun openSignerIntroScreen() {
        navigator.openSignerIntroScreen(requireActivity())
    }

    private fun hideIntroContainerView() {
        binding.introContainer.isVisible = false
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::showWalletState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        mainActivityViewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleEvent(event: WalletsEvent) {
        when (event) {
            AddWalletEvent -> openAddWalletScreen()
            ShowSignerIntroEvent -> openSignerIntroScreen()
            WalletEmptySignerEvent -> openWalletIntroScreen()
            is GetConnectionStatusSuccessEvent -> {
                viewModel.getAppSettings()
            }
            is ShowErrorEvent -> requireActivity().showToast(event.message)
            is Loading -> handleLoading(event)
        }
    }

    private fun handleLoading(event: Loading) {
        binding.signerProgress.isVisible = event.loading
        binding.walletProgress.isVisible = event.loading
    }

    private fun openWalletIntroScreen() {
        activity?.let(navigator::openWalletEmptySignerScreen)
    }

    private fun showWalletState(state: WalletsState) {
        val signers = state.masterSigners.map(MasterSigner::toModel) + state.signers.map(SingleSigner::toModel)
        showIntro(signers, state.wallets)
        showWallets(state.wallets)
        showSigners(signers)
        showConnectionBlockchainStatus(state)
    }

    private fun showConnectionBlockchainStatus(state: WalletsState) {
        binding.tvConnectionStatus.isVisible = BLOCKCHAIN_STATUS != null
        when(BLOCKCHAIN_STATUS) {
            ConnectionStatus.OFFLINE -> {
                binding.tvConnectionStatus.text = getString(
                    R.string.nc_text_home_wallet_connection,
                    getString(R.string.nc_text_connection_status_offline),
                    showChainText(state.chain)
                )
                TextViewCompat.setCompoundDrawableTintList(binding.tvConnectionStatus, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.nc_color_connection_offline)))
            }
            ConnectionStatus.SYNCING -> {
                binding.tvConnectionStatus.text = getString(
                    R.string.nc_text_home_wallet_connection,
                    getString(R.string.nc_text_connection_status_syncing),
                    showChainText(state.chain)
                )
                TextViewCompat.setCompoundDrawableTintList(binding.tvConnectionStatus, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.nc_color_connection_syncing)))
            }
            ConnectionStatus.ONLINE -> {
                binding.tvConnectionStatus.text = getString(
                    R.string.nc_text_home_wallet_connection,
                    getString(R.string.nc_text_connection_status_online),
                    showChainText(state.chain)
                )
                TextViewCompat.setCompoundDrawableTintList(binding.tvConnectionStatus, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.nc_color_connection_online)))
            }
        }
    }

    private fun showChainText(chain: Chain): String {
        return when(chain) {
            Chain.TESTNET -> getString(R.string.nc_text_home_wallet_chain_testnet);
            else -> ""
        }
    }

    private fun showIntro(signers: List<SignerModel>, wallets: List<Wallet>) {
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
    }

    private fun showAddSignerIntro() {
        binding.introContainer.isVisible = true
        binding.introTitle.text = getString(R.string.nc_signer_intro_title)
        binding.introSubtitle.text = getString(R.string.nc_signer_intro_subtitle)
        binding.btnAdd.text = getString(R.string.nc_text_add_signer)
    }

    private fun showWallets(wallets: List<Wallet>) {
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

    private fun showWalletsListView(wallets: List<Wallet>) {
        binding.walletEmpty.isVisible = false
        binding.walletList.isVisible = true
        WalletsViewBinder(binding.walletList, wallets, ::openWalletDetailsScreen).bindItems()
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
        viewModel.retrieveData()
    }
}