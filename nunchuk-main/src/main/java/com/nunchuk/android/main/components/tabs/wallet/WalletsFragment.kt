package com.nunchuk.android.main.components.tabs.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseFragment
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.NunchukNavigator
import javax.inject.Inject

internal class WalletsFragment : BaseFragment() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: WalletsViewModel by lazy {
        ViewModelProviders.of(this, factory).get(WalletsViewModel::class.java)
    }

    private var _binding: FragmentWalletsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()

        observeEvent()
    }

    private fun setupViews() {
        binding.doLater.setOnClickListener { hideIntroContainerView() }
        binding.btnAdd.setOnClickListener { viewModel.handleAddSignerOrWallet() }
        binding.signerHeader.setOnClickListener { viewModel.handleAddSigner() }
        binding.walletsHeader.setOnClickListener { viewModel.handleAddWallet() }
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
    }

    private fun handleEvent(event: WalletsEvent) {
        when (event) {
            AddWalletEvent -> openAddWalletScreen()
            ShowSignerIntroEvent -> openSignerIntroScreen()
            is ShowErrorEvent -> requireActivity().showToast(event.message)
        }
    }

    private fun showWalletState(state: WalletsState) {
        val signers = state.masterSigners.map(MasterSigner::toModel) + state.signers.map(SingleSigner::toModel)
        showIntro(signers, state.wallets)
        showSigners(signers)
        showWallets(state.wallets)
    }

    private fun showIntro(signers:List<SignerModel>, wallets: List<Wallet>) {
        when {
            signers.isEmpty() -> showAddSignerIntro()
            wallets.isEmpty() -> showAddWalletIntro()
            else -> hideIntroContainerView()
        }
    }

    private fun showAddWalletIntro() {
        binding.introTitle.text = getString(R.string.nc_wallet_intro_title)
        binding.introSubtitle.text = getString(R.string.nc_wallet_intro_subtitle)
        binding.btnAdd.text = getString(R.string.nc_text_add_a_wallet)
    }

    private fun showAddSignerIntro() {
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
        WalletsViewBinder(binding.walletList, wallets, ::openWalletReviewScreen).bindItems()
    }

    private fun openWalletReviewScreen(walletId: String) {
        navigator.openWalletConfigScreen(requireActivity(), walletId)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}