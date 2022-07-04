package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ChangeCvcTapSignerUseCase
import com.nunchuk.android.core.domain.SetupTapSignerUseCase
import com.nunchuk.android.model.MasterSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeNfcCvcViewModel @Inject constructor(
    private val setupTapSignerUseCase: SetupTapSignerUseCase,
    private val changeCvcTapSignerUseCase: ChangeCvcTapSignerUseCase
) : ViewModel() {
    private val _event = MutableStateFlow<ChangeNfcCvcEvent?>(null)
    val event = _event.filterIsInstance<ChangeNfcCvcEvent>()
    private var setUpJob : Job? = null

    fun setUpCvc(isoDep: IsoDep?, oldCvc: String, newCvc: String, chainCode: String) {
        isoDep ?: return
        _event.value = ChangeNfcCvcEvent.LongLoading
        setUpJob = viewModelScope.launch {
            val result = setupTapSignerUseCase(SetupTapSignerUseCase.Data(isoDep, oldCvc, newCvc, chainCode))
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _event.value = ChangeNfcCvcEvent.SetupCvcSuccess(data.backUpKeyPath, data.masterSigner)
            } else {
                _event.value = ChangeNfcCvcEvent.Error(result.exceptionOrNull())
            }
        }
    }

    fun cancelSetupNfc() {
        setUpJob?.cancel()
        _event.value = ChangeNfcCvcEvent.SetupCancel
    }

    fun changeCvc(isoDep: IsoDep?, oldCvc: String, newCvc: String) {
        isoDep ?: return
        _event.value = ChangeNfcCvcEvent.Loading
        viewModelScope.launch {
            val result = changeCvcTapSignerUseCase(ChangeCvcTapSignerUseCase.Data(isoDep, oldCvc, newCvc))
            if (result.isSuccess && result.getOrThrow()) {
                _event.value = ChangeNfcCvcEvent.ChangeCvcSuccess
            } else {
                _event.value = ChangeNfcCvcEvent.Error(result.exceptionOrNull())
            }
        }
    }

    fun clearEvent() {
        _event.value = null
    }
}

sealed class ChangeNfcCvcEvent {
    object LongLoading : ChangeNfcCvcEvent()
    object Loading : ChangeNfcCvcEvent()
    object ChangeCvcSuccess : ChangeNfcCvcEvent()
    object SetupCancel : ChangeNfcCvcEvent()
    class SetupCvcSuccess(val backupKeyPath: String, val masterSigner: MasterSigner) : ChangeNfcCvcEvent()
    class Error(val e: Throwable?) : ChangeNfcCvcEvent()
}