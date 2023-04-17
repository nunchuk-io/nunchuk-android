package com.nunchuk.android.settings.walletsecurity

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateWalletPinUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.UpdateWalletSecuritySettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class WalletSecuritySettingViewModel @Inject constructor(
    private val updateWalletSecuritySettingUseCase: UpdateWalletSecuritySettingUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val createOrUpdateWalletPinUseCase: CreateOrUpdateWalletPinUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder
) : NunchukViewModel<WalletSecuritySettingState, WalletSecuritySettingEvent>() {

    override val initialState = WalletSecuritySettingState()

    init {
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    updateState {
                        copy(
                            walletSecuritySetting = it.getOrNull() ?: WalletSecuritySetting()
                        )
                    }
                }
        }
        viewModelScope.launch {
            getWalletPinUseCase(Unit).collect {
                updateState { copy(walletPin = it.getOrDefault("")) }
            }
        }

        viewModelScope.launch {
            if (signInModeHolder.getCurrentMode() == SignInMode.PRIMARY_KEY) {
                val enablePassphrase = primaryKeySignerInfoHolder.isNeedPassphraseSent()
                updateState { copy(isEnablePassphrase = enablePassphrase ) }
            }
        }
    }

    fun updateHideWalletDetail(forceUpdate: Boolean = false) = viewModelScope.launch {
        val walletSecuritySetting = getState().walletSecuritySetting
        val hideWalletDetail =
            if (forceUpdate) true else walletSecuritySetting.hideWalletDetail.not()
        updateSetting(walletSecuritySetting.copy(hideWalletDetail = hideWalletDetail))
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

    fun getWalletPin() = getState().walletPin

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

    fun checkWalletPin(input: String, isHideWalletDetailFlow: Boolean = false) =
        viewModelScope.launch {
            val match = checkWalletPinUseCase(input).getOrDefault(false)
            if (isHideWalletDetailFlow) {
                if (match.not()) updateHideWalletDetail(true)
            } else {
                updateProtectWalletPin(match.not())
            }
            event(WalletSecuritySettingEvent.CheckWalletPin(match, isHideWalletDetailFlow))
        }

    fun confirmPassword(password: String, isHideWalletDetailFlow: Boolean = false) =
        viewModelScope.launch {
            if (password.isBlank() && isHideWalletDetailFlow.not()) {
                updateProtectWalletPassword(true)
                return@launch
            }
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    password = password,
                    targetAction = VerifiedPasswordTargetAction.PROTECT_WALLET.name
                )
            )
            if (result.isSuccess) {
                if (isHideWalletDetailFlow) {
                    event(WalletSecuritySettingEvent.CheckPasswordSuccess)
                } else {
                    updateProtectWalletPassword(false)
                }
            } else {
                if (isHideWalletDetailFlow.not()) updateProtectWalletPassword(true) else updateHideWalletDetail(true)
                event(WalletSecuritySettingEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
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
                if (isHideWalletDetailFlow.not()) updateProtectWalletPassphrase(true) else updateHideWalletDetail(true)
                event(WalletSecuritySettingEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
}