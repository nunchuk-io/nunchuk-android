/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.signer.nfc.decryption

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportTapSignerUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.utils.CrashlyticsReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class NfcDecryptionKeyViewModel @Inject constructor(
    private val importTapSignerUseCase: ImportTapSignerUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application
) : ViewModel() {
    private val _event = MutableStateFlow<NfcDecryptionKeyEvent?>(null)
    val event = _event.filterIsInstance<NfcDecryptionKeyEvent>()

    fun decryptBackUpKey(backUpFileUri: Uri, decryptionKey: String) {
        _event.value = NfcDecryptionKeyEvent.Loading
        viewModelScope.launch {
            withContext(ioDispatcher) {
                getFileFromUri(backUpFileUri, application.cacheDir)
            }?.let { file ->
                val result = importTapSignerUseCase(ImportTapSignerUseCase.Data(file.absolutePath, decryptionKey))
                if (result.isSuccess) {
                    _event.value = NfcDecryptionKeyEvent.ImportTapSignerSuccess(result.getOrThrow())
                } else {
                    _event.value = NfcDecryptionKeyEvent.ImportTapSignerFailed(result.exceptionOrNull())
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
    class ImportTapSignerSuccess(val masterSigner: MasterSigner): NfcDecryptionKeyEvent()
    class ImportTapSignerFailed(val e: Throwable?): NfcDecryptionKeyEvent()
}