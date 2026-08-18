/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.components.imports

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.constants.NativeErrorCode
import com.nunchuk.android.core.domain.ParseQRCodeFromPhotoUseCase
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.share.model.SignFlowType
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.membership.ParseKeystoneDummyTransaction
import com.nunchuk.android.usecase.membership.ParseKeystoneDummyTransactionSignIn
import com.nunchuk.android.usecase.qr.AnalyzeQrUseCase
import com.nunchuk.android.usecase.signer.ExtractColdcardMessageSignatureFromQrUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ImportTransactionViewModel @Inject constructor(
    private val importKeystoneTransactionUseCase: ImportKeystoneTransactionUseCase,
    private val parseKeystoneDummyTransaction: ParseKeystoneDummyTransaction,
    private val parseKeystoneDummyTransactionSignIn: ParseKeystoneDummyTransactionSignIn,
    private val analyzeQrUseCase: AnalyzeQrUseCase,
    private val parseQRCodeFromPhotoUseCase: ParseQRCodeFromPhotoUseCase,
    private val extractColdcardMessageSignatureFromQrUseCase: ExtractColdcardMessageSignatureFromQrUseCase
) : NunchukViewModel<Unit, ImportTransactionEvent>() {
    private val _state = MutableStateFlow(ImportTransactionState())
    val uiState = _state.asStateFlow()

    private lateinit var args: ImportTransactionArgs
    private val importMutex = Mutex()

    private val qrDataList = HashSet<String>()

    override val initialState = Unit

    fun init(args: ImportTransactionArgs) {
        this.args = args
    }

    fun importTransactionViaQR(qrData: String) {
        if (!qrDataList.contains(qrData)) {
            qrDataList.add(qrData)
            viewModelScope.launch {
                importMutex.withLock {
                    analyzeQr()
                    if (isDummyFlow) {
                        parseDummyTransaction()
                    } else {
                        parseNormalTransaction()
                    }
                }
            }
        }
    }

    fun decodeQRCodeFromUri(uri: Uri) {
        viewModelScope.launch {
            parseQRCodeFromPhotoUseCase(uri).onSuccess {
                importTransactionViaQR(it)
            }.onFailure {
                setEvent(ImportTransactionEvent.ImportTransactionError(it.message.orUnknownError()))
            }
        }
    }

    private suspend fun analyzeQr() {
        val result = analyzeQrUseCase(qrDataList.toList())
        if (result.isSuccess) {
            Timber.d("analyzeQrUseCase: ${result.getOrThrow()}")
            _state.update { it.copy(progress = result.getOrThrow().times(100.0)) }
        }
    }

    private suspend fun parseDummyTransaction() {
        when (args.signFlowType) {
            is SignFlowType.ClaimDummy -> {
                extractColdcardMessageSignatureFromQrUseCase(
                    qrDataList.toList()
                ).onSuccess {
                    setEvent(ImportTransactionSuccess(signature = it))
                }
            }

            else -> {
                parseDummyTransactionPsbt().onSuccess {
                    setEvent(ImportTransactionSuccess(it))
                }
            }
        }.onFailure { e ->
            val errorCode = e.nativeErrorCode()
            if (errorCode == NativeErrorCode.JADE_QR_PIN_UNLOCK) {
                setEvent(ImportTransactionEvent.ImportTransactionError(e.message.orUnknownError(), errorCode))
            } else if (_state.value.progress >= 100) {
                setEvent(ImportTransactionEvent.ImportTransactionError("Invalid or unreadable QR code. Please try again."))
            }
        }
    }

    /**
     * A dummy tx has to be decoded against the wallet it belongs to whenever we have one: signers
     * that strip the redundant PSBT metadata (SeedSigner, Krux) leave nothing to attribute the
     * partial signature to, so decoding without the wallet reports no signature at all ("No new
     * signatures detected"). The wallet-less decode stays for the flows that genuinely have no
     * local wallet — sign-in via digital signature and inheritance claim — and as a fallback when
     * the wallet cannot be loaded.
     */
    private suspend fun parseDummyTransactionPsbt(): Result<Transaction> {
        val qrs = qrDataList.toList()
        if (args.signFlowType !is SignFlowType.SignInDummy && args.walletId.isNotEmpty()) {
            val result = parseKeystoneDummyTransaction(
                ParseKeystoneDummyTransaction.Param(args.walletId, qrs)
            )
            if (result.isSuccess) return result
            Timber.e(
                result.exceptionOrNull(),
                "Decode dummy tx with wallet ${args.walletId} failed, fallback to wallet-less decode"
            )
        }
        return parseKeystoneDummyTransactionSignIn(
            ParseKeystoneDummyTransactionSignIn.Param(qrs)
        )
    }

    private suspend fun parseNormalTransaction() {
        Timber.d("[ImportTransaction]execute($args.walletId, $qrDataList)")
        importKeystoneTransactionUseCase.execute(
            walletId = args.walletId,
            qrData = qrDataList.toList(),
            initEventId = args.initEventId,
            masterFingerPrint = args.masterFingerPrint
        )
            .flowOn(IO)
            .onException {
                // A locked Jade is only reported once a full payload decodes, so it does not
                // have to wait for the progress gate the partial-fragment failures need.
                val errorCode = it.nativeErrorCode()
                if (errorCode == NativeErrorCode.JADE_QR_PIN_UNLOCK || _state.value.progress >= 100) {
                    setEvent(ImportTransactionEvent.ImportTransactionError(it.message.orUnknownError(), errorCode))
                }
            }
            .flowOn(Main)
            .collect { event(ImportTransactionSuccess()) }
    }

    private val isDummyFlow: Boolean
        get() = args.signFlowType is SignFlowType.NormalDummy || 
                args.signFlowType is SignFlowType.SignInDummy || 
                args.signFlowType is SignFlowType.ClaimDummy
}

data class ImportTransactionState(val progress: Double = 0.0)