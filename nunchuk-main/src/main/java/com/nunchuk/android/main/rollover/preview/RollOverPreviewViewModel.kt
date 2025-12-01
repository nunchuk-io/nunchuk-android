package com.nunchuk.android.main.rollover.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.DraftRollOverTransaction
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.usecase.DraftRollOverTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RollOverPreviewViewModel @Inject constructor(
    private val draftRollOverTransactionsUseCase: DraftRollOverTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RollOverPreviewUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RollOverPreviewEvent>()
    val event = _event.asSharedFlow()

    private lateinit var selectedTags: List<CoinTag>
    private lateinit var selectedCollections: List<CoinCollection>
    private lateinit var feeRate: Amount
    private lateinit var oldWalletId: String
    private lateinit var newWalletId: String

    private val tags: ArrayList<CoinTag> = arrayListOf()
    private val collections: ArrayList<CoinCollection> = arrayListOf()
    private var signingPath: SigningPath? = null

    private var isDraftedTx = false

    fun init(
        oldWalletId: String,
        newWalletId: String,
        selectedTags: List<CoinTag>,
        selectedCollections: List<CoinCollection>,
        feeRate: Amount,
        signingPath: SigningPath?
    ) {

        this.oldWalletId = oldWalletId
        this.newWalletId = newWalletId
        this.selectedTags = selectedTags
        this.selectedCollections = selectedCollections
        this.feeRate = feeRate
        this.signingPath = signingPath

        if (isAllRequiredParamsInitialized() && isDraftedTx.not()) {
            isDraftedTx = true
            draftRollOverTransactions()
        }
    }

    fun updateTagsAndCollections(tags: List<CoinTag>, collections: List<CoinCollection>) {
        this.tags.clear()
        this.tags.addAll(tags)
        this.collections.clear()
        this.collections.addAll(collections)
    }

    private fun draftRollOverTransactions() = viewModelScope.launch {
        _event.emit(RollOverPreviewEvent.Loading(true))
        draftRollOverTransactionsUseCase(
            DraftRollOverTransactionsUseCase.Data(
                newWalletId = newWalletId,
                oldWalletId = oldWalletId,
                tags = selectedTags,
                collections = selectedCollections,
                feeRate = feeRate,
                signingPath = signingPath
            )
        ).onSuccess { transactions ->
            mappingPreviewUi(transactions)
        }.onFailure {
            _event.emit(RollOverPreviewEvent.Error(it.message.orUnknownError()))
        }
        _event.emit(RollOverPreviewEvent.Loading(false))
    }

    private fun mappingPreviewUi(transactions: List<DraftRollOverTransaction>) {
        val uis = arrayListOf<PreviewTransactionUi>()
        var totalFee = 0L
        transactions.map { rollOverTransaction ->
            uis.add(
                PreviewTransactionUi(
                    transaction = rollOverTransaction.transaction,
                    tags = tags.filter { rollOverTransaction.tagIds.contains(it.id) }.associateBy { it.id },
                    collections = collections.filter { rollOverTransaction.collectionIds.contains(it.id) }.associateBy { it.id }
                )
            )
            totalFee += rollOverTransaction.transaction.feeRate.value
        }
        _uiState.update {
            it.copy(uis = uis, totalFee = totalFee.toAmount())
        }
    }

    private fun isAllRequiredParamsInitialized() =
        ::oldWalletId.isInitialized
                && ::newWalletId.isInitialized
                && ::selectedTags.isInitialized
                && ::selectedCollections.isInitialized
                && ::feeRate.isInitialized
}

data class PreviewTransactionUi(
    val transaction: Transaction,
    val tags: Map<Int, CoinTag> = emptyMap(),
    val collections: Map<Int, CoinCollection> = emptyMap(),
)

data class RollOverPreviewUiState(
    val uis: List<PreviewTransactionUi> = emptyList(),
    val totalFee: Amount = Amount.ZER0
)

sealed class RollOverPreviewEvent {
    data class Loading(val isLoading: Boolean) : RollOverPreviewEvent()
    data class Error(val message: String) : RollOverPreviewEvent()
}