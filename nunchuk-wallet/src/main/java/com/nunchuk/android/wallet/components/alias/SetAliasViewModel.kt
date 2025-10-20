package com.nunchuk.android.wallet.components.alias

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.usecase.byzantine.DeleteWalletAliasUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.byzantine.GetWalletAliasesUseCase
import com.nunchuk.android.usecase.byzantine.SetWalletAliasUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetailWithoutAliasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetAliasViewModel @Inject constructor(
    private val setWalletAliasUseCase: SetWalletAliasUseCase,
    private val deleteWalletAliasUseCase: DeleteWalletAliasUseCase,
    private val getWalletAliasesUseCase: GetWalletAliasesUseCase,
    assistedWalletManager: AssistedWalletManager,
    savedStateHandle: SavedStateHandle,
    private val getGroupUseCase: GetGroupUseCase,
    private val accountManager: AccountManager,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getWalletDetailWithoutAliasUseCase: GetWalletDetailWithoutAliasUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SetAliasState())
    val state = _state.asStateFlow()

    private val walletId = savedStateHandle.get<String>(walletIdArgument).orEmpty()
    private val groupId = assistedWalletManager.getGroupId(walletId).orEmpty()

    init {
        viewModelScope.launch {
            val members = getGroupUseCase(
                GetGroupUseCase.Params(
                    groupId = groupId
                )
            ).map { it.getOrThrow().members }
                .firstOrNull().orEmpty()

            val memberNames = members.associate { it.membershipId to it.getDisplayName() }
            val myEmail = accountManager.getAccount().email
            val myRole =
                members.find { it.emailOrUsername == myEmail }?.role.toRole
            val myMemberShipId =
                members.find { it.emailOrUsername == myEmail }?.membershipId.orEmpty()
            if (myRole == AssistedWalletRole.MASTER) {
                getWalletAliasesUseCase(
                    GetWalletAliasesUseCase.Params(
                        groupId = groupId,
                        walletId = walletId
                    )
                ).onSuccess { aliases ->
                    _state.update { state ->
                        state.copy(
                            memberAliases = aliases
                                .filter { it.membershipId != myMemberShipId }
                                .associate { memberNames[it.membershipId].orEmpty() to it.alias },
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            getWalletDetailWithoutAliasUseCase(walletId).onSuccess { wallet ->
                _state.update { state ->
                    state.copy(
                        defaultName = wallet.name
                    )
                }
            }
        }

        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map {
                it.getOrThrow()
            }.mapNotNull { it.find { wallet -> wallet.localId == walletId } }.collect { wallet ->
                _state.update { state ->
                    state.copy(
                        alias = wallet.alias
                    )
                }
            }
        }
    }

    fun onSaveAlias(alias: String) {
        viewModelScope.launch {
            setWalletAliasUseCase(
                SetWalletAliasUseCase.Params(
                    groupId = groupId,
                    walletId = walletId,
                    alias = alias
                )
            ).onSuccess {
                _state.update { state ->
                    state.copy(
                        setOrRemoveSuccess = true,
                        alias = alias
                    )
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        message = it.message
                    )
                }
            }
        }
    }

    fun onRemoveAlias() {
        viewModelScope.launch {
            deleteWalletAliasUseCase(
                DeleteWalletAliasUseCase.Params(
                    groupId = groupId,
                    walletId = walletId,
                )
            ).onSuccess {
                _state.update { state ->
                    state.copy(
                        setOrRemoveSuccess = true,
                        alias = ""
                    )
                }
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        message = it.message
                    )
                }
            }
        }
    }

    fun onHandledSetOrRemove() {
        _state.update { state ->
            state.copy(
                setOrRemoveSuccess = false
            )
        }
    }
}

data class SetAliasState(
    val alias: String = "",
    val defaultName: String = "",
    val setOrRemoveSuccess: Boolean = false,
    val message: String? = null,
    val memberAliases: Map<String, String> = emptyMap(),
)