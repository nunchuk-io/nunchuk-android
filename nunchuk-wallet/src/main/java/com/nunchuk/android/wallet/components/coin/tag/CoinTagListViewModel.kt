package com.nunchuk.android.wallet.components.coin.tag

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.CreateCoinTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinTagListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createCoinTagUseCase: CreateCoinTagUseCase,
    private val addToCoinTagUseCase: AddToCoinTagUseCase
) : ViewModel() {
    val args = CoinTagListFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CoinTagListEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinTagListState())
    val state = _state.asStateFlow()

    private val hexColorUsedList = hashSetOf<String>()

    private fun getNextAvailableHexColor(): String {
        val hexColor = CoinTagColorUtil.hexColors.firstOrNull {
            hexColorUsedList.contains(it).not()
        } ?: CoinTagColorUtil.hexColors.first()
        return hexColor
    }

    fun updateCoins(allTags: List<CoinTag>, numberOfCoinByTagId: Map<Int, Int>) {
        _state.update {
            it.copy(
                tags = allTags.map { tag -> CoinTagAddition(tag, numberOfCoinByTagId[tag.id] ?: 0) }
            )
        }
    }

    fun addCoinTag() = viewModelScope.launch {
        val result = addToCoinTagUseCase(
            AddToCoinTagUseCase.Param(
                walletId = args.walletId,
                tagIds = _state.value.selectedCoinTags,
                coins = args.coins.toList()
            )
        )
        if (result.isSuccess) {
            _event.emit(CoinTagListEvent.AddCoinToTagSuccess(_state.value.selectedCoinTags.size))
        } else {
            _event.emit(CoinTagListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun onCreateNewCoinTagClick() {
        _state.update { it.copy(coinTagInputHolder = CoinTag(color = getNextAvailableHexColor())) }
    }

    fun changeColor(color: String) {
        _state.update {
            val newCoinTagInputHolder = it.coinTagInputHolder?.copy(color = color)
            it.copy(coinTagInputHolder = newCoinTagInputHolder)
        }
    }

    fun onInputValueChange(value: String) {
        _state.update {
            val newCoinTagInputHolder = it.coinTagInputHolder?.copy(name = value)
            it.copy(coinTagInputHolder = newCoinTagInputHolder)
        }
    }

    fun onCheckedChange(id: Int, checked: Boolean) {
        val selectedCoinTags = _state.value.selectedCoinTags.toMutableList()
        if (checked) {
            selectedCoinTags.add(id)
        } else {
            selectedCoinTags.remove(id)
        }
        _state.update { it.copy(selectedCoinTags = selectedCoinTags) }
    }

    fun onDoneInputClick() = viewModelScope.launch {
        val coinTagInputHolder = _state.value.coinTagInputHolder ?: return@launch
        if (coinTagInputHolder.name.isBlank()) {
            _state.update { it.copy(coinTagInputHolder = null) }
            return@launch
        }
        _event.emit(CoinTagListEvent.Loading(true))
        val result = createCoinTagUseCase(
            CreateCoinTagUseCase.Param(
                walletId = args.walletId,
                name = "#${coinTagInputHolder.name}",
                color = coinTagInputHolder.color
            )
        )
        if (result.isSuccess) {
            result.getOrNull()?.let { newCoinTag ->
                val tags = _state.value.tags.toMutableList()
                tags.add(CoinTagAddition(coinTag = newCoinTag))
                hexColorUsedList.add(newCoinTag.color)
                _state.update { it.copy(coinTagInputHolder = null, tags = tags) }
            }
        } else {
            _event.emit(CoinTagListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
        _event.emit(CoinTagListEvent.Loading(false))
    }

    fun getCoinTagInputHolder() = _state.value.coinTagInputHolder

}