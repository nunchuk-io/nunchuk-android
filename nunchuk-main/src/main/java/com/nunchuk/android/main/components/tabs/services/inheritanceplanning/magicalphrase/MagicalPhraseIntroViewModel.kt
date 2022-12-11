package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.magicalphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.GetInheritanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagicalPhraseIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
    private val getInheritanceUseCase: GetInheritanceUseCase,
    private val getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdFlowUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<MagicalPhraseIntroEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(MagicalPhraseIntroState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        getInheritance()
    }

    private fun getInheritance() = viewModelScope.launch {
        _event.emit(MagicalPhraseIntroEvent.Loading(true))
        getAssistedWalletIdsFlowUseCase(Unit).collect { it ->
            val walletId = it.getOrNull() ?: return@collect
            val result = getInheritanceUseCase(walletId)
            _event.emit(MagicalPhraseIntroEvent.Loading(false))
            if (result.isSuccess) {
                _state.update { it.copy(magicalPhrase = result.getOrThrow().magic) }
            } else {
                _event.emit(MagicalPhraseIntroEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        if (_state.value.magicalPhrase.isNullOrBlank()) return@launch
        _event.emit(MagicalPhraseIntroEvent.OnContinueClicked(_state.value.magicalPhrase.orEmpty()))
    }
}

sealed class MagicalPhraseIntroEvent {
    data class Loading(val loading: Boolean) : MagicalPhraseIntroEvent()
    data class Error(val message: String) : MagicalPhraseIntroEvent()
    data class OnContinueClicked(val magicalPhrase: String) : MagicalPhraseIntroEvent()
}

data class MagicalPhraseIntroState(
    val magicalPhrase: String? = null
)