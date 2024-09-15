package com.nunchuk.android.settings.walletsecurity.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockPinViewModel @Inject constructor(
    private val createOrUpdateWalletPinUseCase: CreateOrUpdateWalletPinUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase
) : ViewModel() {
    init {
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .map { it.getOrDefault(WalletSecuritySetting()) }
                .collect { settings ->
                    _state.update { it.copy(walletSecuritySetting = settings) }
                }
        }
    }

    private val _state = MutableStateFlow(UnlockPinUiState())
    val state = _state.asStateFlow()

    fun removePin(currentPin: String) {
        checkPin(currentPin) {
            createOrUpdateWalletPinUseCase("")
                .onSuccess {
                    _state.update { it.copy(isSuccess = true) }
                }
        }
    }

    fun unlockPin(pin: String) {
        if (isWalletPasswordEnabled()) {
            confirmPassword(pin)
        } else if (isWalletPassphraseEnabled()) {
            confirmPassphrase(pin)
        } else {
            checkPin(pin) {
                _state.update { it.copy(isSuccess = true) }
            }
        }
    }

    fun confirmPassword(password: String) =
        viewModelScope.launch {
            if (password.isBlank()) {
                return@launch
            }
            verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    password = password, targetAction = TargetAction.PROTECT_WALLET.name
                )
            ).onSuccess {
                _state.update { it.copy(isSuccess = true) }
            }.onFailure {
                _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
            }
        }


    fun confirmPassphrase(passphrase: String) =
        viewModelScope.launch {
            if (passphrase.isBlank()) {
                return@launch
            }
            verifiedPKeyTokenUseCase(passphrase)
                .onSuccess {
                    _state.update { it.copy(isSuccess = true) }
                }.onFailure {
                    _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
                }
        }

    private fun checkPin(pin: String, onSuccess: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isFailed = false) }
            checkWalletPinUseCase(pin)
                .onSuccess {
                    if (it) {
                        onSuccess()
                    } else {
                        _state.update { state ->
                            state.copy(
                                isFailed = true,
                                attemptCount = state.attemptCount.inc()
                            )
                        }
                    }
                }.onFailure {
                    _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
                }
        }
    }

    private fun isWalletPasswordEnabled() =
        signInModeHolder.getCurrentMode() == SignInMode.EMAIL && state.value.walletSecuritySetting.protectWalletPassword

    private fun isWalletPassphraseEnabled() =
        signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY && state.value.walletSecuritySetting.protectWalletPassphrase
}

data class UnlockPinUiState(
    val isFailed: Boolean = false,
    val attemptCount: Int = 0,
    val isSuccess: Boolean = false,
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting()
)