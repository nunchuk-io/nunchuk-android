package com.nunchuk.android.main.components.tabs.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseFragment
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.main.R
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.signer.util.SignerMapper
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.core.util.showToast
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        binding.btnAdd.setOnClickListener { openSignerIntroScreen() }
        binding.signerHeader.setOnClickListener { openAddSignerScreen() }
        binding.walletsHeader.setOnClickListener { openAddWalletScreen() }
    }

    private fun openAddWalletScreen() {
        navigator.openAddWalletScreen(requireActivity())
    }

    private fun openAddSignerScreen() {
        navigator.openAddSignerScreen(requireActivity())
    }

    private fun openSignerIntroScreen() {
        navigator.openSignerIntroScreen(requireActivity())
    }

    private fun hideIntroContainerView() {
        binding.introContainer.visibility = View.GONE
    }

    private fun observeEvent() {
        viewModel.state.observe(viewLifecycleOwner, ::showWalletState)
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleEvent(event: WalletsEvent) {
        when (event) {
            WalletsEvent.AddSignerEvent -> openAddSignerScreen()
            WalletsEvent.AddWalletEvent -> openAddWalletScreen()
            is WalletsEvent.ShowErrorEvent -> requireActivity().showToast(event.message)
        }
    }

    private fun showWalletState(state: WalletsState) {
        showIntro(state)
        showSigners(state.signers)
        showWallets(state.wallets)
    }

    private fun showIntro(state: WalletsState) {
        when {
            state.signers.isEmpty() -> showAddSignerIntro()
            state.wallets.isEmpty() -> showAddWalletIntro()
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
            showWalletsListView()
        }
    }

    private fun showWalletsEmptyView() {
        binding.walletEmpty.visibility = View.VISIBLE
        binding.walletList.visibility = View.GONE
    }

    private fun showWalletsListView() {
        binding.walletEmpty.visibility = View.VISIBLE
        binding.walletList.visibility = View.GONE
    }

    private fun showSigners(signers: List<SingleSigner>) {
        if (signers.isEmpty()) {
            showSignersEmptyView()
        } else {
            showSignersListView(signers)
        }
    }

    private fun showSignersEmptyView() {
        binding.signerEmpty.visibility = View.VISIBLE
        binding.signerList.visibility = View.GONE
    }

    private fun showSignersListView(signers: List<SingleSigner>) {
        binding.signerEmpty.visibility = View.GONE
        binding.signerList.visibility = View.VISIBLE
        SignersViewBinder(binding.signerList, signers, ::openSignerInfoScreen).bindItems()
    }

    private fun openSignerInfoScreen(signer: SingleSigner) {
        navigator.openSignerInfoScreen(requireActivity(), signer.name, SignerMapper.toSpec(signer))
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