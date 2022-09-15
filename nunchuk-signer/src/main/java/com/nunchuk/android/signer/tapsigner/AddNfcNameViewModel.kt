package com.nunchuk.android.signer.tapsigner

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateTapSignerUseCase
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.UpdateMasterSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNfcNameViewModel @Inject constructor(
    private val createTapSignerUseCase: CreateTapSignerUseCase,
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val updateMasterSignerUseCase: UpdateMasterSignerUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<AddNfcNameEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(AddNfcNameState())

    fun addNameForNfcKey(
        isoDep: IsoDep?, cvc: String, name: String, shouldCreateBackUp: Boolean = false
    ) {
        isoDep ?: return
        viewModelScope.launch {
            _state.value.signer?.takeIf { shouldCreateBackUp }?.let {
                getBackUpTapSigner(isoDep, cvc, it)
                return@launch
            }
            _event.emit(AddNfcNameEvent.Loading(true))
            val createTapSignerKeyResult =
                createTapSignerUseCase(CreateTapSignerUseCase.Data(isoDep, cvc, name))
            if (createTapSignerKeyResult.isSuccess) {
                val signer = createTapSignerKeyResult.getOrThrow()
                _state.update { it.copy(signer = signer) }
                if (shouldCreateBackUp) {
                    getBackUpTapSigner(isoDep, cvc, signer)
                } else {
                    _event.emit(AddNfcNameEvent.Loading(false))
                    _event.emit(AddNfcNameEvent.Success(signer))
                }
            } else {
                _event.emit(AddNfcNameEvent.Loading(false))
                _event.emit(AddNfcNameEvent.Error(createTapSignerKeyResult.exceptionOrNull()))
            }
        }
    }

    private suspend fun getBackUpTapSigner(
        isoDep: IsoDep, cvc: String, signer: MasterSigner
    ) {
        if (_state.value.filePath.isNotEmpty()) {
            _event.emit(AddNfcNameEvent.BackUpSuccess(_state.value.filePath))
            return
        }
        _event.emit(AddNfcNameEvent.Loading(true))
        val createBackUpKeyResult =
            getTapSignerBackupUseCase(GetTapSignerBackupUseCase.Data(isoDep, cvc, signer.id))
        _event.emit(AddNfcNameEvent.Loading(false))
        if (createBackUpKeyResult.isSuccess) {
            _event.emit(AddNfcNameEvent.BackUpSuccess(createBackUpKeyResult.getOrThrow()))
            _state.update { it.copy(filePath = createBackUpKeyResult.getOrThrow()) }
        } else {
            _event.emit(AddNfcNameEvent.Error(createBackUpKeyResult.exceptionOrNull()))
        }
    }

    fun updateName(masterSigner: MasterSigner, updateSignerName: String) {
        viewModelScope.launch {
            when (val result =
                updateMasterSignerUseCase.execute(masterSigner = masterSigner.copy(name = updateSignerName))) {
                is Result.Success -> {
                    _event.emit(
                        AddNfcNameEvent.Success(masterSigner.copy(name = updateSignerName))
                    )
                }
                is Result.Error -> {
                    _event.emit(AddNfcNameEvent.UpdateError(result.exception))
                }
            }
        }
    }

    fun getMasterSigner(): MasterSigner? = _state.value.signer
}

data class AddNfcNameState(val signer: MasterSigner? = null, val filePath: String = "")

sealed class AddNfcNameEvent {
    data class Loading(val isLoading: Boolean) : AddNfcNameEvent()
    data class Success(val masterSigner: MasterSigner) : AddNfcNameEvent()
    data class BackUpSuccess(val filePath: String) : AddNfcNameEvent()
    data class Error(val e: Throwable?) : AddNfcNameEvent()
    data class UpdateError(val e: Throwable?) : AddNfcNameEvent()
}