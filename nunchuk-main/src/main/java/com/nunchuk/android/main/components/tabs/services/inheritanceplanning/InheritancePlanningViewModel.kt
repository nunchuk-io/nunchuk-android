package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.annotation.Keep
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.WalletServer
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.SyncGroupWalletUseCase
import com.nunchuk.android.usecase.wallet.GetServerWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritancePlanningViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getServerWalletUseCase: GetServerWalletUseCase,
    private val syncGroupWalletUseCase: SyncGroupWalletUseCase,
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
            it.copy(keyTypes = keyTypes, walletType = wallet.walletType)
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
}

data class InheritancePlanningState(
    val groupId: String = "",
    val groupWalletType: GroupWalletType? = null,
    val keyTypes: List<InheritanceKeyType> = emptyList(),
    val walletType: WalletType = WalletType.MULTI_SIG,
    val userEmail: String = "",
    val setupOrReviewParam: InheritancePlanningParam.SetupOrReview
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
        val dummyTransactionId: String = ""
    ) : InheritancePlanningParam()
}

