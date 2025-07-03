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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.domain.BiometricRegisterUseCase
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CheckHasPassphrasePrimaryKeyUseCase
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.domain.membership.RequestFederatedTokenUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
// VerifiedPasswordTokenUseCase removed - password verification now handled by PasswordVerificationHelper
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.setting.BiometricConfig
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetBiometricConfigUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.UpdateBiometricConfigUseCase
import com.nunchuk.android.usecase.UpdateWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.pin.GetCustomPinConfigFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class WalletSecuritySettingViewModel @Inject constructor(
    private val updateWalletSecuritySettingUseCase: UpdateWalletSecuritySettingUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val requestFederatedTokenUseCase: RequestFederatedTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val createOrUpdateWalletPinUseCase: CreateOrUpdateWalletPinUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val checkHasPassphrasePrimaryKeyUseCase: CheckHasPassphrasePrimaryKeyUseCase,
    private val getCustomPinConfigFlowUseCase: GetCustomPinConfigFlowUseCase,
    private val accountManager: AccountManager,
    private val getBiometricConfigUseCase: GetBiometricConfigUseCase,
    private val updateBiometricConfigUseCase: UpdateBiometricConfigUseCase,
    private val biometricRegisterUseCase: BiometricRegisterUseCase
) : NunchukViewModel<WalletSecuritySettingState, WalletSecuritySettingEvent>() {
    private var customPinJob: Job? = null
    override val initialState = WalletSecuritySettingState()

    // Hold the latest biometric config for UI logic
    private var biometricConfig: BiometricConfig? = null

    init {
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    updateState {
                        copy(
                            walletSecuritySetting = it.getOrNull() ?: WalletSecuritySetting.DEFAULT
                        )
                    }
                }
        }
        viewModelScope.launch {
            getWalletPinUseCase(Unit).map { it.getOrDefault("").isNotBlank() }
                .catch { Timber.e(it) }
                .collect {
                    updateState { copy(isAppPinEnable = it) }
                }
        }

        viewModelScope.launch {
            if (signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY) {
                val enablePassphrase = checkHasPassphrasePrimaryKeyUseCase(Unit)
                updateState { copy(isEnablePassphrase = enablePassphrase.getOrDefault(false)) }
            }
        }

        viewModelScope.launch {
            getBiometricConfigUseCase(Unit)
                .map { it.getOrDefault(BiometricConfig.DEFAULT) }
                .collect { result ->
                    biometricConfig = result
                    updateState { copy(isEnableBiometric = result.enabled) }
                }
        }
    }

    fun checkCustomPinConfig() {
        customPinJob?.cancel()
        customPinJob = viewModelScope.launch {
            getCustomPinConfigFlowUseCase(accountManager.getAccount().decoyPin)
                .map { it.getOrDefault(true) }
                .catch { Timber.e(it) }
                .collect {
                    updateState { copy(isCustomPinEnable = it) }
                }
        }
    }

    fun updateHideWalletDetail(forceUpdate: Boolean = false) = viewModelScope.launch {
        val walletSecuritySetting = getState().walletSecuritySetting
        val hideWalletDetail =
            if (forceUpdate) true else walletSecuritySetting.hideWalletDetail.not()
        updateSetting(walletSecuritySetting.copy(hideWalletDetail = hideWalletDetail))
    }

    fun updateProtectWalletBiometric(enable: Boolean, privateKey: String = "") =
        viewModelScope.launch {
            val config = if (enable) {
                BiometricConfig(
                    enabled = true,
                    userId = accountManager.getAccount().id,
                    privateKey = privateKey,
                    email = accountManager.getAccount().email
                )
            } else {
                BiometricConfig.DEFAULT
            }
            updateBiometricConfigUseCase(config)
                .onSuccess {
                    updateState { copy(isEnableBiometric = enable) }
                }
        }

    fun updateProtectWalletPassword(data: Boolean) = viewModelScope.launch {
        val walletSecuritySetting = getState().walletSecuritySetting
        updateSetting(walletSecuritySetting.copy(protectWalletPassword = data))
    }

    fun updateProtectWalletPassphrase(data: Boolean) = viewModelScope.launch {
        val walletSecuritySetting = getState().walletSecuritySetting
        updateSetting(walletSecuritySetting.copy(protectWalletPassphrase = data))
    }

    fun updateProtectWalletPin(data: Boolean) = viewModelScope.launch {
        val walletSecuritySetting = getState().walletSecuritySetting
        updateSetting(walletSecuritySetting.copy(protectWalletPin = data))
        if (data.not()) {
            createOrUpdateWalletPinUseCase("")
        }
    }

    fun getWalletSecuritySetting() = getState().walletSecuritySetting

    fun clearEvent() = event(WalletSecuritySettingEvent.None)

    private suspend fun updateSetting(walletSecuritySetting: WalletSecuritySetting) {
        val result = updateWalletSecuritySettingUseCase(walletSecuritySetting)
        if (result.isSuccess) {
            updateState { copy(walletSecuritySetting = walletSecuritySetting) }
            event(WalletSecuritySettingEvent.UpdateConfigSuccess)
        } else {
            event(WalletSecuritySettingEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun checkWalletPin(input: String) =
        viewModelScope.launch {
            val match = checkWalletPinUseCase(input).getOrDefault(false)
            if (match.not()) updateHideWalletDetail(true)
            event(WalletSecuritySettingEvent.CheckWalletPin(match, true))
        }

    fun registerBiometric(password: String) {
        viewModelScope.launch {
            if (password.isBlank()) {
                return@launch
            }

            biometricRegisterUseCase(
                BiometricRegisterUseCase.Param(
                    password
                )
            )
                .onSuccess {
                    updateProtectWalletBiometric(true, it)
                }.onFailure {
                    updateProtectWalletBiometric(false)
                    event(WalletSecuritySettingEvent.Error(message = it.message.orUnknownError()))
                }
        }
    }

    fun confirmPassphrase(passphrase: String, isHideWalletDetailFlow: Boolean = false) =
        viewModelScope.launch {
            if (passphrase.isBlank() && isHideWalletDetailFlow.not()) {
                updateProtectWalletPassphrase(true)
                return@launch
            }
            val result = verifiedPKeyTokenUseCase(passphrase)
            if (result.isSuccess) {
                if (isHideWalletDetailFlow) {
                    event(WalletSecuritySettingEvent.CheckPassphraseSuccess)
                } else {
                    updateProtectWalletPassphrase(false)
                }
            } else {
                if (isHideWalletDetailFlow.not()) updateProtectWalletPassphrase(true) else updateHideWalletDetail(
                    true
                )
                event(WalletSecuritySettingEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
            }
        }

    fun requestFederatedToken(isResend: Boolean) = viewModelScope.launch {
        val result = requestFederatedTokenUseCase(
            RequestFederatedTokenUseCase.Param(
                targetAction = TargetAction.REGISTER_BIOMETRIC_PUBLIC_KEY.name
            )
        )
        if (result.isSuccess) {
            if (isResend) return@launch
            event(WalletSecuritySettingEvent.RequestFederatedTokenSuccess(email = accountManager.getAccount().email))
        } else {
            updateProtectWalletPassphrase(false)
            event(WalletSecuritySettingEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun isAppPinEnable() = getState().isAppPinEnable

    // Expose current biometric config for UI logic
    fun getCurrentBiometricConfig(): BiometricConfig? {
        return biometricConfig
    }

    // Expose current account id for UI logic
    fun getCurrentAccountId(): String? {
        return accountManager.getAccount().id
    }
}