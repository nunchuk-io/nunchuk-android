package com.nunchuk.android.wallet.components.coin.tagdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.usecase.coin.UpdateCoinTagUseCase
import com.nunchuk.android.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagNameBottomSheetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
) : ViewModel() {

    private val args =
        EditTagNameBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(CoinTagSelectColorBottomSheetState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<EditTagNameBottomSheetEvent>()
    val event = _event.asSharedFlow()

    init {
        var coinTagName = args.coinTag.name
        if (coinTagName.startsWith("#")) {
            coinTagName = coinTagName.removePrefix("#")
        }
        _state.update { it.copy(coinTag = args.coinTag.copy(name = coinTagName)) }
    }

    fun getCoinTagName() = "#${_state.value.coinTag.name}"

    fun updateTagName(value: String) {
        val coinTag = _state.value.coinTag.copy(name = value)
        _state.update { it.copy(coinTag = coinTag) }
    }

    fun onSaveClick() = viewModelScope.launch {
        val coinTagName = "#${_state.value.coinTag.name}"
        val coinTag = _state.value.coinTag.copy(name = coinTagName)
        val result = updateCoinTagUseCase(UpdateCoinTagUseCase.Param(args.walletId, coinTag))
        if (result.isSuccess) {
            if (result.getOrDefault(false)) {
                _event.emit(EditTagNameBottomSheetEvent.UpdateTagNameSuccess)
            } else {
                _state.update { it.copy(errorMsg = context.getString(R.string.nc_tag_name_already_exists)) }
            }
        } else {
            _state.update { it.copy(errorMsg = result.exceptionOrNull()?.message.orUnknownError()) }
        }
    }

}

data class CoinTagSelectColorBottomSheetState(
    val coinTag: CoinTag = CoinTag(), val errorMsg: String = ""
)

sealed class EditTagNameBottomSheetEvent {
    object UpdateTagNameSuccess : EditTagNameBottomSheetEvent()
}