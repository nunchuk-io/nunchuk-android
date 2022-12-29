package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KeyRecoveryIntroViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(KeyRecoveryIntroState())

    private val _event = MutableSharedFlow<KeyRecoveryIntroEvent>()
    val event = _event.asSharedFlow()

    fun getTapSignerList() = viewModelScope.launch {
        _event.emit(KeyRecoveryIntroEvent.Loading(true))
        getMasterSignersUseCase.execute().collect { masterSigners ->
            _event.emit(KeyRecoveryIntroEvent.Loading(false))
            val signers = masterSigners
                .filter { it.device.isTapsigner }
                .map { signer ->
                    masterSignerMapper(signer)
                }.map {
                    it.copy(cardId = getTapSignerStatusByIdUseCase(it.id).getOrThrow().ident.orEmpty())
                }.toList()
            _event.emit(KeyRecoveryIntroEvent.GetTapSignerSuccess(signers))
            _state.update {
                it.copy(
                    tapSigners = signers,
                )
            }
        }
    }

}