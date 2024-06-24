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

package com.nunchuk.android.signer.tapsigner.decryption

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentDecryptionKeyBinding
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.heightExtended
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcDecryptionKeyFragment : BaseFragment<FragmentDecryptionKeyBinding>() {
    private val viewModel by viewModels<NfcDecryptionKeyViewModel>()
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val uri: Uri by lazy(LazyThreadSafetyMode.NONE) { Uri.parse(requireArguments().getString(EXTRA_URI_STRING)) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDecryptionKeyBinding {
        return FragmentDecryptionKeyBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
        initViews()
        observer()
    }

    private fun observer() {
        flowObserver(viewModel.event) {
            showOrHideLoading(it is NfcDecryptionKeyEvent.Loading)
            when (it) {
                is NfcDecryptionKeyEvent.ImportTapSignerFailed -> showError(it.e)
                is NfcDecryptionKeyEvent.ImportTapSignerSuccess -> handleImportSuccess(it.masterSigner)
                else -> {}
            }
        }
    }

    private fun handleImportSuccess(masterSigner: MasterSigner) {
        nfcViewModel.updateMasterSigner(masterSigner)
        findNavController().navigate(R.id.addNfcNameFragment)
    }

    private fun showError(e: Throwable?) {
        if (e?.message.isNullOrEmpty().not()) {
            NCToastMessage(requireActivity()).showError(e?.message.orEmpty())
        }
    }

    private fun initViews() {
        binding.inputDecryptionKey.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_120))
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun registerEvents() {
        binding.btnContinue.setOnClickListener {
            if (binding.inputDecryptionKey.getEditText().isEmpty()) {
                binding.inputDecryptionKey.setError(getString(R.string.nc_text_required))
                return@setOnClickListener
            }
            binding.inputDecryptionKey.hideError()
            viewModel.decryptBackUpKey(
                backUpFileUri = uri,
                decryptionKey = binding.inputDecryptionKey.getEditText(),
                newIndex = (requireActivity() as NfcSetupActivity).signerIndex,
                walletId = (requireActivity() as NfcSetupActivity).walletId
            )
        }
    }

    companion object {
        private const val EXTRA_URI_STRING = "EXTRA_URI_STRING"
        fun buildArguments(uri: Uri) = Bundle().apply {
            putString(EXTRA_URI_STRING, uri.toString())
        }
    }
}