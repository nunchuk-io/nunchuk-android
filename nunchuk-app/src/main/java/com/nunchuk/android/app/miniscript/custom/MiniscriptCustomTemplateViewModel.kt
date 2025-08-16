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
import timber.log.Timber
import javax.inject.Inject

sealed class MiniscriptCustomTemplateEvent {
    data class Success(val template: String, val addressType: AddressType? = null) : MiniscriptCustomTemplateEvent()
    data class Error(val message: String) : MiniscriptCustomTemplateEvent()
    data class ShowTaprootWarning(val template: String) : MiniscriptCustomTemplateEvent()
    data object AddressTypeChangedToTaproot : MiniscriptCustomTemplateEvent()
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
                // Check if template is empty first
                if (result.template.isEmpty()) {
                    _event.value = MiniscriptCustomTemplateEvent.Error("Format not supported")
                } else if (addressType != AddressType.TAPROOT && result.isValidTapscript) {
                    // Check if address type is not Taproot and the result is valid for Tapscript
                    _event.value = MiniscriptCustomTemplateEvent.ShowTaprootWarning(result.template)
                } else {
                    _event.value = MiniscriptCustomTemplateEvent.Success(result.template)
                }
            }.onFailure { e ->
                _event.value =
                    MiniscriptCustomTemplateEvent.Error(e.message.orUnknownError())
            }
        }
    }

    fun continueWithCurrentAddressType(template: String) {
        _event.value = MiniscriptCustomTemplateEvent.Success(template)
    }

    fun changeToTaprootAndContinue(template: String) {
        // First show the success message for address type change
        _event.value = MiniscriptCustomTemplateEvent.AddressTypeChangedToTaproot
    }

    fun proceedWithTaproot(template: String) {
        Timber.tag("miniscript-feature").d("Proceeding with Taproot template: $template")
        _event.value = MiniscriptCustomTemplateEvent.Success(template, AddressType.TAPROOT)
    }



    fun clearEvent() {
        _event.value = null
    }
}