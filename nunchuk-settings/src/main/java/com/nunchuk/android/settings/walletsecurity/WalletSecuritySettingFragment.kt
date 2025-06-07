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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.biometric.BiometricPromptManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.setting.BiometricConfig
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.FragmentWalletSecuritySettingBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import com.nunchuk.android.widget.NCWarningVerticalDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WalletSecuritySettingFragment : BaseFragment<FragmentWalletSecuritySettingBinding>() {

    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    private val viewModel: WalletSecuritySettingViewModel by viewModels()

    // Add variable to track biometric operation intent
    private var isTurningOnBiometric = true

    private val enrollLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

        }

    private val biometricPromptManager: BiometricPromptManager by lazy {
        BiometricPromptManager(requireActivity())
    }

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


        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                biometricPromptManager.promptResults.collect { result ->
                    when (result) {
                        is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                            if (isTurningOnBiometric) {
                                viewModel.requestFederatedToken(false)
                            } else {
                                // Authentication successful, turn off biometric protection
                                viewModel.updateProtectWalletBiometric(false)
                            }
                        }
                        is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                            if (isTurningOnBiometric) {
                                viewModel.updateProtectWalletBiometric(false)
                                NCToastMessage(requireActivity()).showError(message = result.error)
                            } else {
                                // Authentication failed, keep biometric option on
                                binding.protectWalletFingerprintOption.setOptionChecked(true)
                                NCToastMessage(requireActivity()).showError(message = result.error)
                            }
                        }
                        is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                            if (!isTurningOnBiometric) {
                                // Authentication failed, keep biometric option on
                                binding.protectWalletFingerprintOption.setOptionChecked(true)
                            }
                        }
                        else -> {
                            if (!isTurningOnBiometric) {
                                // Any other result (like cancelled), keep biometric option on
                                binding.protectWalletFingerprintOption.setOptionChecked(true)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleState(state: WalletSecuritySettingState) {
        binding.passwordOption.setOptionChecked(state.walletSecuritySetting.protectWalletPassword)
        binding.pinStatus.text =
            if (state.isAppPinEnable && state.isCustomPinEnable) getString(R.string.nc_on) else getString(
                R.string.nc_off
            )
        binding.passphraseOption.setOptionChecked(state.walletSecuritySetting.protectWalletPassphrase)
        binding.passwordOption.isVisible = signInModeHolder.getCurrentMode() == SignInMode.EMAIL
        binding.passphraseOption.isVisible =
            signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY
        binding.passphraseOption.enableSwitchButton(state.isEnablePassphrase)

        val biometricConfig = viewModel.getCurrentBiometricConfig()
        val isCurrentAccount = biometricConfig?.userId == viewModel.getCurrentAccountId()
        val isDefaultBiometric = biometricConfig == BiometricConfig.DEFAULT

        binding.protectWalletFingerprintOption.setOptionChecked(isCurrentAccount && biometricConfig?.enabled == true)

        // Enable if DEFAULT or current account has biometric enabled, otherwise disable
        binding.protectWalletFingerprintOption.setEnable(
            isDefaultBiometric || (isCurrentAccount && biometricConfig?.enabled == true)
        )

        if (
            biometricPromptManager.checkHardwareSupport().not() ||
            signInModeHolder.getCurrentMode() == SignInMode.GUEST_MODE ||
            signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY
        ) {
            binding.protectWalletFingerprintOption.setEnable(false)
        }
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
                    showInputPinDialog()
                } else {
                    viewModel.updateHideWalletDetail()
                }
            }

            WalletSecuritySettingEvent.ShowBiometric -> {
                biometricPromptManager.showBiometricPrompt()
            }

            is WalletSecuritySettingEvent.RequestFederatedTokenSuccess -> {
                NCInputDialog(requireContext()).showDialog(
                    title = getString(R.string.nc_enter_confirmation_code),
                    descMessage = String.format(
                        getString(R.string.nc_enter_confirmation_code_desc),
                        event.email
                    ),
                    inputBoxTitle = getString(R.string.nc_confirmation_code),
                    clickablePhrases = listOf(
                        "Resend code" to {
                            viewModel.requestFederatedToken(true)
                        },
                    ),
                    confirmText = getString(R.string.nc_text_continue),
                    onConfirmed = {
                        viewModel.registerBiometric(it)
                    },
                    onCanceled = {
                        viewModel.updateProtectWalletBiometric(false)
                    }
                )
            }
        }
        viewModel.clearEvent()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { requireActivity().finish() }
        binding.passwordOption.setOptionChangeListener { it ->
            if (it.not()) {
                NCInputDialog(requireContext()).showDialog(
                    title = getString(R.string.nc_re_enter_your_password),
                    descMessage = getString(R.string.nc_re_enter_your_password_dialog_desc),
                    onCanceled = {
                        viewModel.updateProtectWalletPassword(true)
                    },
                    onConfirmed = {
                        viewModel.confirmPassword(it)
                    }
                )
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
                enterPassphraseDialog()
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
        binding.protectWalletFingerprintOption.setOptionChangeListener {
            if (it) {
                isTurningOnBiometric = true
                if (biometricPromptManager.checkDeviceHasBiometricEnrolled().not()) {
                    NCWarningVerticalDialog(requireActivity()).showDialog(
                        title = getString(R.string.nc_fingerprint_not_set_up_yet),
                        message = getString(R.string.nc_fingerprint_not_set_up_yet_desc),
                        btnYes = getString(R.string.nc_try_again),
                        btnNo = getString(R.string.nc_go_settings),
                        btnNeutral = getString(R.string.nc_text_cancel),
                        onNoClick = {
                            biometricPromptManager.enrollBiometric(enrollLauncher)
                        },
                        onYesClick = {
                            binding.protectWalletFingerprintOption.setOptionChecked(false)
                        },
                        onNeutralClick = {
                            binding.protectWalletFingerprintOption.setOptionChecked(false)
                        })
                    return@setOptionChangeListener
                }
                NCWarningDialog(requireActivity()).showDialog(
                    title = getString(R.string.nc_do_you_want_allow_use_fingerprint),
                    message = getString(R.string.nc_do_you_want_allow_use_fingerprint_desc),
                    btnYes = getString(R.string.nc_allow),
                    btnNo = getString(R.string.nc_do_not_allow),
                    onYesClick = {
                        biometricPromptManager.showBiometricPrompt()
                    },
                    onNoClick = {
                        binding.protectWalletFingerprintOption.setOptionChecked(false)
                    }
                )
            } else {
                isTurningOnBiometric = false
                biometricPromptManager.showBiometricPrompt()
            }
        }
    }

    private fun showInputPinDialog() {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_enter_your_pin),
            onCanceled = {
                viewModel.updateProtectWalletPin(true)
            },
            onConfirmed = {
                viewModel.checkWalletPin(it)
            }
        )
    }

    private fun enterPassphraseDialog() {
        NCInputDialog(requireContext()).showDialog(
            title = getString(R.string.nc_re_enter_your_passphrase),
            descMessage = getString(R.string.nc_re_enter_your_passphrase_dialog_desc),
            onCanceled = {
                viewModel.updateProtectWalletPassphrase(true)
            },
            onConfirmed = {
                viewModel.confirmPassphrase(it)
            }
        )
    }
}