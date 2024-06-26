package com.nunchuk.android.app.onboard.unassisted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetLocalMembershipPlansFlowUseCase
import com.nunchuk.android.core.profile.SetOnBoardUseCase
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.wallet.CreateHotWalletUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UnAssistedIntroViewModel @Inject constructor(
    private val createHotWalletUseCase: CreateHotWalletUseCase,
    private val setOnBoardUseCase: SetOnBoardUseCase,
    private val getLocalMembershipPlansFlowUseCase: GetLocalMembershipPlansFlowUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(UnAssistedIntroState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getLocalMembershipPlansFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect {
                    _state.update { state -> state.copy(plans = it) }
                }
        }
        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .collect { groups ->
                    _state.update { state -> state.copy(isInByzantineGroup = groups.isNotEmpty()) }
                }
        }
    }

    fun createHotWallet() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            createHotWalletUseCase(Unit)
                .onSuccess {
                    setOnBoardUseCase(false)
                    _state.update { it.copy(openMainScreen = true) }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun handledOpenMainScreen() {
        _state.update { it.copy(openMainScreen = false) }
    }

    fun markOnBoardDone() {
        viewModelScope.launch {
            runCatching {
                setOnBoardUseCase(false)
            }.onSuccess {
                _state.update { it.copy(openMainScreen = true) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}

data class UnAssistedIntroState(
    val isLoading: Boolean = false,
    val openMainScreen: Boolean = false,
    val plans: List<MembershipPlan> = emptyList(),
    val isInByzantineGroup: Boolean = false,
)