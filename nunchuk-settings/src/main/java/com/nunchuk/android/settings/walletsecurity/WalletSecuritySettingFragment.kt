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

package com.nunchuk.android.settings.walletsecurity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.FragmentWalletSecuritySettingBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletSecuritySettingFragment : BaseFragment<FragmentWalletSecuritySettingBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    private val viewModel: WalletSecuritySettingViewModel by viewModels()

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWalletSecuritySettingBinding {
        return FragmentWalletSecuritySettingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeEvent()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkCustomPinConfig()
    }

    private fun observeEvent() {
        viewModel.event.observe(viewLifecycleOwner, ::handleEvent)
        viewModel.state.observe(viewLifecycleOwner, ::handleState)
    }

    private fun handleState(state: WalletSecuritySettingState) {
        binding.hideWalletDetailOption.setOptionChecked(state.walletSecuritySetting.hideWalletDetail)
        binding.passwordOption.setOptionChecked(state.walletSecuritySetting.protectWalletPassword)
        binding.pinStatus.text =
            if (state.isAppPinEnable && state.isCustomPinEnable) getString(R.string.nc_on) else getString(R.string.nc_off)
        binding.passphraseOption.setOptionChecked(state.walletSecuritySetting.protectWalletPassphrase)
        binding.passwordOption.isVisible = signInModeHolder.getCurrentMode() == SignInMode.EMAIL
        binding.passphraseOption.isVisible =
            signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY
        binding.passphraseOption.enableSwitchButton(state.isEnablePassphrase)
    }

    private fun handleEvent(event: WalletSecuritySettingEvent) {
        when (event) {
            is WalletSecuritySettingEvent.Error -> NCToastMessage(requireActivity()).showError(
                message = event.message
            )

            is WalletSecuritySettingEvent.Loading -> showOrHideLoading(loading = event.loading)
            WalletSecuritySettingEvent.UpdateConfigSuccess -> {

            }

            is WalletSecuritySettingEvent.CheckWalletPin -> {
                if (event.match.not()) {
                    NCToastMessage(requireActivity()).showError(
                        message = getString(R.string.nc_incorrect_current_pin)
                    )
                } else if (event.isHideWalletDetailFlow) {
                    viewModel.updateHideWalletDetail()
                }
            }

            WalletSecuritySettingEvent.None -> {}
            WalletSecuritySettingEvent.CheckPasswordSuccess, WalletSecuritySettingEvent.CheckPassphraseSuccess -> {
                if (viewModel.getWalletSecuritySetting().protectWalletPin && viewModel.isAppPinEnable()) {
                    showInputPinDialog(true)
                } else {
                    viewModel.updateHideWalletDetail()
                }
            }
        }
        viewModel.clearEvent()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
        binding.hideWalletDetailOption.setOptionChangeListener {
            if (it.not()) {
                checkWalletSecurity()
            } else {
                viewModel.updateHideWalletDetail()
            }
        }
        binding.passwordOption.setOptionChangeListener {
            if (it.not()) {
                enterPasswordDialog(false)
            } else if (viewModel.isAppPinEnable()) {
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_text_confirmation),
                    message = getString(R.string.nc_disable_pin_warning),
                    btnNo = getString(R.string.nc_cancel),
                    btnYes = getString(R.string.nc_text_continue),
                    onYesClick = {
                        lifecycleScope.launch {
                            viewModel.updateProtectWalletPin(false).join()
                            viewModel.updateProtectWalletPassword(it)
                        }
                    }
                )
            } else {
                viewModel.updateProtectWalletPassword(it)
            }
        }
        binding.passphraseOption.setOptionChangeListener {
            if (it.not()) {
                enterPassphraseDialog(false)
            } else if (viewModel.isAppPinEnable()) {
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_text_confirmation),
                    message = getString(R.string.nc_disable_pin_warning),
                    btnNo = getString(R.string.nc_cancel),
                    btnYes = getString(R.string.nc_text_continue),
                    onYesClick = {
                        lifecycleScope.launch {
                            viewModel.updateProtectWalletPin(false).join()
                            viewModel.updateProtectWalletPassphrase(it)
                        }
                    }
                )
            } else {
                viewModel.updateProtectWalletPassphrase(it)
            }
        }
        binding.pinOption.setOnClickListener {
            findNavController().navigate(R.id.pinStatusFragment)
        }
    }

    private fun checkWalletSecurity() {
        if (viewModel.getWalletSecuritySetting().protectWalletPassword) {
            enterPasswordDialog(true)
        } else if (viewModel.getWalletSecuritySetting().protectWalletPassphrase) {
            enterPassphraseDialog(true)
        } else if (viewModel.getWalletSecuritySetting().protectWalletPin && viewModel.isAppPinEnable()) {
            showInputPinDialog(true)
        } else {
            viewModel.updateHideWalletDetail()
        }
    }

    private fun showInputPinDialog(isHideWalletDetailFlow: Boolean) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_pin),
            onCanceled = {
                viewModel.updateProtectWalletPin(true)
            },
            onConfirmed = {
                viewModel.checkWalletPin(it, isHideWalletDetailFlow)
            }
        )
    }

    private fun enterPasswordDialog(isHideWalletDetailFlow: Boolean) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_password),
            descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
            onCanceled = {
                viewModel.updateProtectWalletPassword(true)
            },
            onConfirmed = {
                viewModel.confirmPassword(it, isHideWalletDetailFlow)
            }
        )
    }

    private fun enterPassphraseDialog(isHideWalletDetailFlow: Boolean) {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_passphrase),
            descMessage = getString(R.string.nc_re_enter_your_passphrase_dialog_desc),
            onCanceled = {
                viewModel.updateProtectWalletPassphrase(true)
            },
            onConfirmed = {
                viewModel.confirmPassphrase(it, isHideWalletDetailFlow)
            }
        )
    }
}