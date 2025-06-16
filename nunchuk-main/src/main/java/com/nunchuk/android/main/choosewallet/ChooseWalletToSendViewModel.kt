package com.nunchuk.android.main.choosewallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.wallet.WalletUiModel
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.usecase.GetGroupsUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.free.groupwallet.GetGroupWalletsUseCase
import com.nunchuk.android.utils.ByzantineGroupUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChooseWalletToSendViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val byzantineGroupUtils: ByzantineGroupUtils,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getGroupWalletsUseCase: GetGroupWalletsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChooseWalletToSendUiState())
    val state = _state.asStateFlow()

    init {
        loadWallets()
    }

    private fun loadWallets() {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .catch {
                    Timber.e(it)
                }
                .collect { wallets ->
                    _state.update { it.copy(wallets = wallets, hasNoWallets = wallets.isEmpty()) }
                    composeWalletUiModels()
                }
        }
        viewModelScope.launch {
            getGroupWalletsUseCase(Unit)
                .onSuccess { wallets ->
                    _state.update { it.copy(groupWallets = wallets.map { wallet -> wallet.id }.toHashSet()) }
                    composeWalletUiModels()
                }
        }

        viewModelScope.launch {
            getGroupsUseCase(Unit)
                .collect { result ->
                    val groups = result.getOrDefault(emptyList())
                    val joinedGroups =
                        groups.filter { byzantineGroupUtils.isPendingAcceptInvite(it).not() }
                    val roles = groups.associateBy(
                        { it.id },
                        { byzantineGroupUtils.getCurrentUserRole(it).toRole }
                    )
                    _state.update {
                        it.copy(joinedGroups = joinedGroups.associateBy { it.id }, roles = roles)
                    }
                }
        }

        viewModelScope.launch {
            getAssistedWalletsFlowUseCase(Unit)
                .map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect { wallets ->
                    _state.update {
                        it.copy(assistedWallets = wallets.associateBy { it.localId })
                    }
                    composeWalletUiModels()
                }
        }
    }

    private fun composeWalletUiModels() {
        val wallets = state.value.wallets
        if (wallets.isEmpty()) return

        val uis = arrayListOf<WalletUiModel>()
        wallets.forEach { wallet ->
            val assistedWallet = state.value.assistedWallets[wallet.wallet.id]
            var group: ByzantineGroup? = null
            if (assistedWallet?.groupId.isNullOrEmpty().not()) {
                group = state.value.joinedGroups[assistedWallet?.groupId] ?: return@forEach
            }
            val role = state.value.roles[assistedWallet?.groupId]
            val walletStatus = assistedWallet?.status ?: ""
            if (walletStatus == WalletStatus.REPLACED.name) return@forEach

            uis.add(
                WalletUiModel(
                    wallet = wallet,
                    assistedWallet = assistedWallet,
                    isAssistedWallet = assistedWallet?.status == WalletStatus.ACTIVE.name,
                    group = group,
                    role = role ?: AssistedWalletRole.NONE,
                    walletStatus = walletStatus,
                    isGroupWallet = state.value.groupWallets.contains(wallet.wallet.id)
                )
            )
        }
        _state.update { it.copy(walletUiModels = uis) }
    }
} 