package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.usecase.coin.UpdateCoinTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagNameBottomSheetViewModel @Inject constructor(
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<EditTagNameBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private lateinit var walletId: String
    private lateinit var coinTag: CoinTag

    fun init(walletId: String, coinTag: CoinTag) {
        this.walletId = walletId
        this.coinTag = coinTag
    }

    fun onSaveClick(tagName: String) = viewModelScope.launch {
        val coinTag = coinTag.copy(name = tagName)
        val result = updateCoinTagUseCase(UpdateCoinTagUseCase.Param(walletId, coinTag))
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