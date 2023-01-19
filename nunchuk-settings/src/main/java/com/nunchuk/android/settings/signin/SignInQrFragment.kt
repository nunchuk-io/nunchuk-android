/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.settings.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.zxing.client.android.Intents
import com.nunchuk.android.core.base.BaseCameraFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.FragmentSignInQrBinding
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class SignInQrFragment : BaseCameraFragment<FragmentSignInQrBinding>() {
    private val viewModel: SignInQrViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSignInQrBinding {
        return FragmentSignInQrBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestCameraPermissionOrExecuteAction()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) {
            when(it) {
                is SignInQrEvent.Loading -> showOrHideLoading(it.isLoading)
                is SignInQrEvent.ShowError -> showError(it.error)
                is SignInQrEvent.TrySignInSuccess -> NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_confirmation),
                    message = getString(R.string.nc_confirm_qr_sign_in_msg),
                    onYesClick = {
                        viewModel.confirmSignIn(it.data)
                    },
                    onNoClick = {
                        viewModel.enableAcceptQr()
                    }
                )
                SignInQrEvent.ConfirmSignInSuccess -> {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    showSuccess(getString(R.string.nc_remote_sign_in_success))
                }
            }
        }
    }

    private fun setupViews() {
        val barcodeViewIntent = requireActivity().intent
        barcodeViewIntent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        binding.barcodeView.initializeFromIntent(barcodeViewIntent)
        binding.barcodeView.decodeContinuous {
            viewModel.trySignIn(it.text)
        }
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    override fun onCameraPermissionGranted(fromUser: Boolean) {
        // Do nothing
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }
}
