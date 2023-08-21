package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.model.byzantine.InheritancePayload
import com.nunchuk.android.core.domain.byzantine.ParseInheritancePayloadUseCase
import com.nunchuk.android.core.domain.membership.CancelInheritanceUserDataUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceUserDataUseCase
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionType
import com.nunchuk.android.model.byzantine.isInheritanceFlow
import com.nunchuk.android.model.byzantine.isKeyHolder
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.usecase.byzantine.GetGroupDummyTransactionPayloadUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
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
class InheritanceReviewPlanGroupViewModel @Inject constructor(
    private val getGroupDummyTransactionPayloadUseCase: GetGroupDummyTransactionPayloadUseCase,
    private val parseInheritancePayloadUseCase: ParseInheritancePayloadUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getInheritanceUserDataUseCase: GetInheritanceUserDataUseCase,
    private val cancelInheritanceUserDataUseCase: CancelInheritanceUserDataUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val accountManager: AccountManager
) : ViewModel() {

    private lateinit var param: InheritancePlanningParam.SetupOrReview

    private val _event = MutableSharedFlow<InheritanceReviewPlanGroupEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceReviewPlanGroupState())
    val state = _state.asStateFlow()

    fun init(param: InheritancePlanningParam.SetupOrReview) {
        viewModelScope.launch {
            this@InheritanceReviewPlanGroupViewModel.param = param
            _event.emit(InheritanceReviewPlanGroupEvent.Loading(true))
            loadMembers()
            if (param.dummyTransactionId.isNotEmpty()) {
                getGroupDummyTransactionPayloadUseCase(
                    GetGroupDummyTransactionPayloadUseCase.Param(
                        groupId = param.groupId,
                        transactionId = param.dummyTransactionId,
                        walletId = param.walletId
                    )
                ).onSuccess { payload ->
                    if (payload.type.isInheritanceFlow()) {
                        parseInheritancePayloadUseCase(payload).onSuccess { parsePayload ->
                            _state.update {
                                it.copy(
                                    payload = parsePayload,
                                    dummyTransactionId = param.dummyTransactionId,
                                    requiredSignature = CalculateRequiredSignatures(
                                        VerificationType.SIGN_DUMMY_TX,
                                        payload.requiredSignatures
                                    ),
                                    requestByUserId = payload.requestByUserId,
                                    type = payload.type,
                                    pendingSignatures = payload.pendingSignatures
                                )
                            }
                        }

                        getWalletDetail2UseCase(param.walletId).onSuccess { wallet ->
                            _state.update { it.copy(walletName = wallet.name) }
                        }
                    }
                }
            }
            _event.emit(InheritanceReviewPlanGroupEvent.Loading(false))
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            val group = getGroupUseCase(param.groupId).getOrNull()
            val myEmail = accountManager.getAccount().email
            val members = group?.members.orEmpty().mapNotNull { member ->
                if (member.role.toRole.isKeyHolder) {
                    AssistedMember(
                        role = member.role,
                        email = member.emailOrUsername,
                        name = member.user?.name,
                        membershipId = member.membershipId,
                        userId = member.user?.id.orEmpty(),
                    )
                } else null
            }
            _state.update { state ->
                state.copy(
                    members = members,
                    myRole = members.find { it.email == myEmail }?.role?.toRole
                        ?: AssistedWalletRole.NONE
                )
            }
        }
    }

    fun onContinueClick() {
        viewModelScope.launch {
            if (state.value.dummyTransactionId.isNotEmpty()) {
                val resultUserData = if (isCreateOrUpdateFlow().not()) {
                    cancelInheritanceUserDataUseCase(
                        CancelInheritanceUserDataUseCase.Param(
                            walletId = param.walletId,
                            groupId = param.groupId
                        )
                    )
                } else {
                    getInheritanceUserDataUseCase(
                        GetInheritanceUserDataUseCase.Param(
                            walletId = param.walletId,
                            note = state.value.payload.newData?.note.orEmpty(),
                            notificationEmails = state.value.payload.newData?.notificationEmails?.toList()
                                .orEmpty(),
                            notifyToday = state.value.payload.newData?.notifyToday.orFalse(),
                            activationTimeMilis = state.value.payload.newData?.activationTimeMilis
                                ?: 0,
                            bufferPeriodId = state.value.payload.newData?.bufferPeriod?.id,
                            groupId = param.groupId
                        )
                    )
                }
                _event.emit(
                    InheritanceReviewPlanGroupEvent.OnContinue(
                        resultUserData.getOrThrow(),
                        _state.value.requiredSignature,
                        _state.value.dummyTransactionId
                    )
                )
                return@launch
            }
        }
    }

    private fun isCreateOrUpdateFlow() =
        state.value.type != DummyTransactionType.CANCEL_INHERITANCE_PLAN

    fun getDummyTransactionType() = state.value.type
}

sealed class InheritanceReviewPlanGroupEvent {
    data class Loading(val loading: Boolean) : InheritanceReviewPlanGroupEvent()
    data class OnContinue(
        val userData: String,
        val requiredSignatures: CalculateRequiredSignatures,
        val dummyTransactionId: String
    ) : InheritanceReviewPlanGroupEvent()

    data class ProcessFailure(val message: String) : InheritanceReviewPlanGroupEvent()
    object CreateOrUpdateInheritanceSuccess : InheritanceReviewPlanGroupEvent()
    object CancelInheritanceSuccess : InheritanceReviewPlanGroupEvent()
}

data class InheritanceReviewPlanGroupState(
    val payload: InheritancePayload = InheritancePayload(),
    val members: List<AssistedMember> = emptyList(),
    val dummyTransactionId: String = "",
    val walletName: String = "",
    val requestByUserId: String = "",
    val requiredSignature: CalculateRequiredSignatures = CalculateRequiredSignatures(),
    val pendingSignatures: Int = 0,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val type: DummyTransactionType = DummyTransactionType.NONE,
)

