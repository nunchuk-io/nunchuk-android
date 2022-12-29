package com.nunchuk.android.main.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.MarkShowNfcUniversalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniversalNfcIntroViewModel @Inject constructor(
    private val markShowNfcUniversalUseCase: MarkShowNfcUniversalUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<UniversalNfcIntroEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            markShowNfcUniversalUseCase(Unit)
        }
    }

    fun onGotInClicked() {
        viewModelScope.launch {
            _event.emit(UniversalNfcIntroEvent.OnGotItClicked)
        }
    }
}

sealed class UniversalNfcIntroEvent {
    object OnGotItClicked : UniversalNfcIntroEvent()
}