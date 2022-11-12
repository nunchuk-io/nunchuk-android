package com.nunchuk.android.main.membership.honey.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerInheritanceIntroViewModel @Inject constructor(
    membershipStepManager: MembershipStepManager,
) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerInheritanceIntroEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerInheritanceIntroEvent.OnContinueClicked)
        }
    }

    val remainTime = membershipStepManager.remainingTime
}

sealed class TapSignerInheritanceIntroEvent {
    object OnContinueClicked : TapSignerInheritanceIntroEvent()
}