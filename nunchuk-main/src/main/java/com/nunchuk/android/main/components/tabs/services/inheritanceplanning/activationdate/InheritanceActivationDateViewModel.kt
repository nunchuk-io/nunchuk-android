package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InheritanceActivationDateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceActivationDateFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceActivationDateEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceActivationDateState())
    val state = _state.asStateFlow()

    init {
        if (args.isUpdateRequest) {
            _state.update {
                it.copy(date = args.selectedActivationDate)
            }
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        val date = _state.value.date
        if (date != 0L) {
            _event.emit(InheritanceActivationDateEvent.ContinueClick(date))
        }
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _state.value.date
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _state.update {
            it.copy(date = cal.timeInMillis)
        }
    }

}