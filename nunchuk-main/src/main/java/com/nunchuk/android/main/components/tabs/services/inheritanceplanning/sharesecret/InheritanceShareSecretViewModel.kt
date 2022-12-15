package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceShareSecretViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceShareSecretEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceShareSecretState())
    val state = _state.asStateFlow()

    fun onContinueClick() = viewModelScope.launch {
        val option = _state.value.options.first { it.isSelected }
        _event.emit(InheritanceShareSecretEvent.ContinueClick(option.type))
    }

    fun onOptionClick(type: Int) {
        val value = _state.value
        val options = value.options.toMutableList()
        val newOptions = options.map {
            it.copy(isSelected = it.type == type)
        }
        _state.update {
            it.copy(options = newOptions)
        }
    }

}