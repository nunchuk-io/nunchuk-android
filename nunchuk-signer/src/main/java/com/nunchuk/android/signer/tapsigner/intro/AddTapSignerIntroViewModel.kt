package com.nunchuk.android.signer.tapsigner.intro

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusUseCase
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddTapSignerIntroViewModel @Inject constructor(
    private val getTapSignerStatusUseCase: GetTapSignerStatusUseCase,
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {
    private val _event = MutableSharedFlow<AddTapSignerIntroEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(AddTapSignerIntroEvent.ContinueEventAddTapSigner)
        }
    }

    fun getTapSignerStatus(isoDep: IsoDep?) {
        isoDep ?: return
        viewModelScope.launch {
            _event.emit(AddTapSignerIntroEvent.Loading(true))
            val result = getTapSignerStatusUseCase(BaseNfcUseCase.Data(isoDep))
            _event.emit(AddTapSignerIntroEvent.Loading(false))
            if (result.isSuccess) {
                Timber.d("TapSigner auth delay ${result.getOrThrow().authDelayInSecond}")
                _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusSuccess(result.getOrThrow()))
            } else {
                _event.emit(AddTapSignerIntroEvent.GetTapSignerStatusError(result.exceptionOrNull()))
            }
        }
    }

    fun isKeyAddedToAssistedWallet(masterSignerId: String) = membershipStepManager.isKeyExisted(masterSignerId)

    fun getSignerName() = "TAPSIGNER${membershipStepManager.getNextKeySuffixByType(SignerType.NFC)}"
}

sealed class AddTapSignerIntroEvent {
    data class Loading(val isLoading: Boolean) : AddTapSignerIntroEvent()
    data class GetTapSignerStatusSuccess(
        val status: TapSignerStatus
    ) : AddTapSignerIntroEvent()

    data class GetTapSignerStatusError(val e: Throwable?) : AddTapSignerIntroEvent()
    object ContinueEventAddTapSigner : AddTapSignerIntroEvent()
}