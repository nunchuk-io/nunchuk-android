package com.nunchuk.android.app.miniscript.contractpolicy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.CreateMiniscriptTemplateBySelectionUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MiniscriptConfigTemplateViewModel @Inject constructor(
    private val createMiniscriptTemplateBySelectionUseCase: CreateMiniscriptTemplateBySelectionUseCase,
    private val getChainTipUseCase: GetChainTipUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(MiniscriptConfigTemplateState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getChainTipUseCase(Unit).onSuccess { blockHeight ->
                Timber.tag("miniscript-feature").d("MiniscriptConfigTemplateViewModel - blockHeight: $blockHeight")
                _uiState.update { it.copy(currentBlockHeight = blockHeight) }
            }.onFailure { error ->
                Timber.tag("miniscript-feature").e("MiniscriptConfigTemplateViewModel - error: $error")
            }
        }
    }

    fun createMiniscriptTemplateBySelection(
        multisignType: Int,
        newM: Int,
        newN: Int,
        n: Int,
        m: Int,
        timelockType: Int,
        timeUnit: Int,
        time: Long,
        addressType: AddressType,
        reuseSigner: Boolean
    ) {
        viewModelScope.launch {
            createMiniscriptTemplateBySelectionUseCase(
                CreateMiniscriptTemplateBySelectionUseCase.Params(
                    multisignType = multisignType,
                    newM = newM,
                    newN = newN,
                    n = n,
                    m = m,
                    timelockType = timelockType,
                    timeUnit = timeUnit,
                    time = time,
                    addressType = addressType,
                    reuseSigner = reuseSigner
                )
            ).onSuccess { template ->
                Timber.tag("miniscript-feature").e("MiniscriptConfigTemplateViewModel: $template")
                _uiState.update { 
                    it.copy(
                        template = template,
                        event = MiniscriptConfigTemplateEvent.TemplateCreated(template)
                    )
                }
            }.onFailure { error ->
                Timber.tag("miniscript-feature").e("MiniscriptConfigTemplateViewModel - error: $error")
                _uiState.update {
                    it.copy(
                        event = MiniscriptConfigTemplateEvent.ShowError(error.message.orUnknownError())
                    )
                }
            }
        }
    }

    fun onEventHandled() {
        _uiState.update { it.copy(event = null) }
    }
}

data class MiniscriptConfigTemplateState(
    val template: String = "",
    val event: MiniscriptConfigTemplateEvent? = null,
    val currentBlockHeight: Int = 0
)

sealed class MiniscriptConfigTemplateEvent {
    data class TemplateCreated(val template: String) : MiniscriptConfigTemplateEvent()
    data class ShowError(val message: String) : MiniscriptConfigTemplateEvent()
}
