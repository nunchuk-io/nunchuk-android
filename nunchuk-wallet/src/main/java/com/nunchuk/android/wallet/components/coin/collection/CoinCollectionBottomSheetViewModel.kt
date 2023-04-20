package com.nunchuk.android.wallet.components.coin.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinCollection
import com.nunchuk.android.usecase.coin.CreateCoinCollectionUseCase
import com.nunchuk.android.usecase.coin.UpdateCoinCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinCollectionBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createCoinCollectionUseCase: CreateCoinCollectionUseCase,
    private val updateCoinCollectionUseCase: UpdateCoinCollectionUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) : ViewModel() {

    val args = CoinCollectionBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<CoinCollectionBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private lateinit var coinCollection: CoinCollection
    private var allCollections = arrayListOf<CoinCollection>()

    init {
        if (args.flow == CollectionFlow.ADD) {
            this.coinCollection = CoinCollection()
        } else if (args.flow == CollectionFlow.VIEW) {
            this.coinCollection = args.coinCollection!!
        }
    }

    fun createCoinCollection(name: String) = viewModelScope.launch {
        val existedCollection =
            allCollections.firstOrNull { it.name == name }
        if (existedCollection != null) {
            _event.emit(CoinCollectionBottomSheetEvent.ExistedCollectionError)
            return@launch
        }
        coinCollection = coinCollection.copy(name = name)
        val result = when (args.flow) {
            CollectionFlow.ADD -> {
                createCoinCollectionUseCase(
                    CreateCoinCollectionUseCase.Param(
                        walletId = args.walletId,
                        coinCollection = coinCollection,
                        isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                    )
                )
            }

            CollectionFlow.VIEW -> {
                updateCoinCollectionUseCase(
                    UpdateCoinCollectionUseCase.Param(
                        walletId = args.walletId,
                        coinCollection = coinCollection,
                        isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                    )
                )
            }

            else -> {
                throw IllegalArgumentException("invalid flow")
            }
        }

        if (result.isSuccess) {
            _event.emit(CoinCollectionBottomSheetEvent.CreateOrUpdateCollectionSuccess)
        } else {
            _event.emit(CoinCollectionBottomSheetEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun setAddNewCoin(checked: Boolean) {
        coinCollection = coinCollection.copy(isAddNewCoin = checked)
    }

    fun setAutoLock(checked: Boolean) {
        coinCollection = coinCollection.copy(isAutoLock = checked)
    }

    fun setCollections(collections: List<CoinCollection>) {
        allCollections.clear()
        allCollections.addAll(collections)
    }

    fun getCoinCollection() = coinCollection
}

sealed class CoinCollectionBottomSheetEvent {
    data class Error(val message: String) : CoinCollectionBottomSheetEvent()
    object CreateOrUpdateCollectionSuccess : CoinCollectionBottomSheetEvent()
    object ExistedCollectionError : CoinCollectionBottomSheetEvent()
}

