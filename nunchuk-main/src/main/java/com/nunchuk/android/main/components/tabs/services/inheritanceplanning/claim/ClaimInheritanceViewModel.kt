package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.base.MutableSaveStateFlow
import com.nunchuk.android.core.base.update
import com.nunchuk.android.core.domain.ParseWalletDescriptorUseCase
import com.nunchuk.android.core.domain.membership.DownloadWalletForClaimUseCase
import com.nunchuk.android.core.domain.membership.GetClaimSigningChallengeUseCase
import com.nunchuk.android.core.domain.membership.GetClaimingWalletUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.network.ApiErrorCode.INHERITANCE_PLAN_NOT_ACTIVE
import com.nunchuk.android.core.network.ApiErrorCode.INHERITANCE_PLAN_NOT_FOUND
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.model.inheritance.ClaimSigningChallenge
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
import com.nunchuk.android.usecase.membership.DeletePendingRequestsByMagicUseCase
import com.nunchuk.android.usecase.membership.GetAddedKeysForInheritanceUseCase
import com.nunchuk.android.usecase.signer.GetDefaultSignerFromMasterSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ClaimInheritanceViewModel @Inject constructor(
    private val createSoftwareSignerUseCase: CreateSoftwareSignerUseCase,
    private val getDefaultSignerFromMasterSignerUseCase: GetDefaultSignerFromMasterSignerUseCase,
    private val downloadWalletForClaimUseCase: DownloadWalletForClaimUseCase,
    private val getInheritanceClaimStateUseCase: GetInheritanceClaimStateUseCase,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase,
    private val deleteMasterSignerUseCase: DeleteMasterSignerUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val getClaimingWalletUseCase: GetClaimingWalletUseCase,
    private val parseWalletDescriptorUseCase: ParseWalletDescriptorUseCase,
    private val getAddedKeysForInheritanceUseCase: GetAddedKeysForInheritanceUseCase,
    private val deletePendingRequestsByMagicUseCase: DeletePendingRequestsByMagicUseCase,
    private val getClaimSigningChallengeUseCase: GetClaimSigningChallengeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val handledRequestIds = mutableSetOf<String>()

    private val _claimData = MutableSaveStateFlow(
        savedStateHandle = savedStateHandle,
        key = KEY_CLAIM_DATA,
        defaultValue = ClaimData()
    )
    val claimData: StateFlow<ClaimData> = _claimData.asStateFlow()

    private val _uiState = MutableStateFlow(ClaimUiState())
    val uiState: StateFlow<ClaimUiState> = _uiState

    fun reset() {
        _claimData.update { ClaimData() }
        _uiState.update { ClaimUiState() }
        handledRequestIds.clear()
    }

    fun setClaimNoteData(
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
    ) {
        deletePendingRequests(magic)
        _claimData.update {
            it.copy(
                signers = signers.toSet(),
                magic = magic,
                inheritanceAdditional = inheritanceAdditional,
            )
        }
    }

    fun updateClaimInit(magicPhrase: String, init: InheritanceClaimingInit) {
        deletePendingRequests(magicPhrase)
        _claimData.update {
            it.copy(
                signers = emptySet(),
                signatures = emptyList(),
                magic = magicPhrase,
                requiredKeyCount = init.inheritanceKeyCount,
                walletType = init.walletType,
                keyOrigins = init.keyOrigins,
            )
        }
    }

    fun deletePendingRequests(magic: String) {
        viewModelScope.launch {
            if (magic.isNotEmpty()) {
                deletePendingRequestsByMagicUseCase(
                    DeletePendingRequestsByMagicUseCase.Param(
                        magic,
                    )
                )
            }
        }
    }

    fun createSoftwareSignerFromMnemonic(mnemonic: String, passphrase: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentData = _claimData.value
            val signerName = "$INHERITED_KEY_NAME #${currentData.signers.size + 1}"

            runCatching {
                getMasterFingerprintUseCase(
                    GetMasterFingerprintUseCase.Param(
                        mnemonic = mnemonic,
                        passphrase = passphrase
                    )
                ).getOrThrow()?.let {
                    deleteMasterSignerUseCase(it).getOrThrow()
                }
            }

            createSoftwareSignerUseCase(
                CreateSoftwareSignerUseCase.Param(
                    name = signerName,
                    mnemonic = mnemonic,
                )
            ).map { signer ->
                getDefaultSignerFromMasterSignerUseCase(
                    GetDefaultSignerFromMasterSignerUseCase.Params(
                        masterSignerId = signer.id,
                        walletType = WalletType.MULTI_SIG,
                        addressType = AddressType.NATIVE_SEGWIT
                    )
                ).onFailure { e ->
                    Timber.e(e)
                    _uiState.update {
                        it.copy(
                            event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                            isLoading = false
                        )
                    }
                }.getOrNull()
            }.onSuccess { signer ->
                signer?.let {
                    addSigner(singleSignerMapper(signer))
                }
            }.onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun addSigner(signer: SignerModel) {
        val hasSigner = _claimData.value.signers.any {
            it.fingerPrint == signer.fingerPrint && it.derivationPath == signer.derivationPath
        }
        if (hasSigner) {
            _uiState.update {
                it.copy(
                    event = ClaimInheritanceEvent.KeyAlreadyAdded,
                    isLoading = false
                )
            }
        } else {
            _claimData.update { it.copy(signers = it.signers + signer) }
            val isOffChainClaim = !_claimData.value.isOnChainClaim
            if (isOffChainClaim) {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.SignMessage(signer),
                        isLoading = false
                    )
                }
            } else if (claimData.value.signers.size == claimData.value.requiredKeyCount) {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.SignerAdded
                    )
                }
                viewModelScope.launch {
                    downloadWalletForClaim()
                }
            } else {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.AddMoreSigners,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun downloadWalletForClaim() {
        val currentData = _claimData.value
        downloadWalletForClaimUseCase(
            DownloadWalletForClaimUseCase.Param(
                magic = currentData.magic,
                keys = currentData.signers.map { it.toSingleSigner() },
            )
        ).map { wallet ->
            _uiState.update {
                it.copy(
                    isRequiredRegister = wallet.requiresRegistration,
                    walletId = wallet.localId
                )
            }
            getInheritanceStatus(currentData.magic, wallet.bsms)
        }.onFailure { e ->
            if (e is NunchukApiException && (e.code == INHERITANCE_PLAN_NOT_FOUND || e.code == INHERITANCE_PLAN_NOT_ACTIVE)) {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.NavigateToInheritanceError(
                            errorCode = e.code,
                            message = e.message
                        ),
                        isLoading = false
                    )
                }
            } else {
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun getInheritanceStatus(magic: String = "", bsms: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getInheritanceClaimStateUseCase(
                GetInheritanceClaimStateUseCase.Param(
                    magic = magic,
                    bsms = bsms
                )
            ).onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                    )
                }
            }.onSuccess { inheritanceAdditional ->
                inheritanceAdditional.let {
                    _claimData.update {
                        it.copy(
                            inheritanceAdditional = inheritanceAdditional,
                            bsms = bsms
                        )
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getClaimingWallet(bsms: String) = viewModelScope.launch {
        parseWalletDescriptorUseCase(bsms).onSuccess { localWallet ->
            getClaimingWalletUseCase(localWallet.id).onSuccess { wallet ->
                _uiState.update {
                    it.copy(
                        walletId = wallet.localId,
                        isRequiredRegister = wallet.requiresRegistration,
                    )
                }
                _claimData.update {
                    it.copy(
                        walletType = wallet.walletType,
                    )
                }
            }
        }
    }

    fun showImportFile() {
        _uiState.update {
            it.copy(
                event = ClaimInheritanceEvent.ImportFile,
            )
        }
    }

    fun onEventHandled() {
        _uiState.update { it.copy(event = null) }
    }

    fun checkRequestedAddDesktopKey() {
        viewModelScope.launch {
            getAddedKeysForInheritanceUseCase(
                GetAddedKeysForInheritanceUseCase.Param(
                    magic = claimData.value.magic
                )
            ).onSuccess { addedKeys ->
                addedKeys.forEach { entry ->
                    if (!handledRequestIds.contains(entry.key)) {
                        val signerModel = singleSignerMapper(entry.value)
                        addSigner(signerModel)
                    }
                }
                handledRequestIds.addAll(addedKeys.map { it.key })
            }.onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                    )
                }
            }
        }
    }

    fun generateClaimSigningChallengeIfNeeded() {
        viewModelScope.launch {
            val currentData = _claimData.value
            if (currentData.challenge == null && currentData.magic.isNotEmpty() && !currentData.isOnChainClaim) {
                getClaimSigningChallengeUseCase(currentData.magic)
                    .onSuccess { challenge ->
                        _uiState.update {
                            it.copy(
                                event = ClaimInheritanceEvent.GenerateChallengeSuccess,
                            )
                        }
                        _claimData.update { it.copy(challenge = challenge) }
                    }
                    .onFailure { e ->
                        Timber.e(e)
                        _uiState.update {
                            it.copy(
                                event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
                            )
                        }
                    }
            } else {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.GenerateChallengeSuccess,
                    )
                }
            }
        }
    }

    fun updateInheritanceAdditional(inheritanceAdditional: InheritanceAdditional) {
        _claimData.update { it.copy(inheritanceAdditional = inheritanceAdditional) }
    }

    fun addSignature(signature: String) {
        val currentData = _claimData.value
        _claimData.update {
            it.copy(
                signatures = currentData.signatures + signature
            )
        }
    }

    private companion object {
        private const val KEY_CLAIM_DATA = "claim_data"
        private const val INHERITED_KEY_NAME = "Inheritance key"
    }
}

