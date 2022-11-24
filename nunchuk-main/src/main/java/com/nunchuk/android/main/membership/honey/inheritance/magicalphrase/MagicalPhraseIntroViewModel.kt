package com.nunchuk.android.main.membership.honey.inheritance.magicalphrase

import androidx.lifecycle.ViewModel
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class MagicalPhraseIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager
) : ViewModel() {
    private val _event = MutableSharedFlow<MagicalPhraseIntroEvent>()
    val event = _event.asSharedFlow()

    val remainTime = membershipStepManager.remainingTime
}

sealed class MagicalPhraseIntroEvent