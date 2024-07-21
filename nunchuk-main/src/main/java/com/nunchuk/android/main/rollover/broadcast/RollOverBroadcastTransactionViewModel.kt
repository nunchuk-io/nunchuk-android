package com.nunchuk.android.main.rollover.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.isNonePlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RollOverBroadcastTransactionViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RollOverBroadcastTransactionUiState())
    val uiState = _uiState.asStateFlow()

    fun updateDays(days: Int) {
        _uiState.update { it.copy(days = days) }
    }

    fun updateRandomizeBroadcast(randomizeBroadcast: Boolean) {
        _uiState.update { it.copy(randomizeBroadcast = randomizeBroadcast) }
    }

    fun updateIsFreeWallet(isFreeWallet: Boolean) {
        _uiState.update { it.copy(isFreeWallet = isFreeWallet) }
    }
}

data class RollOverBroadcastTransactionUiState(
    val isFreeWallet: Boolean = false,
    val days: Int = 1,
    val randomizeBroadcast: Boolean = true
)