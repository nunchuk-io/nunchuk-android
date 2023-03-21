package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
        _state.update { it.copy(coinTag = args.coinTag) }
        getListCoinByTag()
    }

    private fun getListCoinByTag() {
        viewModelScope.launch {
            getListCoinByTagUseCase(
                GetListCoinByTagUseCase.Param(
                    walletId = args.walletId,
                    tagId = args.coinTag.id
                )
            ).onSuccess { coins ->
                _state.update { it.copy(coins = coins) }
            }
        }

        viewModelScope.launch {
            getAllTagsUseCase(args.walletId).onSuccess { tags ->
                _state.update { state ->
                    state.copy(tags = tags.associateBy { it.id })
                }
            }
        }
    }

    fun deleteCoinTag() = viewModelScope.launch {
        val result = deleteCoinTagUseCase(
            DeleteCoinTagUseCase.Param(
                walletId = args.walletId,
                tagId = args.coinTag.id
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinTagDetailEvent.DeleteTagSuccess)
        } else {
            _event.emit(CoinTagDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getNumCoins() = _state.value.coins.size

    fun updateTagName(name: String) {
        val coinTag = _state.value.coinTag?.copy(name = name) ?: return
        _state.update { it.copy(coinTag = coinTag) }
    }

    fun updateColor(color: String) = viewModelScope.launch {
        val coinTag = _state.value.coinTag?.copy(color = color) ?: return@launch
        val result = updateCoinTagUseCase(
            UpdateCoinTagUseCase.Param(
                walletId = args.walletId,
                coinTag = coinTag
            )
        )
        if (result.isSuccess) {
            _state.update { it.copy(coinTag = coinTag) }
            _event.emit(CoinTagDetailEvent.UpdateTagColorSuccess)
        } else {
            _event.emit(CoinTagDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun removeCoin(coins: List<UnspentOutput>) = viewModelScope.launch {
        val result = removeCoinFromTagUseCase(
            RemoveCoinFromTagUseCase.Param(
                walletId = args.walletId,
                tagId = args.coinTag.id,
                coins = coins
            )
        )
        if (result.isSuccess) {
            val coinList = _state.value.coins.toMutableList()
            coins.forEach {
                coinList.removeIf { coin ->
                    it.txid == coin.txid
                }
            }
            _state.update { it.copy(coins = coinList) }
            _event.emit(CoinTagDetailEvent.RemoveCoinSuccess)
        } else {
            _event.emit(CoinTagDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}