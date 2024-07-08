package com.nunchuk.android.main.rollover.broadcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.main.rollover.coincontrol.RollOverCoinControlUiState
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.isNonePlan
import com.nunchuk.android.usecase.BatchTransactionsUseCase
import com.nunchuk.android.usecase.CreateAndBroadcastRollOverTransactionsUseCase
import com.nunchuk.android.usecase.CreateRollOverTransactionsUseCase
import com.nunchuk.android.usecase.RandomizeBroadcastBatchTransactionsUseCase
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
class RollOverBroadcastTransactionViewModel @Inject constructor(
    private val createAndBroadcastRollOverTransactionsUseCase: CreateAndBroadcastRollOverTransactionsUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _event = MutableSharedFlow<RollOverBroadcastTransactionEvent>()
    val event = _event.asSharedFlow()

    private val _uiState = MutableStateFlow(RollOverBroadcastTransactionUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var groupId: String
    private lateinit var selectedTags: List<CoinTag>
    private lateinit var selectedCollections: List<CoinCollection>
    private lateinit var feeRate: Amount
    private lateinit var oldWalletId: String
    private lateinit var newWalletId: String

    init {
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { plans ->
                    _uiState.update { it.copy(isPaidUser = plans.isNonePlan().not()) }
                }
        }
    }

    fun init(
        oldWalletId: String, newWalletId: String,
        tags: List<CoinTag>,
        collections: List<CoinCollection>,
        feeRate: Amount
    ) {
        this.oldWalletId = oldWalletId
        this.newWalletId = newWalletId
        selectedTags = tags
        selectedCollections = collections
        this.feeRate = feeRate
        groupId = assistedWalletManager.getGroupId(oldWalletId).orEmpty()
    }

    fun createRollOverTransactions() {
        viewModelScope.launch {
            _event.emit(RollOverBroadcastTransactionEvent.Loading(true))
            createAndBroadcastRollOverTransactionsUseCase(
                CreateAndBroadcastRollOverTransactionsUseCase.Param(
                    newWalletId = newWalletId,
                    oldWalletId = oldWalletId,
                    tags = selectedTags,
                    collections = selectedCollections,
                    feeRate = feeRate,
                    groupId = groupId,
                    days = uiState.value.days,
                    randomizeBroadcast = uiState.value.randomizeBroadcast
                )
            ).onSuccess {
                if (it.isNullOrEmpty()) {
                    _event.emit(RollOverBroadcastTransactionEvent.Error("Failed to create transactions"))
                    return@onSuccess
                }
                _event.emit(RollOverBroadcastTransactionEvent.Success)
            }.onFailure {
                _event.emit(RollOverBroadcastTransactionEvent.Error(it.message.orEmpty()))
            }
            _event.emit(RollOverBroadcastTransactionEvent.Loading(false))
        }
    }

    fun updateDays(days: Int) {
        _uiState.update { it.copy(days = days) }
    }

    fun updateRandomizeBroadcast(randomizeBroadcast: Boolean) {
        _uiState.update { it.copy(randomizeBroadcast = randomizeBroadcast) }
    }
}

data class RollOverBroadcastTransactionUiState(
    val isPaidUser: Boolean = false,
    val days: Int = 1,
    val randomizeBroadcast: Boolean = false
)

sealed class RollOverBroadcastTransactionEvent {
    data class Loading(val isLoading: Boolean) : RollOverBroadcastTransactionEvent()
    data class Error(val message: String) : RollOverBroadcastTransactionEvent()
    data object Success : RollOverBroadcastTransactionEvent()
}