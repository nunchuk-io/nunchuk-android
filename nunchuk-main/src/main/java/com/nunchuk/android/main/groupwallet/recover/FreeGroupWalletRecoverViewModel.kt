package com.nunchuk.android.main.groupwallet.recover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.free.groupwallet.RecoverGroupWalletUseCase
import com.nunchuk.android.usecase.signer.GetAllSignersUseCase
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FreeGroupWalletRecoverViewModel @Inject constructor(
    private val getAllSignersUseCase: GetAllSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
    private val singleSignerMapper: SingleSignerMapper,
    private val savedStateHandle: SavedStateHandle,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
    private val recoverGroupWalletUseCase: RecoverGroupWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
) : ViewModel() {
    val walletId: String
        get() = savedStateHandle.get<String>(FreeGroupWalletRecoverActivity.EXTRA_WALLET_ID).orEmpty()
    val filePath: String
        get() = savedStateHandle.get<String>(FreeGroupWalletRecoverActivity.EXTRA_FILE_PATH).orEmpty()

    private val _uiState = MutableStateFlow(FreeGroupWalletRecoverUiState())
    val uiState: StateFlow<FreeGroupWalletRecoverUiState> = _uiState.asStateFlow()
    private val singleSigners = mutableListOf<SingleSigner>()
    private val masterSigners = mutableListOf<MasterSigner>()

    init {
        loadInfo()
    }

    private fun loadSupportedSigners() = viewModelScope.launch {
        _uiState.mapNotNull { it.wallet?.addressType }.distinctUntilChanged()
            .collect { addressType ->
                if (addressType.isTaproot() && _uiState.value.supportedTypes.isEmpty()) {
                    getSupportedSignersUseCase(Unit).onSuccess { supportedTypes ->
                        _uiState.update { it.copy(supportedTypes = supportedTypes) }
                    }
                }
            }
        getSupportedSignersUseCase(Unit).onSuccess { supportedTypes ->
            _uiState.update { it.copy(supportedTypes = supportedTypes) }
        }
    }

    fun getSuggestedSigners(): List<SupportedSigner> {
        return _uiState.value.let { state ->
            state.supportedTypes.takeIf { state.wallet?.addressType?.isTaproot() == true }.orEmpty()
        }
    }

    fun loadInfo() = viewModelScope.launch {
        val curWallet = _uiState.value.wallet
        val newWalletSigners: List<SignerModel>
        if (curWallet == null) {
            val getWalletDetail = async { getWalletDetail2UseCase(walletId) }
            val newWalletDetail = getWalletDetail.await().getOrNull() ?: return@launch
            newWalletSigners = newWalletDetail.signers.map { signer -> singleSignerMapper(signer) }
            _uiState.update { it.copy(wallet = newWalletDetail) }
            deleteWallet()
        } else {
            newWalletSigners = curWallet.signers.map { signer -> singleSignerMapper(signer) }
        }
        val getNewAllSigner = async { getAllSignersUseCase(false) }

        val pairSigner = getNewAllSigner.await().getOrNull() ?: return@launch
        val singleSigner = pairSigner.second.filter { it.type != SignerType.SERVER }
        singleSigners.apply {
            clear()
            addAll(singleSigner)
        }
        masterSigners.apply {
            clear()
            addAll(pairSigner.first)
        }
        val allSigners = pairSigner.first.filter { it.type != SignerType.SERVER }
            .map { signer ->
                masterSignerMapper(signer)
            } + singleSigner.map { signer -> signer.toModel() }

        val oldAllSigners = _uiState.value.allSigners
        val index = savedStateHandle.get<Int>(CURRENT_SIGNER_INDEX) ?: -1

        Timber.tag("recover-group-wallet").e("Old all signers: ${oldAllSigners.size} - New all signers: ${allSigners.size}")
        if (allSigners.isNotEmpty() && oldAllSigners.isNotEmpty() && index != -1 && oldAllSigners.size != allSigners.size) {
            Timber.tag("recover-group-wallet").e("All signers size is different")
            val newSigner = allSigners.find { signer ->
                oldAllSigners.none { it.fingerPrint == signer.fingerPrint }
            }
            newSigner?.let {
                val selectSigner = _uiState.value.signerUis.find { it.index == index }?.signer
                Timber.tag("recover-group-wallet").e("New signer found: $it")
                if (selectSigner?.fingerPrint == newSigner.fingerPrint) {
                    Timber.tag("recover-group-wallet").e("New signer is selected")
                    val signerUi = SignerModelRecoverUi(signer = newSigner, index = index, isInDevice = true)
                    val signers = _uiState.value.signerUis.toMutableList()
                    signers[index] = signerUi
                    _uiState.update { it.copy(signerUis = signers) }
                } else {
                    _uiState.update { it.copy(showAddKeyErrorDialog = true) }
                }
            }
            setCurrentSignerIndex(-1)
        } else {
            Timber.tag("recover-group-wallet").e("All signers size is the same")

            val existingSigners = newWalletSigners.filter { signer ->
                allSigners.any { it.fingerPrint == signer.fingerPrint }
            }
            val notExistingSigners = newWalletSigners.filter { signer ->
                allSigners.none { it.fingerPrint == signer.fingerPrint }
            }
            val signerUis = mutableListOf<SignerModelRecoverUi>()
            signerUis.addAll(existingSigners.mapIndexed { index, signer ->
                SignerModelRecoverUi(signer = signer, index = index, isInDevice = true)
            })
            val signersSize = signerUis.size
            signerUis.addAll(notExistingSigners.mapIndexed { index, signer ->
                SignerModelRecoverUi(
                    signer = signer,
                    index = signersSize + index,
                    isInDevice = false
                )
            })
            _uiState.update { it.copy(signerUis = signerUis, allSigners = allSigners) }

            loadSupportedSigners()
        }
    }

    fun recoverGroupWallet() {
        Timber.tag("recover-group-wallet").e( "Recover group wallet")
        viewModelScope.launch {
            recoverGroupWalletUseCase(
                RecoverGroupWalletUseCase.Params(
                    name = _uiState.value.wallet?.name.orEmpty(),
                    filePath = filePath
                )
            ).onSuccess {
                Timber.tag("recover-group-wallet").e("Recover group wallet success")
                _uiState.update {  it.copy(event = FreeGroupWalletRecoverEvent.RecoverSuccess) }
            }.onFailure {
                Timber.tag("recover-group-wallet").e("Recover group wallet failed: $it")
                _uiState.update { it.copy(errorMessage = it.errorMessage) }
            }
        }
    }

    fun markMessageHandled() {
        _uiState.update { it.copy(errorMessage = "") }
    }

    fun markEventHandled() {
        _uiState.update { it.copy(event = FreeGroupWalletRecoverEvent.None) }
    }

    fun showAddKeyErrorDialogHandled() {
        _uiState.update { it.copy(showAddKeyErrorDialog = false) }
    }

    fun setCurrentSignerIndex(index: Int) {
        Timber.tag("recover-group-wallet").e( "Set current signer index $index")
        savedStateHandle[CURRENT_SIGNER_INDEX] = index
    }

    private fun deleteWallet() {
        viewModelScope.launch {
            deleteWalletUseCase.execute(walletId)
        }
    }

    fun getWallet() = _uiState.value.wallet

    companion object {
        private const val CURRENT_SIGNER_INDEX = "current_signer_index"
    }
}