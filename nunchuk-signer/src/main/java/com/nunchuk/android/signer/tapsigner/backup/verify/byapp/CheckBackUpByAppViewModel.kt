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

package com.nunchuk.android.signer.tapsigner.backup.verify.byapp

import android.app.Application
import android.nfc.tech.IsoDep
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetTapSignerBackupUseCase
import com.nunchuk.android.core.domain.VerifyTapSignerBackupUseCase
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.signer.R
import com.nunchuk.android.usecase.GetDownloadBackUpKeyReplacementUseCase
import com.nunchuk.android.usecase.GetDownloadBackUpKeyUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.utils.ChecksumUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CheckBackUpByAppViewModel @Inject constructor(
    private val verifyTapSignerBackupUseCase: VerifyTapSignerBackupUseCase,
    private val getTapSignerBackupUseCase: GetTapSignerBackupUseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    private val getDownloadBackUpKeyUseCase: GetDownloadBackUpKeyUseCase,
    private val getDownloadBackUpKeyReplacementUseCase: GetDownloadBackUpKeyReplacementUseCase,
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val args: CheckBackUpByAppFragmentArgs =
        CheckBackUpByAppFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CheckBackUpByAppEvent>()
    val event = _event.asSharedFlow()

    var decryptionKey by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf("")
        private set

    private var tryCount = 0

    fun onContinueClicked(groupId: String, masterSignerId: String, isOnChainBackUp: Boolean) {
        viewModelScope.launch {
            val newFile = downloadBackupKeyIfNeeded(
                isReplaceKey = false,
                masterSignerId = masterSignerId,
                groupId = groupId,
                walletId = ""
            )
            val result =
                verifyTapSignerBackupUseCase(
                    VerifyTapSignerBackupUseCase.Data(
                        masterSignerId = masterSignerId,
                        backUpKey = newFile.path,
                        decryptionKey = decryptionKey
                    )
                )
            if (result.isSuccess && result.getOrThrow()) {
                val apiResult =
                    setKeyVerifiedUseCase(
                        SetKeyVerifiedUseCase.Param(
                            groupId,
                            masterSignerId,
                            isAppVerified = if (isOnChainBackUp) true else true
                        )
                    )
                if (apiResult.isSuccess) {
                    _event.emit(CheckBackUpByAppEvent.OnVerifyBackUpKeySuccess)
                } else {
                    _event.emit(CheckBackUpByAppEvent.ShowError(apiResult.exceptionOrNull()))
                }
            } else {
                tryCount++
                errorMessage = application.getString(R.string.nc_decryption_failed_try_again)
                if (tryCount.mod(MAX_TRY) == 0) {
                    _event.emit(CheckBackUpByAppEvent.OnVerifyFailedTooMuch)
                }
            }
        }
    }

    fun onReplaceKeyVerified(
        masterSignerId: String,
        keyId: String,
        groupId: String,
        walletId: String,
        isOnChainBackUp: Boolean
    ) {
        viewModelScope.launch {
            val newFile = downloadBackupKeyIfNeeded(
                isReplaceKey = true,
                groupId = groupId,
                walletId = walletId,
                masterSignerId = masterSignerId
            )
            val checkSum = getChecksum(newFile)
            val result =
                verifyTapSignerBackupUseCase(
                    VerifyTapSignerBackupUseCase.Data(
                        masterSignerId = masterSignerId,
                        backUpKey = newFile.path,
                        decryptionKey = decryptionKey
                    )
                )
            if (result.isSuccess && result.getOrThrow()) {
                val apiResult =
                    setReplaceKeyVerifiedUseCase(
                        SetReplaceKeyVerifiedUseCase.Param(
                            keyId = keyId,
                            checkSum = checkSum,
                            isAppVerified = if (isOnChainBackUp) true else true,
                            groupId = groupId,
                            walletId = walletId
                        )
                    )
                if (apiResult.isSuccess) {
                    _event.emit(CheckBackUpByAppEvent.OnVerifyBackUpKeySuccess)
                } else {
                    _event.emit(CheckBackUpByAppEvent.ShowError(apiResult.exceptionOrNull()))
                }
            } else {
                tryCount++
                errorMessage = application.getString(R.string.nc_decryption_failed_try_again)
                if (tryCount.mod(MAX_TRY) == 0) {
                    _event.emit(CheckBackUpByAppEvent.OnVerifyFailedTooMuch)
                }
            }
        }
    }

    fun getTapSignerBackup(isoDep: IsoDep, masterSignerId: String, cvc: String) {
        viewModelScope.launch {
            _event.emit(CheckBackUpByAppEvent.NfcLoading(true))
            val result = getTapSignerBackupUseCase(
                GetTapSignerBackupUseCase.Data(
                    isoDep,
                    cvc,
                    masterSignerId
                )
            )
            _event.emit(CheckBackUpByAppEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(CheckBackUpByAppEvent.GetTapSignerBackupKeyEvent(result.getOrThrow()))
            } else {
                _event.emit(CheckBackUpByAppEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    private suspend fun getChecksum(file: File): String = withContext(ioDispatcher) {
        ChecksumUtil.getChecksum(file.readBytes())
    }

    private suspend fun downloadBackupKeyIfNeeded(isReplaceKey: Boolean, masterSignerId: String, groupId: String, walletId: String): File {
        return withContext(ioDispatcher) {
            runCatching {
                if (File(args.filePath).exists().not()) {
                    throw Exception("File not found")
                }
            }.let {
                if (it.isSuccess) {
                    Timber.d("Using existing file: ${args.filePath}")
                    File(args.filePath)
                } else {
                    Timber.d("File not found, downloading backup key")
                    File(
                        if (isReplaceKey.not()) {
                            getDownloadBackUpKeyUseCase(
                                GetDownloadBackUpKeyUseCase.Param(
                                    xfp = masterSignerId,
                                    groupId = groupId
                                )
                            ).getOrThrow()
                        } else {
                            getDownloadBackUpKeyReplacementUseCase(
                                GetDownloadBackUpKeyReplacementUseCase.Param(
                                    xfp = masterSignerId,
                                    groupId = groupId,
                                    walletId = walletId
                                )
                            ).getOrThrow()
                        }
                    )
                }
            }
        }
    }

    fun onDecryptionKeyChange(value: String) {
        decryptionKey = value
    }

    companion object {
        private const val MAX_TRY = 5
    }
}

sealed class CheckBackUpByAppEvent {
    object OnVerifyBackUpKeySuccess : CheckBackUpByAppEvent()
    object OnVerifyFailedTooMuch : CheckBackUpByAppEvent()
    data class NfcLoading(val isLoading: Boolean) : CheckBackUpByAppEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : CheckBackUpByAppEvent()
    data class ShowError(val e: Throwable?) : CheckBackUpByAppEvent()
}