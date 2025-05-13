package com.nunchuk.android.app.miniscript.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.CreateMiniscriptTemplateByCustomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MiniscriptCustomTemplateEvent {
    data class Success(val template: String) : MiniscriptCustomTemplateEvent()
    data class Error(val message: String) : MiniscriptCustomTemplateEvent()
}

@HiltViewModel
class MiniscriptCustomTemplateViewModel @Inject constructor(
    private val miniscriptTemplateByCustomUseCase: CreateMiniscriptTemplateByCustomUseCase
) : ViewModel() {

    private val _event = MutableStateFlow<MiniscriptCustomTemplateEvent?>(null)
    val event: StateFlow<MiniscriptCustomTemplateEvent?> = _event

    fun createMiniscriptTemplate(template: String, addressType: AddressType) {
        viewModelScope.launch {
            miniscriptTemplateByCustomUseCase(
                CreateMiniscriptTemplateByCustomUseCase.Params(
                    template = template,
                    addressType = addressType
                )
            ).onSuccess { result ->
                _event.value = MiniscriptCustomTemplateEvent.Success(result)
            }.onFailure { e ->
                _event.value =
                    MiniscriptCustomTemplateEvent.Error(e.message.orUnknownError())
            }
        }
    }

    fun clearEvent() {
        _event.value = null
    }
}