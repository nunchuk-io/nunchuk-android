package com.nunchuk.android.settings.walletsecurity.unlock

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountInfo
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
import com.nunchuk.android.core.util.UnlockPinSourceFlow
import com.nunchuk.android.model.setting.BiometricConfig
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.usecase.GetBiometricConfigUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.pin.DecoyPinExistUseCase
import com.nunchuk.android.usecase.pin.SetCustomPinConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val accountManager: AccountManager,
    private val setCustomPinConfigUseCase: SetCustomPinConfigUseCase,
    private val getBiometricConfigUseCase: GetBiometricConfigUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(UnlockPinUiState())
    val state = _state.asStateFlow()
    private var walletPin: String = ""

    private val args = UnlockPinFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        viewModelScope.launch {
            getWalletPinUseCase(Unit)
                .map { it.getOrDefault("") }
                .collect { pin ->
                    walletPin = pin
                    _state.update { state ->
                        state.copy(
                            walletSecuritySetting = state.walletSecuritySetting.copy(
                                protectWalletPin = walletPin.isNotEmpty()
                            )
                        )
                    }
                }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .map { it.getOrDefault(WalletSecuritySetting.DEFAULT) }
                .collect { settings ->
                    _state.update { state ->
                        state.copy(
                            walletSecuritySetting = settings.copy(
                                protectWalletPin = state.walletSecuritySetting.protectWalletPin
                            )
                        )
                    }
                }
        }
        viewModelScope.launch {
            getBiometricConfigUseCase(Unit)
                .map { it.getOrDefault(BiometricConfig.DEFAULT) }
                .collect { result ->
                    _state.update { it.copy(biometricConfig = result) }
                }
        }
    }

    fun removePin(currentPin: String) {
        viewModelScope.launch {
            val account = accountManager.getAccount()
            if (account.decoyPin.isNotEmpty()) {
                // flow decoy space
                if (currentPin == account.decoyPin) {
                    setCustomPinConfigUseCase(
                        SetCustomPinConfigUseCase.Params(
                            decoyPin = account.decoyPin,
                            isEnable = false
                        )
                    )
                    _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                } else {
                    _state.update { it.copy(isFailed = true, attemptCount = it.attemptCount.inc()) }
                }
            } else {
                checkPin(currentPin) {
                    createOrUpdateWalletPinUseCase("")
                        .onSuccess {
                            _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                        }
                }
            }
        }
    }

    fun unlockPin(pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            if (walletPin.isNotEmpty()) {
                if (decoyPinExistUseCase(pin).getOrElse { false }) {
                    signInModeHolder.clear()
                    clearInfoSessionUseCase(Unit)
                    sendSignOutUseCase(Unit)
                    repeat(3) {
                        initNunchukUseCase(
                            InitNunchukUseCase.Param(
                                passphrase = "",
                                accountId = "",
                                decoyPin = pin
                            )
                        ).onSuccess { replaced ->
                            Timber.d("unlockPin: replaced $replaced")
                            accountManager.storeAccount(
                                AccountInfo(
                                    decoyPin = pin,
                                    loginType = SignInMode.GUEST_MODE.value
                                )
                            )
                            signInModeHolder.setCurrentMode(SignInMode.GUEST_MODE)
                            if (replaced) {
                                _state.update { it.copy(event = UnlockPinEvent.GoToMain) }
                            } else {
                                _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                            }
                            return@repeat
                        }.onFailure {
                            Timber.e("unlockPin failed: $it")
                        }
                        delay(1000L)
                    }
                } else {
                    checkPin(pin) {
                        if (args.sourceFlow == UnlockPinSourceFlow.SIGN_IN_UNKNOWN_MODE) {
                            _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                            return@checkPin
                        }
                        val account = accountManager.getAccount()
                        accountManager.storeAccount(
                            account.copy(decoyPin = "")
                        )
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
                        if (_state.value.biometricConfig.enabled && args.isRemovePin.not() && mode == SignInMode.EMAIL
                            && account.id.isNotEmpty() && account.id == _state.value.biometricConfig.userId){
                            _state.update { it.copy(showBiometricPrompt = true) }
                        } else {
                            _state.update { it.copy(event = UnlockPinEvent.PinMatched) }
                        }
                    }
                }
            } else if (isWalletPasswordEnabled()) {
                confirmPassword(pin)
            } else if (isWalletPassphraseEnabled()) {
                confirmPassphrase(pin)
            }
            _state.update { it.copy(isLoading = false) }
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

    fun signOut() {
        viewModelScope.launch {
            signInModeHolder.clear()
            clearInfoSessionUseCase.invoke(Unit)
            sendSignOutUseCase(Unit)
            _state.update { it.copy(event = UnlockPinEvent.GoToSignIn) }
        }
    }

    fun setShowBiometricPrompt(show: Boolean) {
        _state.update { it.copy(showBiometricPrompt = show) }
    }
}

data class UnlockPinUiState(
    val isLoading: Boolean = false,
    val isFailed: Boolean = false,
    val attemptCount: Int = 0,
    val event: UnlockPinEvent? = null,
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting.DEFAULT,
    val biometricConfig: BiometricConfig = BiometricConfig.DEFAULT,
    val showBiometricPrompt: Boolean = false,
)

sealed class UnlockPinEvent {
    data object GoToMain : UnlockPinEvent()
    data object PinMatched : UnlockPinEvent()
    data object GoToSignIn : UnlockPinEvent()
}