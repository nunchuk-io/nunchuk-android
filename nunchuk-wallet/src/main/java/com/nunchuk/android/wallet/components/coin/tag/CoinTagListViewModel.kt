package com.nunchuk.android.wallet.components.coin.tag

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.CreateCoinTagUseCase
import com.nunchuk.android.usecase.coin.GetCoinTagAdditionListUseCase
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
class CoinTagListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCoinTagAdditionListUseCase: GetCoinTagAdditionListUseCase,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
    private val createCoinTagUseCase: CreateCoinTagUseCase,
    private val addToCoinTagUseCase: AddToCoinTagUseCase
) : ViewModel() {
    val args = CoinTagListFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CoinTagListEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinTagListState())
    val state = _state.asStateFlow()

    private val hexColorUsedList = hashSetOf<String>()

    init {
        getCoinTags()
    }

    fun getCoinTags(loadSilent: Boolean = false) = viewModelScope.launch {
        if (loadSilent) _event.emit(CoinTagListEvent.Loading(true))
        val result = getCoinTagAdditionListUseCase(args.walletId)
        if (result.isSuccess) {
            val tags = result.getOrDefault(emptyList()).map {
                hexColorUsedList.add(it.coinTag.color)
                it
            }
            _state.update { it.copy(tags = tags) }
        } else {
            if (loadSilent) _event.emit(CoinTagListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
        _event.emit(CoinTagListEvent.Loading(false))
    }

    private fun getNextAvailableHexColor(): String {
        val hexColor = CoinTagColorUtil.hexColors.firstOrNull {
            hexColorUsedList.contains(it).not()
        }?.also { CoinTagColorUtil.hexColors.first() }
        return hexColor!!
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
            _event.emit(CoinTagListEvent.AddCoinToTagSuccess)
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

    fun getSelectedCoinTagList(): List<CoinTag> {
        val selectedCoinTags = _state.value.selectedCoinTags
        return _state.value.tags.filter { selectedCoinTags.contains(it.coinTag.id) }
            .map { it.coinTag }
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
                name = coinTagInputHolder.name,
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