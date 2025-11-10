package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.base.MutableSaveStateFlow
import com.nunchuk.android.core.base.update
import com.nunchuk.android.core.domain.membership.DownloadWalletForClaimUseCase
import com.nunchuk.android.core.domain.membership.GetInheritanceClaimStateUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toSingleSigner
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.InheritanceAdditional
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.usecase.DeleteMasterSignerUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _claimData = MutableSaveStateFlow(
        savedStateHandle = savedStateHandle,
        key = KEY_CLAIM_DATA,
        defaultValue = ClaimData()
    )
    val claimData: StateFlow<ClaimData> = _claimData.asStateFlow()

    private val _uiState = MutableStateFlow(ClaimUiState())
    val uiState: StateFlow<ClaimUiState> = _uiState

    fun setClaimNoteData(
        signers: List<SignerModel>,
        magic: String,
        inheritanceAdditional: InheritanceAdditional,
        derivationPaths: List<String>
    ) {
        _claimData.update {
            it.copy(
                signers = signers.toSet(),
                magic = magic,
                derivationPaths = derivationPaths,
                inheritanceAdditional = inheritanceAdditional,
            )
        }
    }

    fun updateClaimInit(magicPhrase: String, init: InheritanceClaimingInit) {
        _claimData.update {
            it.copy(
                signers = emptySet(),
                magic = magicPhrase,
                requiredKeyCount = init.inheritanceKeyCount,
                walletType = init.walletType
            )
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
        if (_claimData.value.signers.contains(signer)) {
            _uiState.update { it.copy(event = ClaimInheritanceEvent.KeyAlreadyAdded) }
        } else {
            _claimData.update {
                it.copy(signers = it.signers + signer)
            }
            if (claimData.value.signers.size == claimData.value.requiredKeyCount) {
                viewModelScope.launch {
                    downloadWalletForClaim()
                }
            } else {
                _uiState.update { it.copy(event = ClaimInheritanceEvent.AddMoreSigners) }
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
            getInheritanceStatus(currentData.magic, wallet.bsms)
        }.onFailure { e ->
            if (e is NunchukApiException && e.code == 831) {
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.NavigateToNoInheritanceFound,
                    )
                }
            } else {
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        event = ClaimInheritanceEvent.ShowError(e.message.orUnknownError()),
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

    fun onEventHandled() {
        _uiState.update { it.copy(event = null) }
    }

    private companion object {
        private const val KEY_CLAIM_DATA = "claim_data"
        private const val INHERITED_KEY_NAME = "Inheritance key"
    }
}

data class ClaimUiState(
    val event: ClaimInheritanceEvent? = null,
    val isLoading: Boolean = false,
)

sealed class ClaimInheritanceEvent {
    data class ShowError(val message: String) : ClaimInheritanceEvent()
    data object NavigateToNoInheritanceFound : ClaimInheritanceEvent()
    data object AddMoreSigners : ClaimInheritanceEvent()
    data object KeyAlreadyAdded : ClaimInheritanceEvent()
}

@Parcelize
data class ClaimData(
    val signers: Set<SignerModel> = emptySet(),
    val magic: String = "",
    val derivationPaths: List<String> = emptyList(),
    val inheritanceAdditional: InheritanceAdditional? = null,
    val requiredKeyCount: Int = 1,
    val walletType: WalletType = WalletType.MULTI_SIG,
    val bsms: String? = null,
) : Parcelable {
    val requiredSigners: List<SignerModel>
        get() = signers.takeIf { bsms.isNullOrEmpty() }.orEmpty().toList()
}

