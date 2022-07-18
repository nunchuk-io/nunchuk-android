package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateTapSignerUseCase
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.UpdateMasterSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNfcNameViewModel @Inject constructor(
    private val createTapSignerUseCase: CreateTapSignerUseCase,
    private val updateMasterSignerUseCase: UpdateMasterSignerUseCase
) : ViewModel() {
    private val _event = MutableStateFlow<AddNfcNameState?>(null)
    val event = _event.filterIsInstance<AddNfcNameState>()

    fun addNameForNfcKey(isoDep: IsoDep?, cvc: String, name: String) {
        isoDep ?: return
        _event.value = AddNfcNameState.Loading
        viewModelScope.launch {
            val result = createTapSignerUseCase(CreateTapSignerUseCase.Data(isoDep, cvc, name))
            if (result.isSuccess) {
                _event.value = AddNfcNameState.Success(result.getOrThrow())
            } else {
                _event.value = AddNfcNameState.Error(result.exceptionOrNull())
            }
        }
    }

    fun updateName(masterSigner: MasterSigner, updateSignerName: String) {
        viewModelScope.launch {
            when (val result = updateMasterSignerUseCase.execute(masterSigner = masterSigner.copy(name = updateSignerName))) {
                is Result.Success -> {
                    _event.value = AddNfcNameState.Success(masterSigner.copy(name = updateSignerName))
                }
                is Result.Error -> {
                    _event.value = AddNfcNameState.UpdateError(result.exception)
                }
            }
        }
    }
}

sealed class AddNfcNameState {
    object Loading : AddNfcNameState()
    class Success(val masterSigner: MasterSigner) : AddNfcNameState()
    class Error(val e: Throwable?) : AddNfcNameState()
    class UpdateError(val e: Throwable?) : AddNfcNameState()
}