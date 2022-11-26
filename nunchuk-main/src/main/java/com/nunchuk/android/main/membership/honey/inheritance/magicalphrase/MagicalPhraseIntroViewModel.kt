package com.nunchuk.android.main.membership.honey.inheritance.magicalphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagicalPhraseIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<MagicalPhraseIntroEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(MagicalPhraseIntroEvent.OnContinueClicked)
        }
    }
}

sealed class MagicalPhraseIntroEvent {
    object OnContinueClicked : MagicalPhraseIntroEvent()
}