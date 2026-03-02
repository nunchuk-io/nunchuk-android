package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.nfc.NdefRecord
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.SignTapSignerPsbtUseCase
import com.nunchuk.android.core.domain.coldcard.ExportRawPsbtToMk4UseCase
import com.nunchuk.android.core.mapper.SingleSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.isNoInternetException
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
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
import com.nunchuk.android.usecase.ValueFromAmountUseCase
import com.nunchuk.android.usecase.membership.InheritanceClaimingClaimUseCase
import com.nunchuk.android.usecase.signer.GetPsbtFromMk4UseCase
import com.nunchuk.android.usecase.signer.GetRemoteOrMasterSignerUseCase
import com.nunchuk.android.usecase.signer.SignSoftwarePsbtUseCase
import com.nunchuk.android.usecase.transaction.DecodeTxUseCase
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
    private val exportRawPsbtToMk4UseCase: ExportRawPsbtToMk4UseCase,
    private val getPsbtFromMk4UseCase: GetPsbtFromMk4UseCase,
    private val decodeTxUseCase: DecodeTxUseCase,
    private val valueFromAmountUseCase: ValueFromAmountUseCase,
    private val savedStateHandle: SavedStateHandle,
    @Assisted private val args: ClaimTransactionArgs
) : ViewModel() {

    private val initialTransaction =
        savedStateHandle.get<Transaction>(KEY_SAVED_TRANSACTION) ?: args.transaction
    private val _state = MutableStateFlow(TransactionDetailsState(transaction = initialTransaction))
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

    private val _claimError = MutableStateFlow<String?>(null)
    val claimError: StateFlow<String?> = _claimError.asStateFlow()

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
                        transaction = currentState.transaction,
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
            val transaction = _state.value.transaction

            val result = signSoftwarePsbtUseCase(
                SignSoftwarePsbtUseCase.Param(
                    masterSignerId = singleSigner.masterSignerId,
                    signers = _singleSigners,
                    psbt = transaction.psbt,
                    subAmount = valueFromAmountUseCase(transaction.subAmount).getOrThrow(),
                    feeRate = valueFromAmountUseCase(transaction.feeRate).getOrThrow(),
                    fee = valueFromAmountUseCase(transaction.fee).getOrThrow(),
                    subtractFeeFromAmount = transaction.subtractFeeFromAmount
                )
            )

            _loadingType.update { null }

            if (result.isSuccess) {
                val signedTransaction = result.getOrThrow()
                updateTransaction(signedTransaction)
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to sign PSBT with software signer")
            }
        }
    }

    fun signTapSignerPsbt(isoDep: IsoDep, cvc: String) {
        viewModelScope.launch {
            _loadingType.update { LoadingType.Nfc }
            val transaction = _state.value.transaction

            val result = signTapSignerPsbtUseCase(
                SignTapSignerPsbtUseCase.Data(
                    isoDep = isoDep,
                    cvc = cvc,
                    signers = _singleSigners,
                    psbt = transaction.psbt,
                    subAmount = valueFromAmountUseCase(transaction.subAmount).getOrThrow(),
                    feeRate = valueFromAmountUseCase(transaction.feeRate).getOrThrow(),
                    fee = valueFromAmountUseCase(transaction.fee).getOrThrow(),
                    subtractFeeFromAmount = transaction.subtractFeeFromAmount
                )
            )

            _loadingType.update { null }

            if (result.isSuccess) {
                val signedTransaction = result.getOrThrow()
                updateTransaction(signedTransaction)
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to sign PSBT with tap signer")
            }
        }
    }

    fun checkAndClaimIfAllSigned(transaction: Transaction) {
        val signedCount = transaction.signers.values.count { it }
        if (signedCount == args.masterSignerIds.size) {
            performClaim(transaction)
        }
    }

    private fun performClaim(transaction: Transaction) {
        viewModelScope.launch {
            _claimError.update { null }
            _loadingType.update { LoadingType.Normal }
            inheritanceClaimingClaimUseCase(
                InheritanceClaimingClaimUseCase.Param(
                    magic = args.magic,
                    psbt = transaction.psbt,
                )
            ).onSuccess { transactionAdditional ->
                _claimError.update { null }
                _state.update { currentState ->
                    currentState.copy(
                        transaction = currentState.transaction.copy(
                            status = transactionAdditional.status
                        )
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to claim inheritance transaction")
                val errorMessage = if (exception.isNoInternetException) {
                    "Network unreachable. Please try again later."
                } else {
                    exception.readableMessage()
                }
                _claimError.update { errorMessage }
            }
            _loadingType.update { null }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        savedStateHandle[KEY_SAVED_TRANSACTION] = transaction
        _state.update { it.copy(transaction = transaction.copy(changeIndex = args.transaction.changeIndex)) }
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

    fun handleExportTransactionToMk4(ndef: Ndef) {
        viewModelScope.launch {
            val psbt = _state.value.transaction.psbt
            if (psbt.isEmpty()) return@launch
            _loadingType.update { LoadingType.ColdCard }
            val result = exportRawPsbtToMk4UseCase(ExportRawPsbtToMk4UseCase.Data(psbt, ndef))
            _loadingType.update { null }
            if (result.isSuccess) {
                _event.emit(ClaimTransactionEvent.ExportTransactionToMk4Success)
            } else {
                _event.emit(ClaimTransactionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleImportTransactionFromMk4(records: List<NdefRecord>) {
        viewModelScope.launch {
            _loadingType.update { LoadingType.ColdCard }
            getPsbtFromMk4UseCase(records.toTypedArray()).onSuccess { importedPsbt ->
                val currentTx = _state.value.transaction
                decodeTxUseCase(
                    DecodeTxUseCase.Param(
                        signers = _singleSigners,
                        psbt = importedPsbt,
                        subAmount = valueFromAmountUseCase(currentTx.subAmount).getOrThrow(),
                        feeRate = valueFromAmountUseCase(currentTx.feeRate).getOrThrow(),
                        fee = valueFromAmountUseCase(currentTx.fee).getOrThrow(),
                        subtractFeeFromAmount = currentTx.subtractFeeFromAmount
                    )
                ).onSuccess { transaction ->
                    updateTransaction(transaction)
                    _event.emit(ClaimTransactionEvent.ImportTransactionFromMk4Success)
                }.onFailure {
                    _event.emit(ClaimTransactionEvent.ShowError(it.message.orUnknownError()))
                }
            }.onFailure {
                _event.emit(ClaimTransactionEvent.ShowError(it.message.orUnknownError()))
            }
            _loadingType.update { null }
        }
    }

    enum class LoadingType {
        Normal, Nfc, ColdCard
    }

    companion object {
        private const val KEY_SAVED_TRANSACTION = "saved_transaction"
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
    data object ExportTransactionToMk4Success : ClaimTransactionEvent()
    data object ImportTransactionFromMk4Success : ClaimTransactionEvent()
}