data class ClaimUiState(
    val event: ClaimInheritanceEvent? = null,
    val isLoading: Boolean = false,
    val walletId: String = "",
    val isRequiredRegister: Boolean = false,
)

sealed class ClaimInheritanceEvent {
    data class ShowError(val message: String) : ClaimInheritanceEvent()
    data class NavigateToInheritanceError(
        val errorCode: Int,
        val message: String
    ) : ClaimInheritanceEvent()

    data object AddMoreSigners : ClaimInheritanceEvent()
    data object KeyAlreadyAdded : ClaimInheritanceEvent()
    data object SignerAdded : ClaimInheritanceEvent()
    data class SignMessage(val signer: SignerModel) : ClaimInheritanceEvent()
    data object GenerateChallengeSuccess : ClaimInheritanceEvent()
    data object ImportFile : ClaimInheritanceEvent()
}

@Parcelize
data class ClaimData(
    val signers: Set<SignerModel> = emptySet(),
    val signatures: List<String> = emptyList(),
    val magic: String = "",
    val inheritanceAdditional: InheritanceAdditional? = null,
    val requiredKeyCount: Int = 1,
    val walletType: WalletType = WalletType.MULTI_SIG,
    val keyOrigins: List<KeyOrigin> = emptyList(),
    val bsms: String? = null,
    val challenge: ClaimSigningChallenge? = null
) : Parcelable {
    val requiredSigners: List<SignerModel>
        get() = signers.takeIf { bsms.isNullOrEmpty() }.orEmpty().toList()

    val isOnChainClaim: Boolean
        get() = !bsms.isNullOrEmpty() || walletType == WalletType.MINISCRIPT

    val derivationPaths = keyOrigins.map { it.derivationPath }
}

