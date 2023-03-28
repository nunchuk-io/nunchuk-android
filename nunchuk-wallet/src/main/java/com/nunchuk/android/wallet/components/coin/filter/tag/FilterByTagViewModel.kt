package com.nunchuk.android.wallet.components.coin.filter.tag

import androidx.lifecycle.ViewModel
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.model.UnspentOutput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FilterByTagViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<FilterByTagEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(FilterByTagUiState())
    val state = _state.asStateFlow()

    fun extractTagAndNumberOfCoin(coins: List<UnspentOutput>, tags: List<CoinTag>) : List<CoinTagAddition> {
        val numberOfCoinByTagId = mutableMapOf<Int, Int>()
        coins.forEach { output ->
            output.tags.forEach { tagId ->
                numberOfCoinByTagId[tagId] = numberOfCoinByTagId.getOrPut(tagId) { 0 } + 1
            }
        }
        return tags.map { CoinTagAddition(it, numberOfCoinByTagId[it.id] ?: 0) }.sortedBy { it.coinTag.name }
    }
}

sealed class FilterByTagEvent

data class FilterByTagUiState(val selectedTags: Set<Int> = emptySet())