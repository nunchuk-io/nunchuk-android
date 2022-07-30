package com.nunchuk.android.signer.software.components.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedEvent.ConfirmSeedCompletedEvent
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedEvent.SelectedIncorrectWordEvent
import com.nunchuk.android.signer.software.databinding.FragmentConfirmSeedBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmSeedFragment : BaseFragment<FragmentConfirmSeedBinding>() {
    private val args: ConfirmSeedFragmentArgs by navArgs()
    private val viewModel: ConfirmSeedViewModel by viewModels()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentConfirmSeedBinding {
        return FragmentConfirmSeedBinding.inflate(inflater, container, false)
    }

    private lateinit var adapter: ConfirmSeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        flowObserver(viewModel.event, ::handleEvent)
        flowObserver(viewModel.state, ::handleState)
    }

    private fun handleState(state: ConfirmSeedState) {
        adapter.submitList(state.groups)
    }

    private fun handleEvent(event: ConfirmSeedEvent) {
        when (event) {
            ConfirmSeedCompletedEvent -> openSetPassphrase()
            SelectedIncorrectWordEvent -> NCToastMessage(requireActivity()).showError(getString(R.string.nc_ssigner_confirm_seed_error))
        }
    }

    private fun openSetPassphrase() {
        if (args.isQuickWallet) {
            findNavController().navigate(ConfirmSeedFragmentDirections.actionConfirmSeedFragmentToSetPassphraseFragment(args.mnemonic, DEFAULT_KEY_NAME, true))
        } else {
            openSetNameScreen()
        }
    }

    private fun openSetNameScreen() {
        navigator.openAddSoftwareSignerNameScreen(requireActivity(), args.mnemonic)
    }

    private fun setupViews() {
        adapter = ConfirmSeedAdapter(viewModel::updatePhraseWordGroup)

        binding.note.tag = 0
        binding.note.setOnClickListener {
            val value = (binding.note.tag as Int).inc()
            binding.note.tag = value
            if ((value % BY_PASS_PRESS_COUNT) == 0) {
                openSetPassphrase()
            }
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    companion object {
        private const val BY_PASS_PRESS_COUNT = 7
        private const val DEFAULT_KEY_NAME = "My Key"
    }
}