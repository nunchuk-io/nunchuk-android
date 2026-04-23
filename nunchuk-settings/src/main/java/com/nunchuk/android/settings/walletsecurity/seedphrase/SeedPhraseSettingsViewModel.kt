package com.nunchuk.android.settings.walletsecurity.seedphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.DEFAULT_SEED_PHRASE_DELAY_HOURS
import com.nunchuk.android.model.MIN_SEED_PHRASE_DELAY_HOURS
import com.nunchuk.android.usecase.signer.GetEffectiveSeedPhraseDelayUseCase
import com.nunchuk.android.usecase.signer.SaveSeedPhraseDecreaseTransitionUseCase
import com.nunchuk.android.usecase.signer.SaveSeedPhraseDelayHoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SeedPhraseSettingsViewModel @Inject constructor(
    private val getEffectiveSeedPhraseDelayUseCase: GetEffectiveSeedPhraseDelayUseCase,
    private val saveSeedPhraseDelayHoursUseCase: SaveSeedPhraseDelayHoursUseCase,
    private val saveSeedPhraseDecreaseTransitionUseCase: SaveSeedPhraseDecreaseTransitionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SeedPhraseSettingsUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SeedPhraseSettingsEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            val effectiveHours = getEffectiveSeedPhraseDelayUseCase(Unit)
                .getOrDefault(DEFAULT_SEED_PHRASE_DELAY_HOURS)
            _state.update {
                it.copy(savedHours = effectiveHours)
            }
        }
    }

    fun onContinueClicked(newHours: Int) {
        viewModelScope.launch {
            val state = _state.value
            if (newHours < MIN_SEED_PHRASE_DELAY_HOURS) return@launch

            val oldHours = state.savedHours
            when {
                newHours > oldHours -> {
                    saveSeedPhraseDecreaseTransitionUseCase(SaveSeedPhraseDecreaseTransitionUseCase.Param())
                    saveSeedPhraseDelayHoursUseCase(newHours)
                        .onSuccess {
                            _state.update { it.copy(savedHours = newHours) }
                            _event.emit(SeedPhraseSettingsEvent.SavedImmediate)
                        }
                }

                newHours < oldHours -> {
                    _state.update {
                        it.copy(
                            showDecreaseConfirmDialog = true,
                            pendingNewHours = newHours,
                        )
                    }
                }

                else -> _event.emit(SeedPhraseSettingsEvent.NoChange)
            }
        }
    }

    fun onConfirmDecrease() {
        viewModelScope.launch {
            val state = _state.value
            val newHours = state.pendingNewHours ?: return@launch
            val oldHours = state.savedHours
            saveSeedPhraseDecreaseTransitionUseCase(
                SaveSeedPhraseDecreaseTransitionUseCase.Param(pendingHours = newHours)
            )
            _state.update { it.copy(showDecreaseConfirmDialog = false) }
            _event.emit(SeedPhraseSettingsEvent.SavedWithDelay(oldHours = oldHours))
        }
    }

    fun onDismissDecreaseDialog() {
        _state.update { it.copy(showDecreaseConfirmDialog = false, pendingNewHours = null) }
    }
}

enum class SeedPhraseDelayOption {
    TWO_HOURS,
    CUSTOM,
}

data class SeedPhraseSettingsUiState(
    val savedHours: Int = DEFAULT_SEED_PHRASE_DELAY_HOURS,
    val showDecreaseConfirmDialog: Boolean = false,
    val pendingNewHours: Int? = null,
)

sealed class SeedPhraseSettingsEvent {
    data object SavedImmediate : SeedPhraseSettingsEvent()
    data class SavedWithDelay(val oldHours: Int) : SeedPhraseSettingsEvent()
    data object NoChange : SeedPhraseSettingsEvent()
}
