package com.nunchuk.android.main.rollover.coincontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinCollectionAddition
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.EstimateRollOverTransactionAndAmountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RollOverCoinControlViewModel @Inject constructor(
    private val estimateRollOverTransactionAndAmountUseCase: EstimateRollOverTransactionAndAmountUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RollOverCoinControlUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RollOverCoinControlEvent>()
    val event = _event.asSharedFlow()

    private var calculateTransactionAndAmountJob: Job? = null

    private lateinit var oldWalletId: String
    private lateinit var newWalletId: String

    fun init(oldWalletId: String, newWalletId: String) {
        this.oldWalletId = oldWalletId
        this.newWalletId = newWalletId

        getEstimateFee()
    }

    private fun getEstimateFee() = viewModelScope.launch {
        _event.emit(RollOverCoinControlEvent.Loading(true))
        val resultFeeRate = estimateFeeUseCase(Unit)
        if (resultFeeRate.isSuccess) {
            _uiState.update {
                it.copy(manualFeeRate = resultFeeRate.getOrThrow().defaultRate)
            }
            calculateTransactionAndAmount()
        }
        _event.emit(RollOverCoinControlEvent.Loading(false))
    }

    fun updateTags(coins: List<UnspentOutput>, tags: List<CoinTag>) {
        val numberOfCoinByTagId = mutableMapOf<Int, Int>()
        coins.forEach { output ->
            output.tags.forEach { tagId ->
                numberOfCoinByTagId[tagId] = numberOfCoinByTagId.getOrPut(tagId) { 0 } + 1
            }
        }
        _uiState.update {
            it.copy(
                tags = tags.map { tag -> CoinTagAddition(tag, numberOfCoinByTagId[tag.id] ?: 0) }
            )
        }
    }

    fun updateCollections(coins: List<UnspentOutput>, collections: List<CoinCollection>) {
        val numberOfCoinByCollectionId = mutableMapOf<Int, Int>()
        coins.forEach { output ->
            output.collection.forEach { collectionId ->
                numberOfCoinByCollectionId[collectionId] =
                    numberOfCoinByCollectionId.getOrPut(collectionId) { 0 } + 1
            }
        }
        _uiState.update {
            it.copy(
                collections = collections.map { collection ->
                    CoinCollectionAddition(
                        collection,
                        numberOfCoinByCollectionId[collection.id] ?: 0
                    )
                }
            )
        }
    }

    fun setSelectTag(tagId: Int) {
        _uiState.update {
            it.copy(selectedCoinTags = it.selectedCoinTags.toMutableSet().apply {
                if (contains(tagId)) {
                    remove(tagId)
                } else {
                    add(tagId)
                }
            })
        }
        calculateTransactionAndAmount()
    }

    fun setSelectCollection(collectionId: Int) {
        _uiState.update {
            it.copy(selectedCoinCollections = it.selectedCoinCollections.toMutableSet().apply {
                if (contains(collectionId)) {
                    remove(collectionId)
                } else {
                    add(collectionId)
                }
            })
        }
        calculateTransactionAndAmount()
    }

    fun toggleSelected(isSelectAll: Boolean) {
        _uiState.update {
            it.copy(
                selectedCoinTags = if (isSelectAll) {
                    it.tags.map { tag -> tag.coinTag.id }.toSet()
                } else {
                    emptySet()
                },
                selectedCoinCollections = if (isSelectAll) {
                    it.collections.map { collection -> collection.collection.id }.toSet()
                } else {
                    emptySet()
                }
            )
        }
        calculateTransactionAndAmount()
    }

    private fun calculateTransactionAndAmount() {
        calculateTransactionAndAmountJob?.cancel()
        calculateTransactionAndAmountJob = viewModelScope.launch {
            val tags =
                _uiState.value.tags.filter { it.coinTag.id in _uiState.value.selectedCoinTags }
                    .map { it.coinTag }
            val collections =
                _uiState.value.collections.filter { it.collection.id in _uiState.value.selectedCoinCollections }
                    .map { it.collection }
            estimateRollOverTransactionAndAmountUseCase(
                EstimateRollOverTransactionAndAmountUseCase.Params(
                    oldWalletId,
                    newWalletId,
                    tags,
                    collections,
                    feeRate = _uiState.value.manualFeeRate.toManualFeeRate()
                )
            )
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            numOfTxs = result.numOfTxs,
                            feeAmount = result.feeAmount
                        )
                    }
                }
                .onFailure {
                    _event.emit(RollOverCoinControlEvent.Error(it.message.orEmpty()))
                }
        }
    }

    fun getSelectedCoinTags(): List<CoinTag> {
        return _uiState.value.tags.filter { it.coinTag.id in _uiState.value.selectedCoinTags }
            .map { it.coinTag }
    }

    fun getSelectedCoinCollections(): List<CoinCollection> {
        return _uiState.value.collections.filter { it.collection.id in _uiState.value.selectedCoinCollections }
            .map { it.collection }
    }

    fun numOfTxs(): Int {
        return uiState.value.numOfTxs
    }
}

internal fun Int.toManualFeeRate() = if (this > 0) toAmount() else Amount(-1)

sealed class RollOverCoinControlEvent {
    data class Loading(val isLoading: Boolean) : RollOverCoinControlEvent()
    data class Error(val message: String) : RollOverCoinControlEvent()
}

data class RollOverCoinControlUiState(
    val replacedWallet: Wallet = Wallet(),
    val newWallet: Wallet = Wallet(),
    val tags: List<CoinTagAddition> = emptyList(),
    val collections: List<CoinCollectionAddition> = emptyList(),
    val selectedCoinTags: Set<Int> = hashSetOf(),
    val selectedCoinCollections: Set<Int> = hashSetOf(),
    val numOfTxs: Int = 1,
    val feeAmount: Amount = Amount.ZER0,
    val manualFeeRate: Int = -1
)