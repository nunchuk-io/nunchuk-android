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

package com.nunchuk.android.signer.software.components.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.OpenSelectPhraseEvent
import com.nunchuk.android.signer.software.databinding.FragmentCreateSeedBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateNewSeedFragment : BaseFragment<FragmentCreateSeedBinding>() {
    private val args: CreateNewSeedFragmentArgs by navArgs()
    private val viewModel: CreateNewSeedViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCreateSeedBinding {
        return FragmentCreateSeedBinding.inflate(inflater, container, false)
    }

    private lateinit var adapter: CreateNewSeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeEvent()
        viewModel.init()
    }

    private fun observeEvent() {
        flowObserver(viewModel.event, ::handleEvent)
        flowObserver(viewModel.state, ::handleState)
    }

    private fun handleState(state: CreateNewSeedState) {
        adapter.items = state.seeds
    }

    private fun handleEvent(event: CreateNewSeedEvent) {
        when (event) {
            is GenerateMnemonicCodeErrorEvent -> NCToastMessage(requireActivity()).showWarning(event.message)
            is OpenSelectPhraseEvent -> {
                if (args.isQuickWallet) {
                    findNavController().navigate(
                        CreateNewSeedFragmentDirections.actionCreateNewSeedFragmentToConfirmSeedFragment(
                            mnemonic = event.mnemonic,
                            isQuickWallet = args.isQuickWallet,
                            primaryKeyFlow = args.primaryKeyFlow,
                            passphrase = "",
                        )
                    )
                } else {
                    navigator.openSelectPhraseScreen(
                        requireActivity(),
                        mnemonic = event.mnemonic,
                        passphrase = args.passphrase,
                        primaryKeyFlow = args.primaryKeyFlow,
                        masterSignerId = viewModel.state.value.masterSignerId,
                        walletId = args.walletId,
                        groupId = args.groupId,
                        replacedXfp = args.replacedXfp
                    )
                }
            }
        }
    }

    private fun setupViews() {
        adapter = CreateNewSeedAdapter()
        binding.seedGrid.layoutManager = GridLayoutManager(requireContext(), COLUMNS)
        binding.seedGrid.adapter = adapter
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinueEvent() }
    }

    companion object {
        private const val COLUMNS = 3
    }

}