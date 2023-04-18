package com.nunchuk.android.wallet.components.coin.tag

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.usecase.coin.AddToCoinTagUseCase
import com.nunchuk.android.usecase.coin.CreateCoinTagUseCase
import com.nunchuk.android.usecase.coin.RemoveCoinFromTagUseCase
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
class CoinTagListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createCoinTagUseCase: CreateCoinTagUseCase,
    private val addToCoinTagUseCase: AddToCoinTagUseCase,
    private val removeCoinFromTagUseCase: RemoveCoinFromTagUseCase
) : ViewModel() {
    val args = CoinTagListFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CoinTagListEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinTagListState())
    val state = _state.asStateFlow()

    private val hexColorUsedList = hashSetOf<String>()

    init {
        setPreSelectedTags()
    }

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
        hexColorUsedList.clear()
        allTags.forEach { hexColorUsedList.add(it.color) }
    }

    fun addCoinTag() = viewModelScope.launch {
        val selectedTags = _state.value.selectedCoinTags
        val preSelectedTags = _state.value.preSelectedCoinTags
        val deletedTags = preSelectedTags.subtract(selectedTags)

        val addResultDefer = async {
            addToCoinTagUseCase(
                AddToCoinTagUseCase.Param(
                    walletId = args.walletId,
                    tagIds = selectedTags.toList(),
                    coins = args.coins.toList()
                )
            )
        }
        val deleteResultDefer = async {
            removeCoinFromTagUseCase(
                RemoveCoinFromTagUseCase.Param(
                    walletId = args.walletId,
                    tagIds = deletedTags.toList(),
                    coins = args.coins.toList()
                )
            )
        }
        val addResult = addResultDefer.await()
        val deleteResult = deleteResultDefer.await()
        if (addResult.isSuccess && deleteResult.isSuccess) {
            _event.emit(CoinTagListEvent.AddCoinToTagSuccess(args.coins.size))
        } else {
            val message = if (addResult.isFailure) {
                addResult.exceptionOrNull()?.message.orUnknownError()
            } else {
                deleteResult.exceptionOrNull()?.message.orUnknownError()
            }
            _event.emit(CoinTagListEvent.Error(message))
        }
    }

    fun enableButtonSave(): Boolean {
        val selectedTags = _state.value.selectedCoinTags
        val preSelectedTags = _state.value.preSelectedCoinTags
        val addedTags = selectedTags.subtract(preSelectedTags)
        val deletedTags = preSelectedTags.subtract(selectedTags)
        return addedTags.isNotEmpty() || deletedTags.isNotEmpty()
    }

    private fun setPreSelectedTags() {
        val preSelectedTags = hashSetOf<Int>()
        args.coins.forEach { output ->
            output.tags.forEach {
                preSelectedTags.add(it)
            }
        }
        _state.update {
            it.copy(
                preSelectedCoinTags = preSelectedTags,
                selectedCoinTags = preSelectedTags
            )
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
        val selectedCoinTags = _state.value.selectedCoinTags.toMutableSet()
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
        val existedTag =
            _state.value.tags.firstOrNull { it.coinTag.name == "#${coinTagInputHolder.name}" }
        if (existedTag != null) {
            _event.emit(CoinTagListEvent.ExistedTagError)
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
                _event.emit(CoinTagListEvent.CreateTagSuccess)
            }
        } else {
            _event.emit(CoinTagListEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
        _event.emit(CoinTagListEvent.Loading(false))
    }

    fun getCoinTagInputHolder() = _state.value.coinTagInputHolder

}