package com.nunchuk.android.main.membership.replacekey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isRecommendedPath
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.usecase.byzantine.GetGroupUseCase
import com.nunchuk.android.usecase.replace.FinalizeReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.GetReplaceWalletStatusUseCase
import com.nunchuk.android.usecase.replace.InitReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.ReplaceKeyUseCase
import com.nunchuk.android.usecase.replace.ResetReplaceKeyUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
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
    private val getReplaceWalletStatusUseCase: GetReplaceWalletStatusUseCase,
    private val finalizeReplaceKeyUseCase: FinalizeReplaceKeyUseCase,
    private val initReplaceKeyUseCase: InitReplaceKeyUseCase,
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val replaceKeyUseCase: ReplaceKeyUseCase,
    private val updateRemoteSignerUseCase: UpdateRemoteSignerUseCase,
    private val resetReplaceKeyUseCase: ResetReplaceKeyUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val nfcFileManager: NfcFileManager
) : ViewModel() {
    private val args = ReplaceKeysFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _uiState = MutableStateFlow(ReplaceKeysUiState())
    val uiState = _uiState.asStateFlow()
    private val singleSigners = mutableListOf<SingleSigner>()
    private val replacedSigners = mutableListOf<SignerServer>()

    init {
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId).onSuccess { wallet ->
                _uiState.update {
                    it.copy(walletSigners = wallet.signers.filter { signer -> signer.type != SignerType.SERVER }
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
        loadSigners()
    }

    private fun loadSigners() {
        viewModelScope.launch {
            getAllSignersUseCase(false).onSuccess { pair ->
                val singleSigner = pair.second.distinctBy { it.masterFingerprint }
                singleSigners.apply {
                    clear()
                    addAll(singleSigner)
                }
                val signers = pair.first.map { signer ->
                    masterSignerMapper(signer)
                } + singleSigner.map { signer -> signer.toModel() }
                _uiState.update { it.copy(signers = signers) }
            }
        }
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
                    replacedSigners.apply {
                        clear()
                        addAll(status.signers.values)
                    }
                    it.copy(
                        replaceSigners = status.signers.mapValues { entry ->
                            entry.value.toModel()
                        },
                        verifiedSigners = verifiedSigners
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(message = it.message) }
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


    fun getTapSigners() =
        _uiState.value.signers.filter { it.type == SignerType.NFC && isSignerExist(it.fingerPrint).not() }

    fun getColdcard() = _uiState.value.signers.filter {
        (it.type == SignerType.COLDCARD_NFC
                && it.derivationPath.isRecommendedPath
                && isSignerExist(it.fingerPrint).not()) || (it.type == SignerType.AIRGAP && it.tags.isEmpty())
    }

    fun getAirgap(tag: SignerTag?): List<SignerModel> {
        return if (tag == null) {
            _uiState.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
            }
        } else {
            _uiState.value.signers.filter {
                it.type == SignerType.AIRGAP
                        && isSignerExist(it.fingerPrint).not()
                        && (it.tags.contains(tag) || it.tags.isEmpty())
            }
        }
    }

    fun getHardwareSigners(tag: SignerTag) =
        _uiState.value.signers.filter { it.type == SignerType.HARDWARE && it.tags.contains(tag) }

    fun getSoftwareSigners() =
        _uiState.value.signers.filter {
            (it.type == SignerType.SOFTWARE || it.type == SignerType.FOREIGN_SOFTWARE) && isSignerExist(
                it.fingerPrint
            ).not()
        }

    fun markOnCreateWalletSuccess() {
        _uiState.update { it.copy(createWalletSuccess = StateEvent.None) }
    }

    private fun isSignerExist(masterSignerId: String) =
        _uiState.value.replaceSigners.containsKey(masterSignerId)
                && _uiState.value.walletSigners.any { it.fingerPrint == masterSignerId }

    fun getCountWalletSoftwareSignersInDevice(): Int {
        val replaceSigners = _uiState.value.replaceSigners
        return _uiState.value.walletSigners.count {
            val signer = replaceSigners[it.fingerPrint] ?: it
            (signer.type == SignerType.SOFTWARE && signer.isVisible)
        }
    }

    fun onUpdateSignerTag(signer: SignerModel, tag: SignerTag) {
        viewModelScope.launch {
            singleSigners.find { it.masterFingerprint == signer.fingerPrint && it.derivationPath == signer.derivationPath }
                ?.let { singleSigner ->
                    val result =
                        updateRemoteSignerUseCase.execute(singleSigner.copy(tags = listOf(tag)))
                    if (result is Result.Success) {
                        loadSigners()
                        onReplaceKey(singleSigner.copy(tags = listOf(tag)))
                    } else {
                        _uiState.update { it.copy(message = (result as Result.Error).exception.message.orUnknownError()) }
                    }
                }
        }
    }

    fun onReplaceKey(signer: SingleSigner) {
        viewModelScope.launch {
            replaceKeyUseCase(
                ReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId,
                    xfp = savedStateHandle.get<String>(REPLACE_XFP).orEmpty(),
                    signer = signer
                )
            ).onSuccess {
                getReplaceWalletStatus()
            }.onFailure {
                _uiState.update { it.copy(message = it.message) }
            }
        }
    }

    fun onHandledMessage() {
        _uiState.update { it.copy(message = "") }
    }

    fun onCancelReplaceWallet() {
        viewModelScope.launch {
            resetReplaceKeyUseCase(
                ResetReplaceKeyUseCase.Param(
                    groupId = args.groupId,
                    walletId = args.walletId
                )
            ).onFailure {
                _uiState.update { it.copy(message = it.message) }
            }
        }
    }

    fun getKeyId(xfp: String): String {
        return replacedSigners.find { it.xfp == xfp }?.tapsignerKeyId.orEmpty()
    }
    fun getKeyChecksum(xfp: String): String? = null
    fun getFilePath(xfp: String) = nfcFileManager.buildFilePath(getKeyId(xfp))

    val replacedXfp: String
        get() = savedStateHandle.get<String>(REPLACE_XFP).orEmpty()

    companion object {
        const val REPLACE_XFP = "REPLACE_XFP"
    }
}

data class ReplaceKeysUiState(
    val walletSigners: List<SignerModel> = emptyList(),
    val replaceSigners: Map<String, SignerModel> = emptyMap(),
    val verifiedSigners: Set<String> = emptySet(),
    val group: ByzantineGroup? = null,
    val myRole: AssistedWalletRole = AssistedWalletRole.NONE,
    val createWalletSuccess: StateEvent = StateEvent.None,
    val signers: List<SignerModel> = emptyList(),
    val message: String = ""
)