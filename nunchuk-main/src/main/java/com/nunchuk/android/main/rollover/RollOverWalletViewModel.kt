package com.nunchuk.android.main.rollover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.CreateAndBroadcastRollOverTransactionsUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllCollectionsUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.replace.UpdateReplaceKeyConfigUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RollOverWalletViewModel @Inject constructor(
    private val getAllCollectionsUseCase: GetAllCollectionsUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val createAndBroadcastRollOverTransactionsUseCase: CreateAndBroadcastRollOverTransactionsUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val updateReplaceKeyConfigUseCase: UpdateReplaceKeyConfigUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RollOverWalletUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RollOverWalletEvent>()
    val event = _event.asSharedFlow()

    private var selectedTagIds: List<Int> = emptyList()
    private var selectedCollectionIds: List<Int> = emptyList()
    private var feeRate: Amount = Amount.ZER0
    private var source: Int = RollOverWalletSource.WALLET_CONFIG

    fun init(
        oldWalletId: String,
        newWalletId: String,
        selectedTagIds: List<Int>,
        selectedCollectionIds: List<Int>,
        feeRate: Amount,
        source: Int
    ) {

        savedStateHandle[OLD_WALLET_ID] = oldWalletId
        savedStateHandle[NEW_WALLET_ID] = newWalletId

        this.selectedTagIds = selectedTagIds
        this.selectedCollectionIds = selectedCollectionIds
        this.feeRate = feeRate
        this.source = source

        getAllCoins()
        getAllTags()
        getAllCollections()

        viewModelScope.launch {
            getUnusedWalletAddressUseCase(newWalletId).onSuccess { addresses ->
                _uiState.update { state ->
                    state.copy(address = addresses.first())
                }
            }
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(oldWalletId).onSuccess { wallet ->
                _uiState.update { it.copy(oldWallet = wallet) }
            }
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(newWalletId).onSuccess { wallet ->
                _uiState.update { it.copy(newWallet = wallet) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFreeWallet = assistedWalletManager.getWalletPlan(oldWalletId) == MembershipPlan.NONE) }
        }
    }

    private fun getAllCoins() {
        viewModelScope.launch {
            _event.emit(RollOverWalletEvent.Loading(true))
            getAllCoinUseCase(getOldWalletId()).onSuccess { coins ->
                _event.emit(RollOverWalletEvent.Loading(false))
                _uiState.update { state ->
                    state.copy(coins = coins)
                }
            }
        }
    }

    private fun getAllTags() {
        viewModelScope.launch {
            getAllTagsUseCase(getOldWalletId()).onSuccess { tags ->
                _uiState.update { state ->
                    state.copy(coinTags = tags)
                }
            }
        }
    }

    private fun getAllCollections() {
        viewModelScope.launch {
            getAllCollectionsUseCase(getOldWalletId()).onSuccess { collections ->
                _uiState.update { state ->
                    state.copy(coinCollections = collections)
                }
            }
        }
    }

    fun createRollOverTransactions(randomizeBroadcast: Boolean, days: Int) {
        viewModelScope.launch {
            _event.emit(RollOverWalletEvent.Loading(true))
            val groupId = assistedWalletManager.getGroupId(getOldWalletId()).orEmpty()
            createAndBroadcastRollOverTransactionsUseCase(
                CreateAndBroadcastRollOverTransactionsUseCase.Param(
                    newWalletId = getNewWalletId(),
                    oldWalletId = getOldWalletId(),
                    tags = getSelectedTags().orEmpty(),
                    collections = getSelectedCollections().orEmpty(),
                    feeRate = feeRate,
                    groupId = groupId,
                    days = days,
                    randomizeBroadcast = randomizeBroadcast,
                    isFreeWallet = uiState.value.isFreeWallet
                )
            ).onSuccess {
                if (it.isNullOrEmpty()) {
                    _event.emit(RollOverWalletEvent.Error("Failed to create transactions"))
                    return@onSuccess
                }
                _event.emit(RollOverWalletEvent.Success)
            }.onFailure {
                _event.emit(RollOverWalletEvent.Error(it.message.orEmpty()))
            }
            _event.emit(RollOverWalletEvent.Loading(false))
        }
    }

    fun getOldWalletId(): String {
        return savedStateHandle.get<String>(OLD_WALLET_ID).orEmpty()
    }

    fun getNewWalletId(): String {
        return savedStateHandle.get<String>(NEW_WALLET_ID).orEmpty()
    }

    fun getAddress(): String {
        return uiState.value.address
    }

    fun getOldWallet(): Wallet {
        return uiState.value.oldWallet
    }

    fun getNewWallet(): Wallet {
        return uiState.value.newWallet
    }

    fun getCoinTags(): List<CoinTag> {
        return uiState.value.coinTags
    }

    fun getCoinCollections(): List<CoinCollection> {
        return uiState.value.coinCollections
    }

    fun getSelectedTags(): List<CoinTag>? {
        if (selectedTagIds.isNotEmpty() && getCoinTags().isEmpty()) {
            return null
        }
        return getCoinTags().filter { selectedTagIds.contains(it.id) }
    }

    fun getSelectedCollections(): List<CoinCollection>? {
        if (selectedCollectionIds.isNotEmpty() && getCoinCollections().isEmpty()) {
            return null
        }
        return getCoinCollections().filter { selectedCollectionIds.contains(it.id) }
    }

    fun getFeeRate(): Amount {
        return feeRate
    }

    fun getSource(): Int {
        return source
    }

    fun updateReplaceKeyConfig(isRemoveKey: Boolean) {
        if (!uiState.value.isFreeWallet) {
            viewModelScope.launch {
                val walletId = getOldWalletId()
                val groupId = assistedWalletManager.getGroupId(walletId).orEmpty()
                updateReplaceKeyConfigUseCase(
                    UpdateReplaceKeyConfigUseCase.Param(
                        groupId = groupId,
                        walletId = walletId,
                        isRemoveKey = isRemoveKey
                    )
                )
            }
        }
    }

    companion object {
        private const val OLD_WALLET_ID = "old_wallet_id"
        private const val NEW_WALLET_ID = "new_wallet_id"
    }
}

sealed class RollOverWalletEvent {
    data class Loading(val isLoading: Boolean) : RollOverWalletEvent()
    data class Error(val message: String) : RollOverWalletEvent()
    data object Success : RollOverWalletEvent()
}

data class RollOverWalletUiState(
    val coinTags: List<CoinTag> = emptyList(),
    val coinCollections: List<CoinCollection> = emptyList(),
    val coins: List<UnspentOutput> = emptyList(),
    val oldWallet: Wallet = Wallet(),
    val newWallet: Wallet = Wallet(),
    val address: String = "",
    val isFreeWallet: Boolean = false,
)