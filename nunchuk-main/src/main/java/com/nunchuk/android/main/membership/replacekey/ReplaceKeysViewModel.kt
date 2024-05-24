package com.nunchuk.android.main.membership.replacekey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.key.SetReplacingKeyXfpUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.replace.FinalizeReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.GetReplaceWalletStatusUseCase
import com.nunchuk.android.usecase.replace.InitReplaceKeyUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplaceKeysViewModel @Inject constructor(
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val accountManager: AccountManager,
    private val setReplacingKeyXfpUseCase: SetReplacingKeyXfpUseCase,
    private val getReplaceWalletStatusUseCase: GetReplaceWalletStatusUseCase,
    private val finalizeReplaceKeyUseCase: FinalizeReplaceKeyUseCase,
    private val initReplaceKeyUseCase: InitReplaceKeyUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = ReplaceKeysFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _uiState = MutableStateFlow(ReplaceKeysUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                _uiState.update {
                    it.copy(signers = wallet.signers.filter { signer -> signer.type != SignerType.SERVER }
                        .map { signer -> signer.toModel() })
                }
            }
        }
        if (args.groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGroupUseCase(
                    GetGroupUseCase.Params(args.groupId)
                ).map { it.getOrThrow() }
                    .distinctUntilChanged()
                    .collect { group ->
                        _uiState.update {
                            it.copy(group = group, myRole = currentUserRole(group.members))
                        }
                    }
            }
        }
        getReplaceWalletStatus()
    }

    fun getReplaceWalletStatus() {
        viewModelScope.launch {
            getReplaceWalletStatusUseCase(
                GetReplaceWalletStatusUseCase.Param(args.groupId, args.walletId)
            ).onSuccess { status ->
                _uiState.update {
                    val verifiedSigners = status.signers.values
                        .filter { signer -> signer.verifyType != VerifyType.NONE }
                        .mapNotNull { signer -> signer.xfp }
                        .toSet()
                    it.copy(
                        replaceSigners = status.signers.mapValues { entry ->
                            entry.value.toModel()
                        },
                        verifiedSigners = verifiedSigners
                    )
                }
            }
        }
    }

    fun onCreateWallet() {
        viewModelScope.launch {
            finalizeReplaceKeyUseCase(
                FinalizeReplaceKeyUseCase.Param(groupId = args.groupId, walletId = args.walletId)
            ).onSuccess { wallet ->
                _uiState.update { it.copy(createWalletSuccess = StateEvent.String(wallet.id)) }
            }
        }
    }

    private fun currentUserRole(members: List<ByzantineMember>): AssistedWalletRole {
        return members.firstOrNull {
            it.emailOrUsername == accountManager.getAccount().email
                    || it.emailOrUsername == accountManager.getAccount().username
        }?.role.toRole
    }

    fun setReplacingXfp(xfp: String) {
        viewModelScope.launch {
            savedStateHandle[REPLACE_XFP] = xfp
        }
    }

    fun initReplaceKey() {
        viewModelScope.launch {
            initReplaceKeyUseCase(
                InitReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()
                )
            )
        }
    }

    fun markOnCreateWalletSuccess() {
        _uiState.update { it.copy(createWalletSuccess = StateEvent.None) }
    }

    val replacedXfp: String
        get() = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()

    companion object {
        const val REPLACE_XFP = "REPLACE_XFP"
    }
}

data class ReplaceKeysUiState(
    val signers: List<SignerModel> = emptyList(),
    val replaceSigners: Map<String, SignerModel> = emptyMap(),
    val verifiedSigners: Set<String> = emptySet(),
    val group: ByzantineGroup? = null,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val createWalletSuccess: StateEvent = StateEvent.None
)