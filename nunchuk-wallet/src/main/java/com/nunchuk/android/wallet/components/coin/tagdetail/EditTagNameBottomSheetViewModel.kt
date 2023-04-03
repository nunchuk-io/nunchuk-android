package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.usecase.coin.UpdateCoinTagUseCase
import com.nunchuk.android.wallet.components.coin.collection.CoinCollectionBottomSheetFragmentArgs
import com.nunchuk.android.wallet.components.coin.tag.CoinTagListEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagNameBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
) : ViewModel() {

    val args = EditTagNameBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<EditTagNameBottomSheetEvent>()
    val event = _event.asSharedFlow()

    fun onSaveClick(tagName: String) = viewModelScope.launch {
        val existedTag =
            args.tags.firstOrNull { it.name == tagName }
        if (existedTag != null) {
            _event.emit(EditTagNameBottomSheetEvent.ExistingTagNameError)
            return@launch
        }
        val coinTag = args.coinTag.copy(name = tagName)
        val result = updateCoinTagUseCase(UpdateCoinTagUseCase.Param(args.walletId, coinTag))
        if (result.isSuccess) {
            if (result.getOrDefault(false)) {
                _event.emit(EditTagNameBottomSheetEvent.UpdateTagNameSuccess(tagName = tagName))
            } else {
                _event.emit(EditTagNameBottomSheetEvent.ExistingTagNameError)
            }
        } else {
            _event.emit(EditTagNameBottomSheetEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}

sealed class EditTagNameBottomSheetEvent {
    data class UpdateTagNameSuccess(val tagName: String) : EditTagNameBottomSheetEvent()
    object ExistingTagNameError : EditTagNameBottomSheetEvent()
    data class Error(val message: String) : EditTagNameBottomSheetEvent()
}