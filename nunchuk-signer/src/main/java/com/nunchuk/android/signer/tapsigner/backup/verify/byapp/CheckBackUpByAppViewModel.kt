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
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.verify.TapSignerRecoveredKeyResolver
import com.nunchuk.android.signer.tapsigner.backup.verify.TapSignerVerifyAccount
import com.nunchuk.android.model.inheritance.RecoveredKey
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
    private val tapSignerRecoveredKeyResolver: TapSignerRecoveredKeyResolver,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    private val getDownloadBackUpKeyUseCase: GetDownloadBackUpKeyUseCase,
    private val getDownloadBackUpKeyReplacementUseCase: GetDownloadBackUpKeyReplacementUseCase,
    private val savedStateHandle: SavedStateHandle,
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

    fun onContinueClicked(
        groupId: String,
        masterSignerId: String,
        isOnChainBackUp: Boolean,
        verificationMethod: ClaimOption? = null,
    ) {
        viewModelScope.launch {
            val newFile = try {
                downloadBackupKeyIfNeeded(
                    isReplaceKey = false,
                    masterSignerId = masterSignerId,
                    groupId = groupId,
                    walletId = ""
                )
            } catch (e: Exception) {
                _event.emit(CheckBackUpByAppEvent.ShowError(e))
                return@launch
            }
            val result =
                verifyTapSignerBackupUseCase(
                    VerifyTapSignerBackupUseCase.Data(
                        masterSignerId = masterSignerId,
                        backUpKey = newFile.path,
                        decryptionKey = decryptionKey
                    )
                )
            if (result.isSuccess && result.getOrThrow()) {
                if (!isOnChainBackUp) {
                    submitVerified(groupId, masterSignerId, verificationMethod, recoveredKey = null)
                    return@launch
                }
                // A configurable on-chain draft verifies against the account (NUN-10320): never
                // send it without one — tap the card for an uncached account, or stop with an error.
                when (val account = tapSignerRecoveredKeyResolver(groupId, masterSignerId)) {
                    TapSignerVerifyAccount.NotRequired ->
                        submitVerified(groupId, masterSignerId, verificationMethod, recoveredKey = null)

                    is TapSignerVerifyAccount.Resolved ->
                        submitVerified(groupId, masterSignerId, verificationMethod, account.key)

                    is TapSignerVerifyAccount.NeedsTap -> {
                        savedStateHandle[KEY_PENDING_GROUP_ID] = groupId
                        savedStateHandle[KEY_PENDING_MASTER_SIGNER_ID] = masterSignerId
                        savedStateHandle[KEY_PENDING_PATH] = account.derivationPath
                        savedStateHandle[KEY_PENDING_METHOD] = verificationMethod?.name
                        _event.emit(CheckBackUpByAppEvent.ReadAccountFromCard)
                    }

                    is TapSignerVerifyAccount.Failed -> _event.emit(CheckBackUpByAppEvent.ShowError(account.error))
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

    /**
     * The card was tapped after [CheckBackUpByAppEvent.ReadAccountFromCard]: read the draft's
     * account off it, then mark the backup verified with it. A failed read keeps the request, so
     * the user can tap again.
     */
    fun readAccountWithTap(isoDep: IsoDep, cvc: String) {
        val groupId = savedStateHandle.get<String>(KEY_PENDING_GROUP_ID) ?: return
        val masterSignerId = savedStateHandle.get<String>(KEY_PENDING_MASTER_SIGNER_ID) ?: return
        val path = savedStateHandle.get<String>(KEY_PENDING_PATH) ?: return
        val method = savedStateHandle.get<String>(KEY_PENDING_METHOD)?.let { name ->
            ClaimOption.entries.firstOrNull { it.name == name }
        }
        viewModelScope.launch {
            _event.emit(CheckBackUpByAppEvent.NfcLoading(true))
            val account = tapSignerRecoveredKeyResolver.readWithTap(isoDep, cvc, masterSignerId, path)
            _event.emit(CheckBackUpByAppEvent.NfcLoading(false))
            when (account) {
                is TapSignerVerifyAccount.Resolved -> {
                    listOf(KEY_PENDING_GROUP_ID, KEY_PENDING_MASTER_SIGNER_ID, KEY_PENDING_PATH, KEY_PENDING_METHOD)
                        .forEach { savedStateHandle.remove<String>(it) }
                    submitVerified(groupId, masterSignerId, method, account.key)
                }

                is TapSignerVerifyAccount.Failed -> _event.emit(CheckBackUpByAppEvent.ShowError(account.error))
                else -> Unit
            }
        }
    }

    private suspend fun submitVerified(
        groupId: String,
        masterSignerId: String,
        verificationMethod: ClaimOption?,
        recoveredKey: RecoveredKey?,
    ) {
        setKeyVerifiedUseCase(
            SetKeyVerifiedUseCase.Param(
                groupId = groupId,
                masterSignerId = masterSignerId,
                verifyType = VerifyType.APP_VERIFIED,
                verificationMethod = verificationMethod,
                recoveredKey = recoveredKey,
            )
        ).onSuccess {
            _event.emit(CheckBackUpByAppEvent.OnVerifyBackUpKeySuccess)
        }.onFailure {
            _event.emit(CheckBackUpByAppEvent.ShowError(it))
        }
    }

    fun onReplaceKeyVerified(
        masterSignerId: String,
        keyId: String,
        groupId: String,
        walletId: String,
        isOnChainBackUp: Boolean,
        verificationMethod: ClaimOption? = null,
    ) {
        viewModelScope.launch {
            val newFile = try {
                downloadBackupKeyIfNeeded(
                    isReplaceKey = true,
                    groupId = groupId,
                    walletId = walletId,
                    masterSignerId = masterSignerId
                )
            } catch (e: Exception) {
                _event.emit(CheckBackUpByAppEvent.ShowError(e))
                return@launch
            }
            val checkSum = if (isOnChainBackUp) "" else getChecksum(newFile)
            val result =
                verifyTapSignerBackupUseCase(
                    VerifyTapSignerBackupUseCase.Data(
                        masterSignerId = masterSignerId,
                        backUpKey = newFile.path,
                        decryptionKey = decryptionKey
                    )
                )
            if (result.isSuccess && result.getOrThrow()) {
                // A configurable wallet verifies against the replacement's account (NUN-10320).
                val recoveredKey = if (isOnChainBackUp) {
                    tapSignerRecoveredKeyResolver.replacementKey(groupId, walletId, masterSignerId).getOrElse {
                        _event.emit(CheckBackUpByAppEvent.ShowError(it))
                        return@launch
                    }
                } else {
                    null
                }
                val apiResult =
                    setReplaceKeyVerifiedUseCase(
                        SetReplaceKeyVerifiedUseCase.Param(
                            keyId = keyId,
                            checkSum = checkSum,
                            verifyType = VerifyType.APP_VERIFIED,
                            groupId = groupId,
                            walletId = walletId,
                            verificationMethod = verificationMethod,
                            recoveredKey = recoveredKey,
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
        private const val KEY_PENDING_GROUP_ID = "pending_account_group_id"
        private const val KEY_PENDING_MASTER_SIGNER_ID = "pending_account_master_signer_id"
        private const val KEY_PENDING_PATH = "pending_account_path"
        private const val KEY_PENDING_METHOD = "pending_account_method"
    }
}

sealed class CheckBackUpByAppEvent {
    object OnVerifyBackUpKeySuccess : CheckBackUpByAppEvent()
    object OnVerifyFailedTooMuch : CheckBackUpByAppEvent()
    data class NfcLoading(val isLoading: Boolean) : CheckBackUpByAppEvent()
    data class GetTapSignerBackupKeyEvent(val filePath: String) : CheckBackUpByAppEvent()
    data class ShowError(val e: Throwable?) : CheckBackUpByAppEvent()

    /** The draft's account of this key is not cached: ask for a tap to read it. */
    data object ReadAccountFromCard : CheckBackUpByAppEvent()
}