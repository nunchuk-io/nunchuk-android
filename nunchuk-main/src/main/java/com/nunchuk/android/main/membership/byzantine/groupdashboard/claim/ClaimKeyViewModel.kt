package com.nunchuk.android.main.membership.byzantine.groupdashboard.claim

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.KeyHealthCheckUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClaimKeyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val keyHealthCheckUseCase: KeyHealthCheckUseCase,
) : ViewModel() {
    private val args = ClaimKeyFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<ClaimKeyEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ClaimKeyUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId)
                .onSuccess {
                    val signers = it.signers.filter { signer -> signer.type != SignerType.SERVER }
                        .map { signer -> singleSignerMapper(signer) }
                    _state.update { state -> state.copy(signers = signers) }
                }
        }
    }

    fun onHealthCheck(signerModel: SignerModel) {
        viewModelScope.launch {
            _event.emit(ClaimKeyEvent.Loading(true))
            keyHealthCheckUseCase(
                KeyHealthCheckUseCase.Params(
                    args.groupId,
                    args.walletId,
                    signerModel.fingerPrint,
                )
            ).onSuccess {
                _event.emit(ClaimKeyEvent.GetHealthCheckPayload(it))
            }
            _event.emit(ClaimKeyEvent.Loading(false))
        }
    }
}

sealed class ClaimKeyEvent {
    data class Loading(val isLoading: Boolean) : ClaimKeyEvent()
    data class GetHealthCheckPayload(val payload: DummyTransactionPayload) : ClaimKeyEvent()
}

data class ClaimKeyUiState(val signers: List<SignerModel> = emptyList())