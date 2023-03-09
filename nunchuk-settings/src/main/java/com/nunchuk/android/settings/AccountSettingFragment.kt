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

package com.nunchuk.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isPrimaryKey
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.settings.AccountSettingEvent.*
import com.nunchuk.android.settings.databinding.FragmentAccountSettingBinding
import com.nunchuk.android.widget.NCDeleteConfirmationDialog
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountSettingFragment : BaseFragment<FragmentAccountSettingBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    @Inject
    lateinit var accountManager: AccountManager

    private val viewModel: AccountSettingViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAccountSettingBinding {
        return FragmentAccountSettingBinding.inflate(inflater, container, false)
    }

    private val isSignInPrimaryKey by lazy { signInModeHolder.getCurrentMode().isPrimaryKey() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
    }

    private fun handleEvent(event: AccountSettingEvent) {
        when (event) {
            Loading -> showLoading()
            is RequestDeleteError -> showRequestError(event.message)
            RequestDeleteSuccess -> handleRequestSuccess()
            DeletePrimaryKeySuccess -> {
                hideLoading()
                navigator.openSignInScreen(requireActivity(), isAccountDeleted = true)
            }
            is CheckNeedPassphraseSent -> {
                hideLoading()
                showEnterPassphraseDialog(event.isNeeded)
            }
        }
    }

    private fun handleRequestSuccess() {
        hideLoading()
        DeleteAccountActivity.start(requireActivity())
    }

    private fun showRequestError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).showError(message)
    }

    private fun setupViews() {
        binding.password.isVisible = isSignInPrimaryKey.not()
        binding.replacePrimaryKey.isVisible = isSignInPrimaryKey

        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
        binding.devices.setOnClickListener { navigator.openUserDevicesScreen(requireActivity()) }
        binding.enableSync.setOnClickListener {
            navigator.openSyncSettingScreen(requireActivity())
        }
        binding.password.setOnClickListener { navigator.openChangePasswordScreen(requireActivity()) }
        binding.delete.setOnClickListener {
            if (isSignInPrimaryKey) {
                if (accountManager.getPrimaryKeyInfo()?.xfp.isNullOrEmpty()) return@setOnClickListener
                showDeletePrimaryKeyConfirmation()
            } else {
                showDeleteAccountConfirmation()
            }
        }
        binding.replacePrimaryKey.setOnClickListener {
            if (accountManager.getPrimaryKeyInfo()?.xfp.isNullOrEmpty()) return@setOnClickListener
            navigator.openPrimaryKeyReplaceIntroScreen(requireActivity())
        }
        binding.signInQr.setOnDebounceClickListener {
            findNavController().navigate(
                AccountSettingFragmentDirections.actionAccountSettingFragmentToSignInQrFragment()
            )
        }
    }

    private fun showDeletePrimaryKeyConfirmation() {
        NCDeleteConfirmationDialog(requireActivity()).showDialog(
            message = getString(R.string.nc_delete_primary_key_desc),
            onConfirmed = {
                if (it.trim() == CONFIRMATION_TEXT) {
                    viewModel.checkNeedPassphraseSent()
                }
            })
    }

    private fun showDeleteAccountConfirmation() {
        NCDeleteConfirmationDialog(requireActivity()).showDialog(onConfirmed = {
            if (it.trim() == CONFIRMATION_TEXT) {
                viewModel.sendRequestDeleteAccount()
            }
        })
    }

    private fun showEnterPassphraseDialog(isNeeded: Boolean) {
        if (isNeeded) {
            NCInputDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_transaction_enter_passphrase),
                onConfirmed = {
                    viewModel.deletePrimaryKey(it)
                }
            )
        } else {
            viewModel.deletePrimaryKey("")
        }
    }

    companion object {
        private const val CONFIRMATION_TEXT = "DELETE"
    }
}