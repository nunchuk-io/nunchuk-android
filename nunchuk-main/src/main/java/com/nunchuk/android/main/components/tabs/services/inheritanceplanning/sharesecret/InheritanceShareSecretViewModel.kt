package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceShareSecretViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceShareSecretEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceShareSecretState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

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