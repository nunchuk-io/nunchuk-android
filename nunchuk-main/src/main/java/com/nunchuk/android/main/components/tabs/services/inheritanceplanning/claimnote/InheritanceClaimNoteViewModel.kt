package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claimnote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceClaimNoteViewModel @Inject constructor(
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceClaimNoteFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceClaimNoteEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceClaimNote())
    val state = _state.asStateFlow()

    init {
        getStatus()
    }

    private fun getStatus() = viewModelScope.launch {
        _event.emit(InheritanceClaimNoteEvent.Loading(true))
        val result = getInheritanceClaimStateUseCase(
            GetInheritanceClaimStateUseCase.Param(
                signer = args.signer,
                magic = args.magic
            )
        )
        _event.emit(InheritanceClaimNoteEvent.Loading(false))
        if (result.isSuccess) {
            _state.update { it.copy(inheritanceAdditional = result.getOrThrow()) }
        } else {
            _event.emit(InheritanceClaimNoteEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun onWithdrawClick() = viewModelScope.launch {
        _event.emit(
            InheritanceClaimNoteEvent.WithdrawClick(
                _state.value.inheritanceAdditional?.balance ?: 0.0
            )
        )
    }

    fun checkWallet() = viewModelScope.launch {
        getWalletsUseCase.execute().flowOn(Dispatchers.IO)
            .onException { _event.emit(InheritanceClaimNoteEvent.Error(it.message.orUnknownError())) }
            .flowOn(Dispatchers.Main)
            .collect {
                _event.emit(InheritanceClaimNoteEvent.CheckHasWallet(it.isNotEmpty()))
            }
    }

    fun getBalance() = _state.value.inheritanceAdditional?.balance ?: 0.0

}