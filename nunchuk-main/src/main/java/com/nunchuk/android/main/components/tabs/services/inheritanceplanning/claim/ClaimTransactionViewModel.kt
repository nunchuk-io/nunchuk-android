package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.SignTapSignerPsbtUseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.nav.args.ClaimTransactionArgs
import com.nunchuk.android.transaction.components.details.TransactionDetailsState
import com.nunchuk.android.transaction.components.details.TransactionMiniscriptUiState
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.membership.InheritanceClaimingClaimUseCase
import com.nunchuk.android.usecase.signer.GetRemoteOrMasterSignerUseCase
import com.nunchuk.android.usecase.signer.SignSoftwarePsbtUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileOutputStream

@HiltViewModel(assistedFactory = ClaimTransactionViewModel.Factory::class)
class ClaimTransactionViewModel @AssistedInject constructor(
    private val getRemoteOrMasterSignerUseCase: GetRemoteOrMasterSignerUseCase,
    private val singleSignerMapper: SingleSignerMapper,
    private val signSoftwarePsbtUseCase: SignSoftwarePsbtUseCase,
    private val signTapSignerPsbtUseCase: SignTapSignerPsbtUseCase,
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
    private val inheritanceClaimingClaimUseCase: InheritanceClaimingClaimUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val saveLocalFileUseCase: SaveLocalFileUseCase,
    @Assisted private val args: ClaimTransactionArgs
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionDetailsState())
    val state: StateFlow<TransactionDetailsState> = _state.asStateFlow()
    private val _singleSigners = mutableListOf<SingleSigner>()

    private val _miniscriptState = MutableStateFlow(TransactionMiniscriptUiState())
    val miniscriptState: StateFlow<TransactionMiniscriptUiState> = _miniscriptState.asStateFlow()

    private val _loadingType = MutableStateFlow<LoadingType?>(null)
    val loadingType: StateFlow<LoadingType?> = _loadingType.asStateFlow()

    private val _needPassphrase = MutableStateFlow<String?>(null)
    val needPassphrase: StateFlow<String?> = _needPassphrase.asStateFlow()

    private val _event = MutableSharedFlow<ClaimTransactionEvent>()
    val event = _event.asSharedFlow()

    init {
        loadSigners()
    }

    private fun loadSigners() {
        if (args.masterSignerIds.size != args.derivationPaths.size) {
            Timber.e("Master signer IDs and derivation paths must have the same size")
            return
        }

        viewModelScope.launch {
            _loadingType.update { LoadingType.Normal }
            try {
                val signers = mutableListOf<SignerModel>()

                val singleSigners = args.masterSignerIds.mapIndexed { index, masterSignerId ->
                    getRemoteOrMasterSignerUseCase(
                        GetRemoteOrMasterSignerUseCase.Data(
                            id = masterSignerId,
                            derivationPath = args.derivationPaths.getOrNull(index).orEmpty()
                        )
                    ).getOrThrow()
                }

                for (singleSigner in singleSigners) {
                    val signerModel = singleSignerMapper(singleSigner)
                    signers.add(signerModel)
                }
                _singleSigners.apply {
                    clear()
                    addAll(singleSigners)
                }

                _state.update { currentState ->
                    currentState.copy(
                        transaction = args.transaction,
                        signers = signers,
                        enabledSigners = emptySet()
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load signers")
            } finally {
                _loadingType.update { null }
            }
        }
    }

    fun checkSoftwarePassphrase(signerModel: SignerModel) {
        val singleSigner = _singleSigners.firstOrNull {
            it.masterFingerprint == signerModel.fingerPrint && it.derivationPath == signerModel.derivationPath
        } ?: return
        viewModelScope.launch {
            if (singleSigner.hasMasterSigner) {
                getMasterSignerUseCase(singleSigner.masterSignerId).onSuccess { masterSigner ->
                    if (masterSigner.device.needPassPhraseSent) {
                        _needPassphrase.update { singleSigner.masterSignerId }
                    } else {
                        signSoftwarePsbt(singleSigner)
                    }
                }.onFailure {
                    Timber.e(it, "Failed to get master signer")
                }
            } else {
                signSoftwarePsbt(singleSigner)
            }
        }
    }

    fun handlePassphrase(passphrase: String) {
        val masterSignerId = _needPassphrase.value ?: return
        viewModelScope.launch {
            sendSignerPassphraseUseCase(
                SendSignerPassphraseUseCase.Param(
                    signerId = masterSignerId,
                    passphrase = passphrase
                )
            ).onSuccess {
                val singleSigner = _singleSigners.firstOrNull {
                    it.masterSignerId == masterSignerId
                } ?: return@launch
                signSoftwarePsbt(singleSigner)
                _needPassphrase.update { null }
            }.onFailure {
                Timber.e(it, "Failed to send passphrase")
            }
        }
    }

    private fun signSoftwarePsbt(singleSigner: SingleSigner) {
        viewModelScope.launch {
            _loadingType.update { LoadingType.Normal }
            val transaction = args.transaction

            val result = signSoftwarePsbtUseCase(
                SignSoftwarePsbtUseCase.Param(
                    masterSignerId = singleSigner.masterSignerId,
                    signers = _singleSigners,
                    psbt = transaction.psbt,
                    subAmount = transaction.subAmount.value.toString(),
                    feeRate = transaction.feeRate.value.toString(),
                    fee = transaction.fee.value.toString(),
                    subtractFeeFromAmount = transaction.subtractFeeFromAmount
                )
            )

            _loadingType.update { null }

            if (result.isSuccess) {
                val signedTransaction = result.getOrThrow()
                _state.update { currentState ->
                    currentState.copy(
                        transaction = signedTransaction
                    )
                }
                checkAndClaimIfAllSigned(signedTransaction)
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to sign PSBT with software signer")
            }
        }
    }

    fun signTapSignerPsbt(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            _loadingType.update { LoadingType.Nfc }
            val transaction = args.transaction

            val result = signTapSignerPsbtUseCase(
                SignTapSignerPsbtUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    signers = _singleSigners,
                    psbt = transaction.psbt,
                    subAmount = transaction.subAmount.value.toString(),
                    feeRate = transaction.feeRate.value.toString(),
                    fee = transaction.fee.value.toString(),
                    subtractFeeFromAmount = transaction.subtractFeeFromAmount
                )
            )

            _loadingType.update { null }

            if (result.isSuccess) {
                val signedTransaction = result.getOrThrow()
                _state.update { currentState ->
                    currentState.copy(
                        transaction = signedTransaction
                    )
                }
                checkAndClaimIfAllSigned(signedTransaction)
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to sign PSBT with tap signer")
            }
        }
    }

    private fun checkAndClaimIfAllSigned(transaction: Transaction) {
        val signedCount = transaction.signers.values.count { it }
        if (signedCount == args.masterSignerIds.size) {
            viewModelScope.launch {
                _loadingType.update { LoadingType.Normal }
                inheritanceClaimingClaimUseCase(
                    InheritanceClaimingClaimUseCase.Param(
                        magic = args.magic,
                        psbt = transaction.psbt,
                    )
                ).onSuccess { transactionAdditional ->
                    _state.update { currentState ->
                        currentState.copy(
                            transaction = currentState.transaction.copy(
                                status = transactionAdditional.status
                            )
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception, "Failed to claim inheritance transaction")
                }
                _loadingType.update { null }
            }
        }
    }

    fun updateTransactionFromImport(transaction: Transaction) {
        _state.update { it.copy(transaction = transaction) }
        checkAndClaimIfAllSigned(transaction)
    }

    fun saveLocalFile(psbt: String) {
        viewModelScope.launch {
            _loadingType.update { LoadingType.Normal }
            val result = saveLocalFileUseCase(
                SaveLocalFileUseCase.Params(
                    fileName = "transaction.psbt",
                    fileContent = psbt
                )
            )
            _loadingType.update { null }
            _event.emit(ClaimTransactionEvent.SaveLocalFile(result.isSuccess))
        }
    }

    fun exportTransactionToFile(psbt: String) {
        viewModelScope.launch {
            _loadingType.update { LoadingType.Normal }
            when (val result = createShareFileUseCase.execute("transaction.psbt")) {
                is Result.Success -> writePsbtToFile(result.data, psbt)
                is Result.Error -> {
                    _event.emit(ClaimTransactionEvent.ShowError(result.exception.messageOrUnknownError()))
                    _loadingType.update { null }
                }
            }
        }
    }

    private fun writePsbtToFile(filePath: String, psbt: String) {
        viewModelScope.launch {
            val result = runCatching {
                FileOutputStream(filePath).use {
                    it.write(psbt.toByteArray(Charsets.UTF_8))
                }
            }
            _loadingType.update { null }
            if (result.isSuccess) {
                _event.emit(ClaimTransactionEvent.ExportToFileSuccess(filePath))
            } else {
                _event.emit(ClaimTransactionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    enum class LoadingType {
        Normal, Nfc
    }

    @AssistedFactory
    interface Factory {
        fun create(
            args: ClaimTransactionArgs
        ): ClaimTransactionViewModel
    }
}

sealed class ClaimTransactionEvent {
    data class SaveLocalFile(val isSuccess: Boolean) : ClaimTransactionEvent()
    data class ExportToFileSuccess(val filePath: String) : ClaimTransactionEvent()
    data class ShowError(val message: String) : ClaimTransactionEvent()
}
