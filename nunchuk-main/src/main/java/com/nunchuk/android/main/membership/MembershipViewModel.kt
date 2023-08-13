package com.nunchuk.android.main.membership

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.main.membership.model.toGroupWalletType
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.usecase.byzantine.GetGroupBriefByIdFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val getGroupBriefByIdFlowUseCase: GetGroupBriefByIdFlowUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel(){
    private val groupId = savedStateHandle.get<String>(MembershipActivity.EXTRA_GROUP_ID).orEmpty()

    private val _state = MutableStateFlow(MembershipState(groupId = savedStateHandle.get<String>(MembershipActivity.EXTRA_GROUP_ID).orEmpty()))
    val state = _state.asStateFlow()

    init {
        if (groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupBriefByIdFlowUseCase(groupId)
                    .filter { it.isSuccess }
                    .map { it.getOrThrow() }
                    .collect { groupBrief ->
                        _state.update { it.copy(groupWalletType = groupBrief.walletConfig.toGroupWalletType()) }
                    }
            }
        }
    }
}

data class MembershipState(
    val groupId: String = "",
    val groupWalletType: GroupWalletType? = null,
)