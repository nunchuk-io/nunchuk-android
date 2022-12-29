package com.nunchuk.android.main.membership.key.recoveryquestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.membership.model.SecurityQuestionModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryQuestionBottomSheetViewModel @Inject constructor() : ViewModel() {

    private val _event = MutableSharedFlow<RecoveryQuestionBottomSheetEvent>()
    val event = _event.asSharedFlow()

    fun onSelectQuestion(question: SecurityQuestionModel) = viewModelScope.launch {
        _event.emit(RecoveryQuestionBottomSheetEvent.SelectQuestion(question))
    }

}

sealed class RecoveryQuestionBottomSheetEvent {
    data class SelectQuestion(val question: SecurityQuestionModel) :
        RecoveryQuestionBottomSheetEvent()
}