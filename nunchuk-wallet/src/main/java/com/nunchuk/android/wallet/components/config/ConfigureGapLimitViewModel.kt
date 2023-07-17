package com.nunchuk.android.wallet.components.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.type.Chain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigureGapLimitViewModel @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<ConfigureGapLimitEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            val result = getAppSettingUseCase(Unit)
            if (result.isSuccess) {
                val appSettings = result.getOrThrow()
                val url = when (appSettings.chain) {
                    Chain.MAIN -> appSettings.mainnetServers[0]
                    else -> ""
                }
                _event.emit(
                    ConfigureGapLimitEvent.GetAppSettingSuccess(url)
                )
            }
        }
    }
}

sealed class ConfigureGapLimitEvent {
    data class GetAppSettingSuccess(val url: String) : ConfigureGapLimitEvent()
}