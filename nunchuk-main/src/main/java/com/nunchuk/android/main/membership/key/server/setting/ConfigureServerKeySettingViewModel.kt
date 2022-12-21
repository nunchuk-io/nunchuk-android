package com.nunchuk.android.main.membership.key.server.setting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.CreateServerKeysUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.SERVER_KEY_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigureServerKeySettingViewModel @Inject constructor(
    private val createServerKeysUseCase: CreateServerKeysUseCase,
    private val membershipStepManager: MembershipStepManager,
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
                autoBroadcastSwitched = args.keyPolicy?.autoBroadcastTransaction ?: true,
                cosigningTextHours = args.keyPolicy?.getSigningDelayInHours()?.toString().orEmpty(),
                cosigningTextMinutes = args.keyPolicy?.getSigningDelayInMinutes()?.toString()
                    .orEmpty(),
                enableCoSigningSwitched = (args.keyPolicy?.signingDelayInSeconds ?: 0) > 0
            )
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        val state = _state.value
        if (state.enableCoSigningSwitched && state.cosigningTextMinutes.isEmpty()) {
            _event.emit(ConfigureServerKeySettingEvent.NoDelayInput)
            return@launch
        }
        val signingDelayInHour =
            if (state.enableCoSigningSwitched) (state.cosigningTextHours.toIntOrNull() ?: 0) else 0
        val signingDelayInMinute =
            if (state.enableCoSigningSwitched) (state.cosigningTextMinutes.toIntOrNull()
                ?: 0) else 0
        val signingDelayInHourFinal = signingDelayInHour + signingDelayInMinute / ONE_MINUTE
        if (signingDelayInHourFinal !in 0..MAX_DELAY_IN_HOUR) {
            _event.emit(ConfigureServerKeySettingEvent.DelaySigningInHourInvalid)
            return@launch
        }
        val signingDelayInSeconds =
            signingDelayInHour * KeyPolicy.ONE_HOUR_TO_SECONDS + signingDelayInMinute * KeyPolicy.ONE_MINUTE_TO_SECONDS
        if (args.xfp.isNullOrEmpty()) {
            _event.emit(ConfigureServerKeySettingEvent.Loading(true))
            val result = createServerKeysUseCase(
                CreateServerKeysUseCase.Param(
                    name = SERVER_KEY_NAME,
                    keyPolicy = KeyPolicy(
                        autoBroadcastTransaction = state.autoBroadcastSwitched,
                        signingDelayInSeconds = signingDelayInSeconds,
                        spendingPolicy = args.spendingLimit
                    ),
                    plan = membershipStepManager.plan
                )
            )
            _event.emit(ConfigureServerKeySettingEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(ConfigureServerKeySettingEvent.ConfigServerSuccess(result.getOrThrow()))
            } else {
                _event.emit(ConfigureServerKeySettingEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        } else if (args.keyPolicy != null) {
            _event.emit(
                ConfigureServerKeySettingEvent.ConfigServerSuccess(
                    args.keyPolicy!!.copy(
                        autoBroadcastTransaction = state.autoBroadcastSwitched,
                        signingDelayInSeconds = signingDelayInSeconds,
                    )
                )
            )
        }
    }

    fun updateCoSigningDelayHourText(hour: String) {
        _state.update {
            it.copy(cosigningTextHours = hour.take(MAX_INPUT_HOUR_LENGTH))
        }
    }

    fun updateCoSigningDelayMinuteText(minute: String) {
        val minutes = minute.toIntOrNull()
        if (minutes == null) {
            _state.update {
                it.copy(cosigningTextMinutes = "")
            }
        } else {
            _state.update {
                it.copy(cosigningTextMinutes = minutes.coerceAtMost(MAX_DELAY_IN_MINUTE).toString())
            }
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
        const val MAX_DELAY_IN_MINUTE = 59
        const val ONE_HOUR = 60 * 60
        const val ONE_MINUTE = 60
    }
}



