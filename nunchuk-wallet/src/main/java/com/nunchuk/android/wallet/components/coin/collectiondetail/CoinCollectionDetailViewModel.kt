package com.nunchuk.android.wallet.components.coin.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.DeleteCoinCollectionUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromCollectionUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromTagUseCase
import com.nunchuk.android.usecase.coin.UpdateCoinCollectionUseCase
import com.nunchuk.android.wallet.components.coin.tag.CoinTagListEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinCollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deleteCoinCollectionUseCase: DeleteCoinCollectionUseCase,
    private val updateCoinCollectionUseCase: UpdateCoinCollectionUseCase,
    private val removeCoinFromCollectionUseCase: RemoveCoinFromCollectionUseCase,
) : ViewModel() {

    private val _event = MutableSharedFlow<CoinCollectionDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinCollectionDetailState())
    val state = _state.asStateFlow()

    private val args = CoinCollectionDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        _state.update { it.copy(coinCollection = args.coinCollection) }
    }

    fun getListCoinByTag(
        allCoins: List<UnspentOutput>,
        tags: Map<Int, CoinTag>
    ) {
        val coins = allCoins.filter { it.collection.contains(args.coinCollection.id) }
        _state.update {
            it.copy(coins = coins, tags = tags)
        }
    }

    fun updateCoinCollection(coinCollection: CoinCollection) {
        _state.update { it.copy(coinCollection = coinCollection) }
    }

    fun deleteCoinCollection() = viewModelScope.launch {
        val result = deleteCoinCollectionUseCase(
            DeleteCoinCollectionUseCase.Param(
                walletId = args.walletId,
                collectionId = args.coinCollection.id
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinCollectionDetailEvent.DeleteCollectionSuccess)
        } else {
            _event.emit(CoinCollectionDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getNumCoins() = _state.value.coins.size

    fun getCoinCollection() = _state.value.coinCollection

    fun removeCoin(coins: List<UnspentOutput>) = viewModelScope.launch {
        val result = removeCoinFromCollectionUseCase(
            RemoveCoinFromCollectionUseCase.Param(
                walletId = args.walletId,
                collectionIds = listOf(args.coinCollection.id),
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
            _event.emit(CoinCollectionDetailEvent.RemoveCoinSuccess)
        } else {
            _event.emit(CoinCollectionDetailEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}