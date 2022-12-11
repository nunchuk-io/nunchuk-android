package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceNoteViewModel @Inject constructor(savedStateHandle: SavedStateHandle) :
    ViewModel() {

    private val _event = MutableSharedFlow<InheritanceNoteEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceNoteState())
    val state = _state.asStateFlow()

    private val args = InheritanceNoteFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        if (args.isUpdateRequest) {
            updateNote(args.preNoted)
        }
    }

    fun onContinueClicked() = viewModelScope.launch {
        val note = _state.value.note
        _event.emit(InheritanceNoteEvent.ContinueClick(note))
    }

    fun updateNote(note: String) =
        _state.update {
            it.copy(note = note)
        }

}