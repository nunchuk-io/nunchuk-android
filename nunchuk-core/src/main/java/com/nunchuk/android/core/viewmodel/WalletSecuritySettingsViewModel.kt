package com.nunchuk.android.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletSecuritySettingsViewModel @Inject constructor(
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val accountManager: AccountManager
) : ViewModel() {

    private var walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting()

    private val _event = MutableSharedFlow<WalletSecuritySettingsEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    walletSecuritySetting = it.getOrNull() ?: WalletSecuritySetting()
                }
        }
    }

    fun confirmPassword(password: String, walletId: String) =
        viewModelScope.launch {
            if (password.isBlank()) {
                return@launch
            }
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    password = password, targetAction = TargetAction.PROTECT_WALLET.name
                )
            )
            if (result.isSuccess) {
                _event.emit(WalletSecuritySettingsEvent.DoNextAction(walletId))
            } else {
                _event.emit(WalletSecuritySettingsEvent.ShowError(result.exceptionOrNull()))
            }
        }


    fun confirmPassphrase(passphrase: String, walletId: String) =
        viewModelScope.launch {
            if (passphrase.isBlank()) {
                return@launch
            }
            val result = verifiedPKeyTokenUseCase(passphrase)
            if (result.isSuccess) {
                _event.emit(WalletSecuritySettingsEvent.DoNextAction(walletId))
            } else {
                _event.emit(WalletSecuritySettingsEvent.ShowError(result.exceptionOrNull()))
            }
        }

    fun isWalletPasswordEnabled() =
        accountManager.loginType() == SignInMode.EMAIL.value && walletSecuritySetting.protectWalletPassword

    fun isWalletPassphraseEnabled() =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && walletSecuritySetting.protectWalletPassphrase
}

sealed class WalletSecuritySettingsEvent {
    data class DoNextAction(val walletId: String) : WalletSecuritySettingsEvent()
    data class OpenWalletDetailsScreen(val walletId: String) : WalletSecuritySettingsEvent()
    data class ShowError(val throwable: Throwable?) : WalletSecuritySettingsEvent()
    data object InvalidPin : WalletSecuritySettingsEvent()
}