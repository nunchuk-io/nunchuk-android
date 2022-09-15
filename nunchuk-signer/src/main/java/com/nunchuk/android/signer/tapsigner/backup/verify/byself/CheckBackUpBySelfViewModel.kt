package com.nunchuk.android.signer.tapsigner.backup.verify.byself

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckBackUpBySelfViewModel @Inject constructor(
   private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
   savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args : CheckBackUpBySelfFragmentArgs = CheckBackUpBySelfFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CheckBackUpBySelfEvent>()
    val event = _event.asSharedFlow()

    fun onBtnClicked(event: CheckBackUpBySelfEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    fun setKeyVerified() {
        viewModelScope.launch {
            val result = setKeyVerifiedUseCase(args.masterSignerId)
            if (result.isSuccess) {
                _event.emit(OnExitSelfCheck)
            } else {
                _event.emit(ShowError(result.exceptionOrNull()))
            }
        }
    }
}

sealed class CheckBackUpBySelfEvent
object OnDownloadBackUpClicked : CheckBackUpBySelfEvent()
object OnVerifiedBackUpClicked : CheckBackUpBySelfEvent()
object OnExitSelfCheck : CheckBackUpBySelfEvent()
data class ShowError(val e: Throwable?) : CheckBackUpBySelfEvent()