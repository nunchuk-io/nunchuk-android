package com.nunchuk.android.settings.walletsecurity

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
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
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase
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
    }

    fun updateHideWalletDetail(hide: Boolean) = viewModelScope.launch {
        val walletSecuritySetting = getState().walletSecuritySetting
        val result = updateWalletSecuritySettingUseCase(walletSecuritySetting.copy(hide))
        if (result.isSuccess) {
            event(WalletSecuritySettingEvent.UpdateConfigSuccess)
        } else {
            event(WalletSecuritySettingEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}