package com.nunchuk.android.app.miniscript.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.usecase.CreateMiniscriptTemplateByCustomUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MiniscriptIntroViewModel @Inject constructor(
    private val createMiniscriptTemplateByCustomUseCase: CreateMiniscriptTemplateByCustomUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MiniscriptIntroState())
    val uiState = _uiState.asStateFlow()

    fun handleFileContent(content: String, addressType: AddressType) {
        Timber.tag("miniscript-feature")
            .d("Handling file content: $content with address type: $addressType")
        viewModelScope.launch {
            createMiniscriptTemplateByCustomUseCase(
                CreateMiniscriptTemplateByCustomUseCase.Params(
                    template = content,
                    addressType = addressType
                )
            ).onSuccess { template ->
                Timber.tag("miniscript-feature").d("Miniscript template created successfully: $template")
                if (template.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            template = template,
                            event = MiniscriptIntroEvent.NavigateToCustomTemplate(template)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            event = MiniscriptIntroEvent.ShowError("Failed to read data or script has invalid syntax")
                        )
                    }
                }
            }.onFailure { error ->
                Timber.e("Failed to create miniscript template: $error")
                _uiState.update {
                    it.copy(
                        event = MiniscriptIntroEvent.ShowError(error.message.orUnknownError())
                    )
                }
            }
        }
    }

    fun onEventHandled() {
        _uiState.update { it.copy(event = null) }
    }
}

data class MiniscriptIntroState(
    val isLoading: Boolean = false,
    val template: String = "",
    val event: MiniscriptIntroEvent? = null
)

sealed class MiniscriptIntroEvent {
    data class NavigateToCustomTemplate(val template: String) : MiniscriptIntroEvent()
    data class ShowError(val message: String) : MiniscriptIntroEvent()
} 