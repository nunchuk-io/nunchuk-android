package com.nunchuk.android.main.rollover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.RollOverWalletParam
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.getNearestTimeLock
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.main.rollover.RollOverWalletActivity.Companion.NEW_WALLET_ID
import com.nunchuk.android.main.rollover.RollOverWalletActivity.Companion.OLD_WALLET_ID
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.usecase.CreateAndBroadcastRollOverTransactionsUseCase
import com.nunchuk.android.usecase.EstimateFeeForSigningPathsUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.EstimateRollOverFeeForSigningPathsUseCase
import com.nunchuk.android.usecase.GetChainTipUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.coin.GetAllCollectionsUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.miniscript.GetSpendableNowAmountUseCase
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val getSpendableNowAmountUseCase: GetSpendableNowAmountUseCase,
    private val getChainTipUseCase: GetChainTipUseCase,
    private val estimateRollOverFeeForSigningPathsUseCase: EstimateRollOverFeeForSigningPathsUseCase,
    private val estimateFeeForSigningPathsUseCase: EstimateFeeForSigningPathsUseCase,
    private val estimateFeeUseCase: EstimateFeeUseCase,
) : ViewModel() {
    private val mutex = Mutex()

    private val _uiState = MutableStateFlow(RollOverWalletUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RollOverWalletEvent>()
    val event = _event.asSharedFlow()

    private var selectedTagIds: List<Int> = emptyList()
    private var selectedCollectionIds: List<Int> = emptyList()
    private var feeRate: Amount = Amount.ZER0
    private var source: Int = RollOverWalletSource.WALLET_CONFIG
    private var antiFeeSniping: Boolean = false

    fun init(
        oldWalletId: String,
        newWalletId: String,
        selectedTagIds: List<Int>,
        selectedCollectionIds: List<Int>,
        feeRate: Amount,
        source: Int,
        antiFeeSniping: Boolean,
    ) {
        savedStateHandle[OLD_WALLET_ID] = oldWalletId
        savedStateHandle[NEW_WALLET_ID] = newWalletId

        this.selectedTagIds = selectedTagIds
        this.selectedCollectionIds = selectedCollectionIds
        this.feeRate = feeRate
        this.source = source
        this.antiFeeSniping = antiFeeSniping

        getAllCoins()

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
                // Load miniscript wallet data if applicable
                if (wallet.miniscript.isNotEmpty()) {
                    loadMiniscriptWalletData()
                }
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
        viewModelScope.launch {
            fetchFeeRateIfNeeded()
        }
    }

    fun getAllTagsAndCollections() {
        getAllTags()
        getAllCollections()
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
                    isFreeWallet = uiState.value.isFreeWallet,
                    antiFeeSniping = antiFeeSniping,
                    signingPath = savedStateHandle[RollOverWalletActivity.SIGNING_PATH]
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

    fun getSigningPath(): SigningPath? = savedStateHandle[RollOverWalletActivity.SIGNING_PATH]

    suspend fun checkSigningPathsForRollOver(rollOverWalletParam: RollOverWalletParam): Int {
        fetchFeeRateIfNeeded()
        val result = estimateRollOverFeeForSigningPathsUseCase(
            EstimateRollOverFeeForSigningPathsUseCase.Params(
                oldWalletId = getOldWalletId(),
                newWalletId = rollOverWalletParam.newWalletId,
                feeRate = feeRate,
                tags = rollOverWalletParam.tags,
                collections = rollOverWalletParam.collections
            )
        ).getOrNull().orEmpty()
        if (result.size == 1) {
            savedStateHandle[RollOverWalletActivity.SIGNING_PATH] = result.first().first
        }

        return result.size
    }

    suspend fun checkSigningPathsForConsolidation(): Int {
        val address = getAddress()
        val balance = getOldWallet().balance.pureBTC()
        val outputs = mapOf(address to balance.toAmount())
        fetchFeeRateIfNeeded()
        val result = estimateFeeForSigningPathsUseCase(
            EstimateFeeForSigningPathsUseCase.Params(
                walletId = getOldWalletId(),
                outputs = outputs,
                subtractFeeFromAmount = true,
                feeRate = feeRate,
                inputs = emptyList(),
            )
        ).getOrNull().orEmpty()

        if (result.size == 1) {
            savedStateHandle[RollOverWalletActivity.SIGNING_PATH] = result.first().first
        }

        return result.size
    }

    fun updateReplaceKeyConfig(isRemoveKey: Boolean) {
        if (source == RollOverWalletSource.REPLACE_KEY) {
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

    private fun loadMiniscriptWalletData() {
        val oldWallet = getOldWallet()
        if (oldWallet.miniscript.isEmpty()) return

        viewModelScope.launch {
            val walletId = getOldWalletId()

            // Get spendable now amount
            getSpendableNowAmountUseCase(walletId).onSuccess { spendableNowAmount ->
                if (spendableNowAmount.value > 0L) {
                    val totalBalance = oldWallet.balance
                    val timelockedAmount =
                        Amount(value = maxOf(0, totalBalance.value - spendableNowAmount.value))

                    // Get furthest timelock from coins
                    val currentBlockHeight = getChainTipUseCase(Unit).getOrDefault(0)
                    var furthestTimelock: Pair<MiniscriptTimelockBased, Long>? = null
                    var maxTimelock: Long = Long.MIN_VALUE

                    getAllCoinUseCase(walletId).onSuccess { coins ->
                        coins.forEach { coin ->
                            coin.getNearestTimeLock(currentBlockHeight)?.let { time ->
                                if (time > maxTimelock) {
                                    maxTimelock = time
                                    furthestTimelock = coin.lockBased to time
                                }
                            }
                        }

                        _uiState.update { state ->
                            state.copy(
                                spendableNowAmount = spendableNowAmount,
                                timelockedAmount = timelockedAmount,
                                furthestTimelock = furthestTimelock
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchFeeRateIfNeeded() = mutex.withLock {
        if (feeRate == Amount.ZER0) {
            feeRate =
                estimateFeeUseCase(Unit).getOrNull()?.defaultRate?.toManualFeeRate() ?: Amount.ZER0
        }
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
    val spendableNowAmount: Amount? = null,
    val timelockedAmount: Amount? = null,
    val furthestTimelock: Pair<MiniscriptTimelockBased, Long>? = null,
)