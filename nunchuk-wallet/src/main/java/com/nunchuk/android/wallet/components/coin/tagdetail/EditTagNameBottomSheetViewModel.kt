package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.usecase.coin.UpdateCoinTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagNameBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) : ViewModel() {

    val args = EditTagNameBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<EditTagNameBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private var allTags = arrayListOf<CoinTag>()

    fun onSaveClick(tagName: String) = viewModelScope.launch {
        val existedTag =
            allTags.firstOrNull { it.name == tagName }
        if (existedTag != null) {
            _event.emit(EditTagNameBottomSheetEvent.ExistingTagNameError)
            return@launch
        }
        val coinTag = args.coinTag.copy(name = tagName)
        val result = updateCoinTagUseCase(
            UpdateCoinTagUseCase.Param(
                args.walletId,
                coinTag,
                assistedWalletManager.isActiveAssistedWallet(args.walletId)
            )
        )
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

    fun setTags(tags: List<CoinTag>) {
        allTags.clear()
        allTags.addAll(tags)
    }
}

sealed class EditTagNameBottomSheetEvent {
    data class UpdateTagNameSuccess(val tagName: String) : EditTagNameBottomSheetEvent()
    object ExistingTagNameError : EditTagNameBottomSheetEvent()
    data class Error(val message: String) : EditTagNameBottomSheetEvent()
}