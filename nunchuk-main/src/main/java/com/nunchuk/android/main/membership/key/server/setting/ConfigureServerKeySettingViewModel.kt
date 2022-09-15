package com.nunchuk.android.main.membership.key.server.setting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CreateServerKeysUseCase
import com.nunchuk.android.core.domain.membership.UpdateServerKeysUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.signer.util.SERVER_KEY_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigureServerKeySettingViewModel @Inject constructor(
    private val createServerKeysUseCase: CreateServerKeysUseCase,
    private val updateServerKeysUseCase: UpdateServerKeysUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args: ConfigureServerKeySettingFragmentArgs =
        ConfigureServerKeySettingFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<ConfigureServerKeySettingEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ConfigureServerKeySettingState.Empty)
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                autoBroadcastSwitched = args.keyPolicy?.autoBroadcastTransaction ?: false,
                cosigningText = args.keyPolicy?.signingDelayInHour?.toString().orEmpty(),
                enableCoSigningSwitched = (args.keyPolicy?.signingDelayInHour ?: 0) > 0
            )
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        val state = _state.value
        if (state.enableCoSigningSwitched && state.cosigningText.isEmpty()) {
            _event.emit(ConfigureServerKeySettingEvent.NoDelayInput)
            return@launch
        }
        val signingDelayInHour =
            if (state.enableCoSigningSwitched) (state.cosigningText.toIntOrNull() ?: 0) else 0
        if (signingDelayInHour !in 0..MAX_DELAY_IN_HOUR) {
            _event.emit(ConfigureServerKeySettingEvent.DelaySigningInHourInvalid)
            return@launch
        }
        _event.emit(ConfigureServerKeySettingEvent.Loading(true))
        val result = if (args.xfp.isNullOrEmpty()) {
            createServerKeysUseCase(
                CreateServerKeysUseCase.Param(
                    name = SERVER_KEY_NAME, keyPolicy = KeyPolicy(
                        autoBroadcastTransaction = state.autoBroadcastSwitched,
                        signingDelayInHour = signingDelayInHour
                    )
                )
            )
        } else {
            updateServerKeysUseCase(
                UpdateServerKeysUseCase.Param(
                    name = SERVER_KEY_NAME,
                    keyPolicy = KeyPolicy(
                        autoBroadcastTransaction = state.autoBroadcastSwitched,
                        signingDelayInHour = signingDelayInHour
                    ),
                    keyIdOrXfp = args.xfp.orEmpty()
                )
            )
        }
        _event.emit(ConfigureServerKeySettingEvent.Loading(false))
        if (result.isSuccess) {
            _event.emit(ConfigureServerKeySettingEvent.ConfigServerSuccess(result.getOrThrow()))
        } else {
            _event.emit(ConfigureServerKeySettingEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun updateCoSigningDelayText(cosigningText: String) {
        _state.update {
            it.copy(cosigningText = cosigningText.take(MAX_INPUT_HOUR_LENGTH))
        }
    }

    fun updateAutoBroadcastSwitched(autoBroadcastSwitched: Boolean) {
        _state.update {
            it.copy(autoBroadcastSwitched = autoBroadcastSwitched)
        }
    }

    fun updateEnableCoSigningSwitched(enableCoSigningSwitched: Boolean) {
        _state.update {
            it.copy(enableCoSigningSwitched = enableCoSigningSwitched)
        }
    }

    companion object {
        const val MAX_INPUT_HOUR_LENGTH = 3
        const val MAX_DELAY_IN_HOUR = 168
    }
}



