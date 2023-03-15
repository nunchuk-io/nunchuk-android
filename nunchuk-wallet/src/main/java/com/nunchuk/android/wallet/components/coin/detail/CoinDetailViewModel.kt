package com.nunchuk.android.wallet.components.coin.detail

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class CoinDetailViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<CoinDetailEvent>()
    val event = _event.asSharedFlow()

}

sealed class CoinDetailEvent