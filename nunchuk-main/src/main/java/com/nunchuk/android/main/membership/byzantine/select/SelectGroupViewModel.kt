package com.nunchuk.android.main.membership.byzantine.select

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.usecase.membership.GetGroupAssistedWalletConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectGroupViewModel @Inject constructor(
    private val getGroupAssistedWalletConfigUseCase: GetGroupAssistedWalletConfigUseCase,
    private val membershipStepManager: MembershipStepManager,
) : ViewModel() {
    private val _state = MutableStateFlow(SelectGroupUiState(plan = membershipStepManager.plan))
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<SelectGroupEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            _event.emit(SelectGroupEvent.Loading(true))
            getGroupAssistedWalletConfigUseCase(Unit)
                .onSuccess { config ->
                    _state.update {
                        it.copy(
                            remainingByzantineWallet = config.remainingByzantineWallet + config.remainingFinneyWallet + config.remainingFinneyProWallet,
                            remainingByzantineProWallet = config.remainingByzantineProWallet,
                            remainingByzantinePremier = config.remainingPremierWallet,
                            options = config.allowWalletTypes,
                            isLoaded = true
                        )
                    }
                }
            _event.emit(SelectGroupEvent.Loading(false))
        }
    }

    fun checkGroupTypeAvailable(groupWalletType: GroupWalletType): Boolean {
        if (membershipStepManager.plan == MembershipPlan.BYZANTINE_PREMIER) {
            return (groupWalletType == GroupWalletType.TWO_OF_FOUR_MULTISIG && state.value.remainingByzantineProWallet > 0)
                    || (groupWalletType == GroupWalletType.THREE_OF_FIVE_INHERITANCE && state.value.remainingByzantineProWallet > 0)
                    || (groupWalletType == GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY && state.value.remainingByzantinePremier > 0)
                    || (groupWalletType == GroupWalletType.TWO_OF_FOUR_MULTISIG_NO_INHERITANCE && state.value.remainingByzantinePremier > 0)
                    || state.value.remainingByzantineWallet > 0
        }
        return (groupWalletType == GroupWalletType.TWO_OF_FOUR_MULTISIG && state.value.remainingByzantineProWallet > 0)
                || (groupWalletType == GroupWalletType.THREE_OF_FIVE_INHERITANCE && state.value.remainingByzantineProWallet > 0)
                || (groupWalletType == GroupWalletType.THREE_OF_FIVE_PLATFORM_KEY && state.value.remainingByzantineProWallet > 0)
                || state.value.remainingByzantineWallet > 0
    }
}

sealed class SelectGroupEvent {
    data class Loading(val isLoading: Boolean) : SelectGroupEvent()
}

data class SelectGroupUiState(
    val remainingByzantineWallet: Int = 0,
    val remainingByzantineProWallet: Int = 0,
    val remainingByzantinePremier: Int = 0,
    val options: List<GroupWalletType> = emptyList(),
    val isLoaded: Boolean = false,
    val plan : MembershipPlan = MembershipPlan.NONE,
)