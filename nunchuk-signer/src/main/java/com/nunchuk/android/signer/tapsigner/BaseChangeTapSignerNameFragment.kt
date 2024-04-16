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

import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.NCInfoDialog
import kotlinx.coroutines.flow.filter


abstract class BaseChangeTapSignerNameFragment : MembershipFragment() {
    protected val nfcViewModel by activityViewModels<NfcViewModel>()
    protected val nameNfcViewModel by viewModels<AddNfcNameViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
    }

    private fun observer() {
        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_NFC_ADD_KEY }
                    .collect {
                        nameNfcViewModel.addNameForNfcKey(
                            isoDep = IsoDep.get(it.tag),
                            cvc = nfcViewModel.inputCvc.orEmpty(),
                            name = signerName,
                            shouldCreateBackUp = isMembershipFlow,
                        )
                        nfcViewModel.clearScanInfo()
                    }
            }
        }

        lifecycleScope.launchWhenCreated {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nameNfcViewModel.event.collect { event ->
                    when (event) {
                        is AddNfcNameEvent.BackUpSuccess -> onBackUpFileReady(event.filePath)
                        is AddNfcNameEvent.Error -> if (
                            nfcViewModel.handleNfcError(event.e).not()
                        ) {
                            showError(event.e?.message?.orUnknownError().orEmpty())
                        }
                        is AddNfcNameEvent.Loading -> showOrHideLoading(
                            event.isLoading, message = getString(R.string.nc_keep_holding_nfc)
                        )
                        is AddNfcNameEvent.Success -> onUpdateNameSuccess(event.masterSigner)
                        is AddNfcNameEvent.UpdateError -> showError(event.e?.message.orEmpty())

                        is AddNfcNameEvent.SignerExist -> {
                            NCInfoDialog(requireActivity())
                                .showDialog(
                                    message = String.format(getString(R.string.nc_existing_key_is_software_key_delete_key), event.cardIdent),
                                    btnYes = getString(R.string.nc_text_yes),
                                    btnInfo = getString(R.string.nc_text_no),
                                    onYesClick = {
                                        nameNfcViewModel.setReplaceKeyFlow(true)
                                        (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_NFC_ADD_KEY)
                                    },
                                    onInfoClick = {}
                                )
                        }
                    }
                }
            }
        }
    }

    open fun onBackUpFileReady(path: String) {}
    open fun onUpdateNameSuccess(signer: MasterSigner) {}
    abstract val signerName: String
    abstract val isMembershipFlow: Boolean
}