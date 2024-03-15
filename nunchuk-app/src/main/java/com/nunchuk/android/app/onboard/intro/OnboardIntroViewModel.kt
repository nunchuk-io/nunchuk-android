package com.nunchuk.android.app.onboard.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.profile.MarkOnBoardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardIntroViewModel @Inject constructor(
    private val markOnBoardUseCase: MarkOnBoardUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardIntroState())
    val state = _state.asStateFlow()

    fun markOnBoardDone() {
        viewModelScope.launch {
            runCatching {
                markOnBoardUseCase(Unit)
            }.onSuccess {
                _state.update { it.copy(openMainScreen = true) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun handledOpenMainScreen() {
        _state.update { it.copy(openMainScreen = false) }
    }
}

data class OnboardIntroState(
    val openMainScreen: Boolean = false,
)