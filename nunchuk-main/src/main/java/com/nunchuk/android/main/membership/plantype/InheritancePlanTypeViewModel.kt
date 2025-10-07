package com.nunchuk.android.main.membership.plantype

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.SetLocalMembershipPlanFlowUseCase
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.model.toMembershipPlan
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.wallet.InitPersonalWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritancePlanTypeViewModel @Inject constructor(
    private val initPersonalWalletUseCase: InitPersonalWalletUseCase,
    private val setLocalMembershipPlanFlowUseCase: SetLocalMembershipPlanFlowUseCase,
    private val applicationScope: CoroutineScope,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(InheritancePlanTypeUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<InheritancePlanTypeEvent>()
    val event = _event.asSharedFlow()

    private val args: InheritancePlanTypeFragmentArgs =
        InheritancePlanTypeFragmentArgs.fromSavedStateHandle(savedStateHandle)

    fun onPlanTypeSelected(planType: InheritancePlanType) {
        viewModelScope.launch {
            _state.emit(_state.value.copy(selectedPlanType = planType))
        }
    }

    fun onContinueClicked() {
        viewModelScope.launch {
            // Call setLocalMembershipPlan when user continues
            args.slug?.let { slug ->
                args.walletType?.let { walletTypeStr ->
                    // Convert String to GroupWalletType
                    val groupWalletType = try {
                        GroupWalletType.valueOf(walletTypeStr)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    groupWalletType?.let { walletType ->
                        setLocalMembershipPlan(slug, walletType, args.groupId)
                    }
                }
            }
            _event.emit(InheritancePlanTypeEvent.OnContinueClicked(_state.value.selectedPlanType))
        }
    }

    private fun setLocalMembershipPlan(slug: String, type: GroupWalletType, groupId: String?) {
        applicationScope.launch {
            val plan = slug.toMembershipPlan()
            // Determine wallet type based on selected plan type
            val walletType = when (_state.value.selectedPlanType) {
                InheritancePlanType.OFF_CHAIN -> WalletType.MULTI_SIG
                InheritancePlanType.ON_CHAIN -> WalletType.MINISCRIPT
            }
            initPersonalWalletUseCase(
                InitPersonalWalletUseCase.Param(
                    walletConfig = WalletConfig(
                        allowInheritance = type.allowInheritance,
                        m = type.m,
                        n = type.n,
                        requiredServerKey = type.requiredServerKey
                    ),
                    groupId = groupId,
                    walletType = walletType
                )
            )
            setLocalMembershipPlanFlowUseCase(plan)
        }
    }
}
