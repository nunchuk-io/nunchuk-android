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

package com.nunchuk.android.signer.software.components.confirm

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
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
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmSeedFragment : BaseFragment<FragmentConfirmSeedBinding>() {
    private val args: ConfirmSeedFragmentArgs by navArgs()
    private val viewModel: ConfirmSeedViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentConfirmSeedBinding {
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
            ConfirmSeedCompletedEvent -> confirmBeforeOpenNewScreen()
            SelectedIncorrectWordEvent -> NCToastMessage(requireActivity()).showError(getString(R.string.nc_ssigner_confirm_seed_error))
        }
    }

    private fun confirmBeforeOpenNewScreen() {
        NCInfoDialog(requireActivity()).showDialog(
            message = SpannableStringBuilder().bold {
                append(getString(R.string.nc_seed_phase_confirmation_desc_one))
            }.append(getString(R.string.nc_seed_phase_confirmation_desc_two)),
            btnInfo = getString(R.string.nc_i_ve_backed_it_up),
            onYesClick = {
                if (args.isQuickWallet) {
                    findNavController().popBackStack()
                } else {
                    requireActivity().finish()
                }
            },
            onInfoClick = { openSetPassphrase() }
        )
    }

    private fun openSetPassphrase() {
        if (args.masterSignerId.isNotEmpty()) {
            // TODO Hai confirm backup seed
            navigator.returnToMainScreen()
            navigator.openSignerInfoScreen(
                activityContext = requireActivity(),
                id = args.masterSignerId,
                isInAssistedWallet = false,
                isInWallet = true,
                masterFingerprint = args.masterSignerId,
                type = SignerType.SOFTWARE,
                name = "",
                isMasterSigner = true
            )
        } else if (args.isQuickWallet) {
            findNavController().navigate(
                ConfirmSeedFragmentDirections.actionConfirmSeedFragmentToSetPassphraseFragment(
                    mnemonic = args.mnemonic,
                    signerName = DEFAULT_KEY_NAME,
                    isQuickWallet = true,
                    primaryKeyFlow = args.primaryKeyFlow
                )
            )
        } else {
            openSetNameScreen()
        }
    }

    private fun openSetNameScreen() {
        navigator.openAddSoftwareSignerNameScreen(
            requireActivity(),
            args.mnemonic,
            args.primaryKeyFlow,
            passphrase = args.passphrase
        )
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