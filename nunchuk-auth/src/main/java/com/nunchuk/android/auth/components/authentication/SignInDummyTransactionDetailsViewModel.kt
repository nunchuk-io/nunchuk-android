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

package com.nunchuk.android.auth.components.authentication

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.getFileFromUri
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.SaveLocalFileUseCase
import com.nunchuk.android.usecase.membership.GetSignInDummyTxFromPsbtByteArrayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
internal class DummyTransactionDetailsViewModel @Inject constructor(
    private val getDummyTxFromPsbtByteArrayUseCase: GetSignInDummyTxFromPsbtByteArrayUseCase,
    private val createShareFileUseCase: CreateShareFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val application: Application,
    private val saveLocalFileUseCase: SaveLocalFileUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SignInDummyTransactionState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<DummyTransactionDetailEvent>()
    val event = _event.asSharedFlow()

    fun importTransactionViaFile(uri: Uri) {
        viewModelScope.launch {
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(true))
            val bytes = withContext(ioDispatcher) {
                getFileFromUri(application.contentResolver, uri, application.cacheDir)?.let {file ->
                    File(file.absolutePath).readBytes()
                }
            } ?: return@launch
            val result = getDummyTxFromPsbtByteArrayUseCase(
                GetSignInDummyTxFromPsbtByteArrayUseCase.Param(bytes)
            )
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(false))
            if (result.isSuccess) {
                _event.emit(DummyTransactionDetailEvent.ImportTransactionSuccess(result.getOrThrow()))
            } else {
                _event.emit(DummyTransactionDetailEvent.TransactionError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }


    fun exportTransactionToFile(dataToSign: String) {
        viewModelScope.launch {
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(true))
            when (val result = createShareFileUseCase.execute( "dummy.psbt")) {
                is Result.Success -> exportTransaction(result.data, dataToSign)
                is Result.Error -> _event.emit(DummyTransactionDetailEvent.TransactionError(result.exception.messageOrUnknownError()))
            }
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(false))
        }
    }

    private fun exportTransaction(filePath: String, dataToSign: String) {
        viewModelScope.launch {
                val result = runCatching {
                    withContext(ioDispatcher) {
                        FileOutputStream(filePath).use {
                            it.write(dataToSign.toByteArray(Charsets.UTF_8))
                        }
                    }
                }
                if (result.isSuccess) {
                    _event.emit(DummyTransactionDetailEvent.ExportToFileSuccess(filePath))
                } else {
                    _event.emit(DummyTransactionDetailEvent.TransactionError(result.exceptionOrNull()?.message.orUnknownError()))
                }
        }
    }

    fun saveFileToLocal(dataToSign: String) {
        viewModelScope.launch {
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(true))
            val result = saveLocalFileUseCase(SaveLocalFileUseCase.Params(fileName = "dummy.psbt", fileContent = dataToSign))
            _event.emit(DummyTransactionDetailEvent.LoadingEvent(false))
            _event.emit(DummyTransactionDetailEvent.SaveLocalFile(result.isSuccess))
        }
    }


    fun handleViewMoreEvent() {
        _state.update { it.copy(viewMore = it.viewMore.not()) }
    }
}

sealed class DummyTransactionDetailEvent {
    data class LoadingEvent(val isLoading: Boolean) : DummyTransactionDetailEvent()
    data class ImportTransactionSuccess(val transaction: Transaction?) : DummyTransactionDetailEvent()
    data class ExportToFileSuccess(val filePath: String) : DummyTransactionDetailEvent()
    data class TransactionError(val error: String) : DummyTransactionDetailEvent()
    data class SaveLocalFile(val isSuccess: Boolean) : DummyTransactionDetailEvent()
}