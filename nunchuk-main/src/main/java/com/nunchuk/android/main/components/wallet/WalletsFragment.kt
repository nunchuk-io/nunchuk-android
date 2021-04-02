package com.nunchuk.android.main.components.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseFragment
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.main.databinding.FragmentWalletsBinding
import com.nunchuk.android.main.util.SignerMapper
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
        binding.doLater.setOnClickListener { binding.introContainer.visibility = View.GONE }
        binding.addASigner.setOnClickListener { navigator.openSignerIntroScreen(requireActivity()) }
        binding.signerHeader.setOnClickListener { navigator.openAddSignerScreen(requireActivity()) }
        binding.walletsHeader.setOnClickListener { navigator.openAddWalletScreen(requireActivity()) }
    }

    private fun observeEvent() {
        viewModel.state.observe(this.viewLifecycleOwner, {
            showSigners(it.signers)
            showWallets(it.wallets)
        })
    }

    private fun showWallets(wallets: List<Wallet>) {
        if (wallets.isEmpty()) {
            showWalletsEmptyView()
        } else {
            showWalletsListView()
        }
    }

    private fun showWalletsEmptyView() {}

    private fun showWalletsListView() {}

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