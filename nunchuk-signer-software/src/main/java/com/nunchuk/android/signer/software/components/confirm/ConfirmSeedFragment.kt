package com.nunchuk.android.signer.software.components.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedEvent.ConfirmSeedCompletedEvent
import com.nunchuk.android.signer.software.components.confirm.ConfirmSeedEvent.SelectedIncorrectWordEvent
import com.nunchuk.android.signer.software.databinding.FragmentConfirmSeedBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmSeedFragment : BaseFragment<FragmentConfirmSeedBinding>() {

    private val viewModel: ConfirmSeedViewModel by viewModels()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentConfirmSeedBinding {
        return FragmentConfirmSeedBinding.inflate(inflater, container, false)
    }

    private val args: ConfirmSeedArgs by lazy { ConfirmSeedArgs.deserializeFrom(requireArguments()) }

    private lateinit var adapter: ConfirmSeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeEvent()
        viewModel.init(args.mnemonic)
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: ConfirmSeedState) {
        adapter.items = state.groups
    }

    private fun handleEvent(event: ConfirmSeedEvent) {
        when (event) {
            ConfirmSeedCompletedEvent -> openSetNameScreen()
            SelectedIncorrectWordEvent -> NCToastMessage(requireActivity()).showError(getString(R.string.nc_ssigner_confirm_seed_error))
        }
    }

    private fun openSetNameScreen() {
        navigator.openAddSoftwareSignerNameScreen(requireActivity(), args.mnemonic)
    }

    private fun setupViews() {
        adapter = ConfirmSeedAdapter(viewModel::updatePhraseWordGroup)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        binding.recyclerView.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }
}