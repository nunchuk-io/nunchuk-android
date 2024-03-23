package com.nunchuk.android.app.onboard.advisor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.profile.SetOnBoardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class OnboardAdvisorIntroViewModel @Inject constructor(
    private val setOnBoardUseCase: SetOnBoardUseCase,
    accountManager: AccountManager,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardAdvisorIntroUiState(
        isLoggedIn = accountManager.getAccount().chatId.isNotEmpty()
    ))
    val state = _state.asStateFlow()

    fun markOnboardDone() = viewModelScope.launch {
        runCatching {
            setOnBoardUseCase(false)
        }.onSuccess {
            _state.update { it.copy(openMainScreen = true) }
        }.onFailure {
            Timber.e(it)
        }
    }

    fun handledOpenMainScreen() {
        _state.update { it.copy(openMainScreen = false) }
    }
}

data class OnboardAdvisorIntroUiState(
    val openMainScreen: Boolean = false,
    val isLoggedIn: Boolean = false
)