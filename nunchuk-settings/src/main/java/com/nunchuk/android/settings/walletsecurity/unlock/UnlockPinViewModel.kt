package com.nunchuk.android.settings.walletsecurity.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.pin.DecoyPinExistUseCase
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
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val decoyPinExistUseCase: DecoyPinExistUseCase,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val sendSignOutUseCase: SendSignOutUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : ViewModel() {
    private val _state = MutableStateFlow(UnlockPinUiState())
    val state = _state.asStateFlow()
    private var walletPin: String = ""

    init {
        viewModelScope.launch {
            getWalletPinUseCase(Unit)
                .map { it.getOrDefault("") }
                .collect { pin ->
                    walletPin = pin
                }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .map { it.getOrDefault(WalletSecuritySetting()) }
                .collect { settings ->
                    _state.update { it.copy(walletSecuritySetting = settings) }
                }
        }
    }

    fun removePin(currentPin: String) {
        viewModelScope.launch {
            checkPin(currentPin) {
                createOrUpdateWalletPinUseCase("")
                    .onSuccess {
                        _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                    }
            }
        }
    }

    fun unlockPin(pin: String) {
        viewModelScope.launch {
            if (walletPin.isNotEmpty()) {
                if (decoyPinExistUseCase(pin).getOrElse { false }) {
                    signInModeHolder.clear()
                    clearInfoSessionUseCase(Unit)
                    sendSignOutUseCase(Unit)
                    initNunchukUseCase(
                        InitNunchukUseCase.Param(
                            passphrase = "",
                            accountId = "",
                            decoyPin = pin
                        )
                    ).onSuccess { replaced ->
                        signInModeHolder.setCurrentMode(SignInMode.GUEST_MODE)
                        if (replaced) {
                            _state.update { it.copy(event = UnlockPinEvent.GoToMain) }
                        } else {
                            _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                        }
                    }
                } else {
                    checkPin(pin) {
                        val account = accountManager.getAccount()
                        val accountId = if (account.loginType == SignInMode.PRIMARY_KEY.value) {
                            account.username
                        } else {
                            account.email
                        }
                        val mode = SignInMode.entries.find { it.value == account.loginType }
                            ?.takeIf { it != SignInMode.UNKNOWN }
                            ?: SignInMode.GUEST_MODE
                        signInModeHolder.setCurrentMode(mode)
                        initNunchukUseCase(InitNunchukUseCase.Param(accountId = accountId))
                        _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                    }
                }
            } else if (isWalletPasswordEnabled()) {
                confirmPassword(pin)
            } else if (isWalletPassphraseEnabled()) {
                confirmPassphrase(pin)
            }
        }
    }

    suspend fun confirmPassword(password: String) =
        viewModelScope.launch {
            if (password.isBlank()) {
                return@launch
            }
            verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    password = password, targetAction = TargetAction.PROTECT_WALLET.name
                )
            ).onSuccess {
                _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
            }.onFailure {
                _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
            }
        }


    private suspend fun confirmPassphrase(passphrase: String) =
        viewModelScope.launch {
            if (passphrase.isBlank()) {
                return@launch
            }
            verifiedPKeyTokenUseCase(passphrase)
                .onSuccess {
                    _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                }.onFailure {
                    _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
                }
        }

    private suspend fun checkPin(pin: String, onSuccess: suspend () -> Unit) {
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

    fun markEventHandled() {
        _state.update { it.copy(event = null) }
    }

    private fun isWalletPasswordEnabled() =
        signInModeHolder.getCurrentMode() == SignInMode.EMAIL && state.value.walletSecuritySetting.protectWalletPassword

    private fun isWalletPassphraseEnabled() =
        signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY && state.value.walletSecuritySetting.protectWalletPassphrase
}

data class UnlockPinUiState(
    val isFailed: Boolean = false,
    val attemptCount: Int = 0,
    val event: UnlockPinEvent? = null,
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting()
)

sealed class UnlockPinEvent {
    data object GoToMain : UnlockPinEvent()
    data object PinMatched : UnlockPinEvent()
}