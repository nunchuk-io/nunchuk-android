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

package com.nunchuk.android.signer.tapsigner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.util.NFC_DEFAULT_NAME
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentAddNameKeyBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddNfcNameFragment : BaseChangeTapSignerNameFragment() {

    private val viewModel by viewModels<AddNfcNameViewModel>()

    private var _binding: FragmentAddNameKeyBinding? = null
    val keyBinding: FragmentAddNameKeyBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNameKeyBinding.inflate(inflater, container, false)
        return keyBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        registerEvents()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onUpdateNameSuccess(signer: MasterSigner) {
        val isReplaceKey = (activity as NfcSetupActivity).walletId.isNotEmpty()
        val isAddKeyToGroup = (activity as NfcSetupActivity).groupId.isNotEmpty()
        if (!isReplaceKey && !isAddKeyToGroup) {
            navigator.openSignerInfoScreen(
                activityContext = requireActivity(),
                isMasterSigner = true,
                id = signer.id,
                masterFingerprint = signer.device.masterFingerprint,
                name = signer.name,
                type = signer.type,
                justAdded = true
            )
        }
        requireActivity().finish()
    }

    override val signerName: String
        get() = keyBinding.signerName.getEditText()

    override val isMembershipFlow: Boolean = false

    private fun initViews() {
        keyBinding.signerName.getEditTextView()
            .setText(nfcViewModel.masterSigner?.name ?: NFC_DEFAULT_NAME)
        keyBinding.signerName.setMaxLength(20)
    }

    private fun registerEvents() {
        keyBinding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        keyBinding.signerName.addTextChangedCallback {
            keyBinding.nameCounter.text = "${it.length}/$MAX_LENGTH"
        }
        keyBinding.btnContinue.setOnClickListener {
            if (keyBinding.signerName.getEditText().isEmpty()) {
                keyBinding.signerName.setError(getString(R.string.nc_text_required))
                return@setOnClickListener
            }
            keyBinding.signerName.hideError()
            nfcViewModel.masterSigner?.let { masterSigner ->
                viewModel.updateName(masterSigner, keyBinding.signerName.getEditText())
            } ?: run {
                startNfcAddKeyFlow()
            }
        }
    }

    private fun startNfcAddKeyFlow() {
        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_ADD_KEY)
    }

    companion object {
        private const val MAX_LENGTH = 20
    }
}