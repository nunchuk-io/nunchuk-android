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

package com.nunchuk.android.signer.tapsigner.decryption

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportTapSignerUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NfcDecryptionKeyViewModel @Inject constructor(
    private val importTapSignerUseCase: ImportTapSignerUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val pushEventManager: PushEventManager,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val application: Application
) : ViewModel() {
    private val _event = MutableSharedFlow<NfcDecryptionKeyEvent>()
    val event = _event.asSharedFlow()

    fun decryptBackUpKey(backUpFileUri: Uri, decryptionKey: String, newIndex: Int, walletId: String) {
        viewModelScope.launch {
            _event.emit(NfcDecryptionKeyEvent.Loading)
            withContext(ioDispatcher) {
                getFileFromUri(backUpFileUri, application.cacheDir)
            }?.let { file ->
                val result = importTapSignerUseCase(
                    ImportTapSignerUseCase.Data(
                        file.absolutePath,
                        decryptionKey
                    )
                )
                runCatching { file.delete() }
                if (result.isSuccess) {
                    val signer = result.getOrThrow()
                    getSingleSigner(signer, newIndex, walletId)
                    _event.emit(NfcDecryptionKeyEvent.ImportTapSignerSuccess(signer))
                } else {
                    _event.emit(NfcDecryptionKeyEvent.ImportTapSignerFailed(result.exceptionOrNull()))
                }
            }
        }
    }

    // for replace key free wallet
    private suspend fun getSingleSigner(
        signer: MasterSigner,
        newIndex: Int,
        walletId: String
    ) {
        getWalletDetail2UseCase(walletId).onSuccess { wallet ->
            val walletType = if (wallet.signers.size > 1) WalletType.MULTI_SIG else WalletType.SINGLE_SIG
            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Param(
                    xfp = signer.id,
                    walletType = walletType,
                    addressType = AddressType.NATIVE_SEGWIT,
                    index = newIndex
                )
            ).onSuccess { singleSigner ->
                singleSigner?.let { signer ->
                    pushEventManager.push(PushEvent.LocalUserSignerAdded(signer))
                }
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun getFileFromUri(uri: Uri, directory: File) = try {
        val file = File.createTempFile("NCsuffix", ".prefixNC", directory)
        file.outputStream().use {
            application.contentResolver.openInputStream(uri)?.copyTo(it)
        }
        file
    } catch (t: Throwable) {
        CrashlyticsReporter.recordException(t)
        null
    }
}

sealed class NfcDecryptionKeyEvent {
    object Loading : NfcDecryptionKeyEvent()
    class ImportTapSignerSuccess(val masterSigner: MasterSigner) : NfcDecryptionKeyEvent()
    class ImportTapSignerFailed(val e: Throwable?) : NfcDecryptionKeyEvent()
}