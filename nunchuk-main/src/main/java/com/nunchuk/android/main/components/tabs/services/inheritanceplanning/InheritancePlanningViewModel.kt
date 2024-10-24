package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val groupId = savedStateHandle.get<String>(MembershipFragment.EXTRA_GROUP_ID).orEmpty()
    private val walletId = savedStateHandle.get<String>(InheritancePlanningActivity.EXTRA_WALLET_ID).orEmpty()

    private val _state = MutableStateFlow(
        InheritancePlanningState(
            groupId = savedStateHandle.get<String>(
                MembershipFragment.EXTRA_GROUP_ID
            ).orEmpty()
        )
    )
    val state = _state.asStateFlow()

    lateinit var setupOrReviewParam: InheritancePlanningParam.SetupOrReview
        private set

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
            getServerWalletUseCase(walletId).onSuccess { wallet ->
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
                    it.copy(keyTypes = keyTypes)
                }
            }
        }
    }

    fun setOrUpdate(param: InheritancePlanningParam) {
        if (param is InheritancePlanningParam.SetupOrReview) {
            setupOrReviewParam = param
        }
    }

    fun getGroupWalletType(): GroupWalletType? {
        return state.value.groupWalletType
    }
}

data class InheritancePlanningState(
    val groupId: String = "",
    val groupWalletType: GroupWalletType? = null,
    val keyTypes: List<InheritanceKeyType> = emptyList(),
)

enum class InheritanceKeyType {
    TAPSIGNER, COLDCARD
}

sealed class InheritancePlanningParam {
    data class SetupOrReview(
        val activationDate: Long = 0L,
        val walletId: String,
        val emails: List<String> = emptyList(),
        val isNotify: Boolean = false,
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

