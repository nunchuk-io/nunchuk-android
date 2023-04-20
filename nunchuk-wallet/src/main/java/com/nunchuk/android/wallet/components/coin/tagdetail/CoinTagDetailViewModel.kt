package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.usecase.coin.DeleteCoinTagUseCase
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
    private val deleteCoinTagUseCase: DeleteCoinTagUseCase,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
    private val removeCoinFromTagUseCase: RemoveCoinFromTagUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) : ViewModel() {

    private val _event = MutableSharedFlow<CoinTagDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinTagDetailState())
    val state = _state.asStateFlow()

    private val args = CoinTagDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)

    init {
        _state.update { it.copy(coinTag = args.coinTag) }
    }

    fun getListCoinByTag(allCoins: List<UnspentOutput>, tags: Map<Int, CoinTag>) {
        val coins = allCoins.filter { it.tags.contains(args.coinTag.id) }
        _state.update {
            it.copy(coins = coins, tags = tags)
        }
    }

    fun deleteCoinTag() = viewModelScope.launch {
        val result = deleteCoinTagUseCase(
            DeleteCoinTagUseCase.Param(
                walletId = args.walletId,
                tagId = args.coinTag.id,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
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
                coinTag = coinTag,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
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
                tagIds = listOf(args.coinTag.id),
                coins = coins,
                isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
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