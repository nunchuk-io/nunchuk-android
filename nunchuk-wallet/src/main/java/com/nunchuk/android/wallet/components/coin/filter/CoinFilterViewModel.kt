package com.nunchuk.android.wallet.components.coin.filter

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
class CoinFilterViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<CoinFilterEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinFilterUiState())
    val state = _state.asStateFlow()

    fun onApplyFilter() {

    }

}

sealed class CoinFilterEvent

data class CoinFilterUiState(
    val filters: List<CoinFilter> = emptyList(),
    val selectTags: Set<Int> = emptySet(),
)