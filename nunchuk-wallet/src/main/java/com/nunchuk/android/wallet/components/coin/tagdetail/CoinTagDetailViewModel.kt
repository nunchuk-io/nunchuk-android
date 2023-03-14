package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.DeleteCoinTagUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.GetListCoinByTagUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromTagUseCase
import com.nunchuk.android.usecase.coin.UpdateCoinTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinTagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getListCoinByTagUseCase: GetListCoinByTagUseCase,
    private val deleteCoinTagUseCase: DeleteCoinTagUseCase,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
    private val removeCoinFromTagUseCase: RemoveCoinFromTagUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<CoinTagDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinTagDetailState())
    val state = _state.asStateFlow()

    private val args = CoinTagDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        _state.update { it.copy(coinTagAddition = args.coinTagAddition) }
        getListCoinByTag()
    }

    private fun getListCoinByTag() {
        viewModelScope.launch {
            getListCoinByTagUseCase(
                GetListCoinByTagUseCase.Param(
                    walletId = args.walletId,
                    tagId = args.coinTagAddition.coinTag.id
                )
            ).onSuccess { coins ->
                _state.update { it.copy(coins = coins) }
            }

            viewModelScope.launch {
                getAllTagsUseCase(args.walletId).onSuccess { tags ->
                    _state.update { state ->
                        state.copy(tags = tags.associateBy { it.id })
                    }
                }
            }
        }
    }

    fun deleteCoinTag() = viewModelScope.launch {
        val result = deleteCoinTagUseCase(
            DeleteCoinTagUseCase.Param(
                walletId = args.walletId,
                tagId = args.coinTagAddition.coinTag.id
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinTagDetailEvent.DeleteTagSuccess)
        } else {
            _event.emit(CoinTagDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun updateTagName(name: String) {
        val coinTag = _state.value.coinTagAddition?.coinTag?.copy(name = name) ?: return
        val coinTagAddition = _state.value.coinTagAddition?.copy(coinTag = coinTag) ?: return
        _state.update { it.copy(coinTagAddition = coinTagAddition) }
    }

    fun updateColor(color: String) = viewModelScope.launch {
        val coinTag = _state.value.coinTagAddition?.coinTag?.copy(color = color) ?: return@launch
        val result = updateCoinTagUseCase(
            UpdateCoinTagUseCase.Param(
                walletId = args.walletId,
                coinTag = coinTag
            )
        )
        if (result.isSuccess) {
            val coinTagAddition =
                _state.value.coinTagAddition?.copy(coinTag = coinTag) ?: return@launch
            _state.update { it.copy(coinTagAddition = coinTagAddition) }
            _event.emit(CoinTagDetailEvent.UpdateTagColorSuccess)
        } else {
            _event.emit(CoinTagDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun removeCoin(coin: UnspentOutput) = viewModelScope.launch {
        val result = removeCoinFromTagUseCase(
            RemoveCoinFromTagUseCase.Param(
                walletId = args.walletId,
                txId = coin.txid,
                tagId = args.coinTagAddition.coinTag.id,
                vout = coin.vout
            )
        )
        if (result.isSuccess) {
            val coins = _state.value.coins.toMutableList()
            coins.removeIf { it.txid == coin.txid }
            _state.update { it.copy(coins = coins) }
        } else {
            _event.emit(CoinTagDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}