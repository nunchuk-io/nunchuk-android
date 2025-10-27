package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.wallet.GetWalletBsmsUseCase
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.wallet.GetServerWalletUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class InheritancePlanningViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletBsmsUseCase: GetWalletBsmsUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    accountManager: AccountManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val groupId = savedStateHandle.get<String>(MembershipFragment.EXTRA_GROUP_ID).orEmpty()
    private val walletId =
        savedStateHandle.get<String>(InheritancePlanningActivity.EXTRA_WALLET_ID).orEmpty()

    private val _state = MutableStateFlow(
        InheritancePlanningState(
            groupId = savedStateHandle.get<String>(
                MembershipFragment.EXTRA_GROUP_ID
            ).orEmpty(),
            userEmail = accountManager.getAccount().email,
            setupOrReviewParam = InheritancePlanningParam.SetupOrReview(walletId = walletId)
        )
    )
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<InheritancePlanningEvent>()
    val event = _event.asSharedFlow()

    val setupOrReviewParam: InheritancePlanningParam.SetupOrReview
        get() = state.value.setupOrReviewParam

    init {
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupUseCase(GetGroupUseCase.Params(groupId))
                    .filter { it.isSuccess }
                    .map { it.getOrThrow() }
                    .collect { groupBrief ->
                        _state.update { it.copy(groupWalletType = groupBrief.walletConfig.toGroupWalletType()) }
                    }
            }
        }

        viewModelScope.launch {
            if (groupId.isNotEmpty()) {
                syncGroupWalletUseCase(groupId).onSuccess { wallet ->
                    updateKeyTypes(wallet)
                }
            } else {
                getServerWalletUseCase(walletId).onSuccess { wallet ->
                    updateKeyTypes(wallet)
                }
            }
        }
    }

    private fun updateKeyTypes(wallet: WalletServer) {
        val keyTypes = mutableListOf<InheritanceKeyType>()
        wallet.signers.filter { it.tags.contains(SignerTag.INHERITANCE.name) }
            .forEach { key ->
                if (key.type == SignerType.NFC) {
                    keyTypes.add(InheritanceKeyType.TAPSIGNER)
                } else {
                    keyTypes.add(InheritanceKeyType.COLDCARD)
                }
            }
        _state.update {
            it.copy(
                keyTypes = keyTypes,
                walletType = wallet.walletType,
                setupOrReviewParam = it.setupOrReviewParam.copy(
                    activationDate = if (wallet.walletType == WalletType.MINISCRIPT) wallet.timelockValue.seconds.inWholeMilliseconds else it.setupOrReviewParam.activationDate
                )
            )
        }
    }

    fun setOrUpdate(param: InheritancePlanningParam.SetupOrReview) {
        _state.update {
            it.copy(setupOrReviewParam = param)
        }
    }

    fun getGroupWalletType(): GroupWalletType? {
        return state.value.groupWalletType
    }

    fun isMiniscriptWallet() = state.value.walletType == WalletType.MINISCRIPT

    fun handleShareBsms() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { currentWallet ->
                when (val result = createShareFileUseCase.execute("${currentWallet.id}.bsms")) {
                    is Result.Success -> exportWallet(result.data, currentWallet)
                    is Result.Error -> {
                        _state.update { it.copy(isLoading = false) }
                        _event.emit(InheritancePlanningEvent.Failure(result.exception.message.orUnknownError()))
                    }
                }
            }
        }
    }

    fun saveBSMSToLocal() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { currentWallet ->
                getWalletBsmsUseCase(currentWallet).onSuccess { bsmsData ->
                    val result = saveLocalFileUseCase(
                        SaveLocalFileUseCase.Params(
                            "${currentWallet.id}.bsms",
                            bsmsData
                        )
                    )
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(InheritancePlanningEvent.SaveLocalFile(result.isSuccess))
                }.onFailure {
                    _state.update { it.copy(isLoading = false) }
                    _event.emit(InheritancePlanningEvent.SaveLocalFile(false))
                }
            }
        }
    }

    private suspend fun exportWallet(filePath: String, wallet: Wallet) {
        getWalletBsmsUseCase(wallet).onSuccess { bsmsData ->
            withContext(ioDispatcher) {
                File(filePath).writeText(bsmsData)
            }
            _state.update { it.copy(isLoading = false) }
            _event.emit(InheritancePlanningEvent.Success(filePath))
        }.onFailure {
            _state.update { it.copy(isLoading = false) }
            _event.emit(InheritancePlanningEvent.Failure(it.message.orUnknownError()))
        }
    }
}

sealed class InheritancePlanningEvent {
    data class Success(val filePath: String) : InheritancePlanningEvent()
    data class Failure(val message: String) : InheritancePlanningEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : InheritancePlanningEvent()
}

data class InheritancePlanningState(
    val groupId: String = "",
    val groupWalletType: GroupWalletType? = null,
    val keyTypes: List<InheritanceKeyType> = emptyList(),
    val walletType: WalletType = WalletType.MULTI_SIG,
    val userEmail: String = "",
    val setupOrReviewParam: InheritancePlanningParam.SetupOrReview,
    val isLoading: Boolean = false
) {
    val isMiniscriptWallet: Boolean
        get() = walletType == WalletType.MINISCRIPT
}

@Keep
enum class InheritanceKeyType {
    TAPSIGNER, COLDCARD
}

sealed class InheritancePlanningParam {
    data class SetupOrReview(
        val activationDate: Long = 0L,
        val selectedZoneId: String = "",
        val walletId: String,
        val emails: List<String> = emptyList(),
        val isNotify: Boolean = false,
        val notificationSettings: InheritanceNotificationSettings? = null,
        val magicalPhrase: String = "",
        val bufferPeriod: Period? = null,
        val note: String = "",
        val verifyToken: String = "",
        val planFlow: Int = 0,
        val sourceFlow: Int = InheritanceSourceFlow.NONE,
        val groupId: String = "",
        val dummyTransactionId: String = "",
        val inheritanceKeys: List<String> = emptyList(),
    ) : InheritancePlanningParam()
}

