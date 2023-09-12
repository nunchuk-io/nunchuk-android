package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.util.LoadingOptions
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val groupId = savedStateHandle.get<String>(MembershipActivity.EXTRA_GROUP_ID).orEmpty()

    private val _state = MutableStateFlow(
        InheritancePlanningState(
            groupId = savedStateHandle.get<String>(
                MembershipActivity.EXTRA_GROUP_ID
            ).orEmpty()
        )
    )
    val state = _state.asStateFlow()

    lateinit var setupOrReviewParam: InheritancePlanningParam.SetupOrReview
        private set

    init {
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupUseCase(GetGroupUseCase.Params(groupId, loadingOptions = LoadingOptions.OFFLINE_ONLY))
                    .filter { it.isSuccess }
                    .map { it.getOrThrow() }
                    .collect { groupBrief ->
                        _state.update { it.copy(groupWalletType = groupBrief.walletConfig.toGroupWalletType()) }
                    }
            }
        }
    }

    fun setOrUpdate(param: InheritancePlanningParam) {
        if (param is InheritancePlanningParam.SetupOrReview) {
            setupOrReviewParam = param
        }
    }
}

data class InheritancePlanningState(
    val groupId: String = "",
    val groupWalletType: GroupWalletType? = null,
)

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
        val isOpenFromWizard: Boolean = false,
        val groupId: String = "",
        val dummyTransactionId: String = ""
    ) : InheritancePlanningParam()
}

