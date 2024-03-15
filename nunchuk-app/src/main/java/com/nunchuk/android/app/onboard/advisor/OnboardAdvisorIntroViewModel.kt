package com.nunchuk.android.app.onboard.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.profile.SetOnBoardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OnboardAdvisorIntroViewModel @Inject constructor(
    private val setOnBoardUseCase: SetOnBoardUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardAdvisorIntroUiState())
    val state = _state.asStateFlow()

    fun markOnboardDone() = viewModelScope.launch {
        setOnBoardUseCase(false)
    }

    fun handledOpenMainScreen() {
        _state.update { it.copy(openMainScreen = false) }
    }
}

data class OnboardAdvisorIntroUiState(
    val openMainScreen: Boolean = false,
)