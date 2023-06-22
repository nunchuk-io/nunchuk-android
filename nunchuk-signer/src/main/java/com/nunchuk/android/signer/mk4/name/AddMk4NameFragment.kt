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

package com.nunchuk.android.signer.mk4.name

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentAddNameKeyBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddMk4NameFragment : BaseFragment<FragmentAddNameKeyBinding>() {
    private val viewModel by viewModels<AddMk4NameViewModel>()
    private val args by navArgs<AddMk4NameFragmentArgs>()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddNameKeyBinding {
        return FragmentAddNameKeyBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        registerEvents()
        observer()
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            when (it) {
                is AddNameMk4ViewEvent.CreateMk4SignerSuccess -> {
                    navigator.openSignerInfoScreen(
                        activityContext = requireActivity(),
                        id = it.signer.masterSignerId,
                        masterFingerprint = it.signer.masterFingerprint,
                        name = it.signer.name,
                        type = it.signer.type,
                        derivationPath = it.signer.derivationPath,
                        justAdded = true,
                    )
                    activity?.finish()
                }
                is AddNameMk4ViewEvent.Loading -> showOrHideLoading(it.isLoading)
                is AddNameMk4ViewEvent.ShowError -> showError(it.message)
            }
        }
    }

    private fun initViews() {
        binding.btnContinue.text = getString(R.string.nc_add_key)
        binding.signerName.getEditTextView().setText(getString(R.string.nc_my_coldcard))
        binding.signerName.setMaxLength(20)
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.signerName.addTextChangedCallback {
            binding.nameCounter.text = "${it.length}/$MAX_LENGTH"
        }
        binding.btnContinue.setOnClickListener {
            viewModel.createMk4Signer(args.signer.copy(name = binding.signerName.getEditText()))
        }
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}