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
                magic = magicPhrase,
                requiredKeyCount = init.inheritanceKeyCount,
                walletType = init.walletType
            )
        }
    }

    fun createSoftwareSignerFromMnemonic(mnemonic: String) {
        viewModelScope.launch {
            val currentData = _claimData.value
            val signerName = "$INHERITED_KEY_NAME #${currentData.signers.size + 1}"

            createSoftwareSignerUseCase(
                CreateSoftwareSignerUseCase.Param(
                    name = signerName,
                    mnemonic = mnemonic,
                )
            ).map {
                getDefaultSignerFromMasterSignerUseCase(
                    GetDefaultSignerFromMasterSignerUseCase.Params(
                        masterSignerId = it.id,
                        walletType = WalletType.MULTI_SIG,
                        addressType = AddressType.NATIVE_SEGWIT
                    )
                ).onFailure { e ->
                    Timber.e(e)
                    _uiState.update {
                        it.copy(
                            message = e.message.orUnknownError(),
                        )
                    }
                }.getOrNull()
            }.onSuccess { signer ->
                signer?.let {
                    addSigner(singleSignerMapper(signer))
                }
            }.onFailure {
                Timber.e(it)
                _uiState.update {
                    it.copy(
                        message = it.message.orUnknownError(),
                    )
                }
            }
        }
    }

    fun addSigner(signer: SignerModel) {
        _claimData.update {
            it.copy(signers = it.signers + signer)
        }
        if (claimData.value.signers.size == claimData.value.requiredKeyCount) {
            viewModelScope.launch {
                downloadWalletForClaim()
            }
        } else {
            // TODO Hai add more signers UI state
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
            getInheritanceClaimStateUseCase(
                GetInheritanceClaimStateUseCase.Param(
                    magic = currentData.magic,
                    bsms = wallet.bsms
                )
            ).onFailure { e ->
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        message = e.message.orUnknownError(),
                    )
                }
            }.getOrNull()
        }.onSuccess { inheritanceAdditional ->
            inheritanceAdditional?.let {
                _claimData.update {
                    it.copy(
                        inheritanceAdditional = inheritanceAdditional,
                    )
                }
            }
        }.onFailure { e ->
            if (e is NunchukApiException && e.code == 831) {
                _uiState.update {
                    it.copy(
                        isInheritanceNotFound = true,
                    )
                }
            } else {
                Timber.e(e)
                _uiState.update {
                    it.copy(
                        message = e.message.orUnknownError(),
                    )
                }
            }
        }
    }

    fun handledInheritanceNotFound() {
        _uiState.update { it.copy(isInheritanceNotFound = false,) }
    }

    fun handledMessageShown() {
        _uiState.update { it.copy(message = "",) }
    }

    private companion object {
        private const val KEY_CLAIM_DATA = "claim_data"
        private const val INHERITED_KEY_NAME = "Inheritance key"
    }
}

data class ClaimUiState(
    val message: String = "",
    val isInheritanceNotFound: Boolean = false,
)

@Parcelize
data class ClaimData(
    val signers: Set<SignerModel> = emptySet(),
    val magic: String = "",
    val derivationPaths: List<String> = emptyList(),
    val inheritanceAdditional: InheritanceAdditional? = null,
    val requiredKeyCount: Int = 1,
    val walletType: WalletType = WalletType.MULTI_SIG,
    val bsms: String? = null,
) : Parcelable

