package com.nunchuk.android.signer.mk4.intro

import android.nfc.NdefRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetMk4SingersUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4IntroViewModel @Inject constructor(
    private val getMk4SingersUseCase: GetMk4SingersUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<Mk4IntroViewEvent>()
    val event = _event.asSharedFlow()

    private val _mk4Signers = mutableListOf<SingleSigner>()

    val mk4Signers: List<SingleSigner>
        get() = _mk4Signers

    fun getMk4Signer(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.Loading(true))
            val result = getMk4SingersUseCase(records.toTypedArray())
            _event.emit(Mk4IntroViewEvent.Loading(false))
            if (result.isSuccess) {
                this@Mk4IntroViewModel._mk4Signers.apply {
                    clear()
                    addAll(result.getOrThrow())
                }
                _event.emit(Mk4IntroViewEvent.LoadMk4SignersSuccess(this@Mk4IntroViewModel._mk4Signers))
            } else {
                _event.emit(Mk4IntroViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(Mk4IntroViewEvent.OnContinueClicked)
        }
    }
}