package com.nunchuk.android.signer.mk4.inheritance.backup.viaapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.VerifyColdCardBackupUseCase
import com.nunchuk.android.core.util.lastWord
import com.nunchuk.android.core.util.replaceLastWord
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.GetBip39WordListUseCase
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import com.nunchuk.android.usecase.membership.SetReplaceKeyVerifiedUseCase
import com.nunchuk.android.utils.ChecksumUtil
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
import javax.inject.Inject

@HiltViewModel
class ColdCardVerifyBackupViaAppViewModel @Inject constructor(
    private val getBip39WordListUseCase: GetBip39WordListUseCase,
    private val verifyColdCardBackupUseCase: VerifyColdCardBackupUseCase,
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase,
    private val setReplaceKeyVerifiedUseCase: SetReplaceKeyVerifiedUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _event = MutableSharedFlow<ColdCardVerifyBackupViaAppEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ColdCardVerifyBackupViaAppState())
    val state = _state.asStateFlow()

    private var bip39Words = ArrayList<String>()

    init {
        viewModelScope.launch {
            val result = getBip39WordListUseCase.execute()
            if (result is Result.Success) {
                bip39Words = ArrayList(result.data)
            }
        }
    }

    var tryCount = 0

    fun onContinueClicked(groupId: String, masterSignerId: String, filePath: String) {
        viewModelScope.launch {
            val result =
                verifyColdCardBackupUseCase(
                    VerifyColdCardBackupUseCase.Data(
                        xfp = masterSignerId,
                        backUpPassword = _state.value.backUpPassword,
                        filePath = filePath
                    )
                )
            if (result.isSuccess) {
                val apiResult =
                    setKeyVerifiedUseCase(
                        SetKeyVerifiedUseCase.Param(
                            groupId,
                            masterSignerId,
                            true
                        )
                    )
                if (apiResult.isSuccess) {
                    _event.emit(ColdCardVerifyBackupViaAppEvent.OnVerifyBackUpKeySuccess)
                } else {
                    _event.emit(ColdCardVerifyBackupViaAppEvent.ShowError(apiResult.exceptionOrNull()))
                }
            } else {
                tryCount++
                if (tryCount.mod(MAX_TRY) == 0) {
                    _event.emit(ColdCardVerifyBackupViaAppEvent.OnVerifyFailedTooMuch)
                }
            }
        }
    }

    fun onReplaceKeyVerified(
        masterSignerId: String, keyId: String, filePath: String, groupId: String,
        walletId: String
    ) {
        _state.update { it.copy(showVerifyError = false) }
        viewModelScope.launch {
            val result =
                verifyColdCardBackupUseCase(
                    VerifyColdCardBackupUseCase.Data(
                        xfp = masterSignerId,
                        backUpPassword = _state.value.backUpPassword,
                        filePath = filePath
                    )
                )
            if (result.isSuccess) {
                val apiResult =
                    setReplaceKeyVerifiedUseCase(
                        SetReplaceKeyVerifiedUseCase.Param(
                            keyId = keyId.ifEmpty { masterSignerId },
                            checkSum = getChecksum(filePath),
                            isAppVerified = true,
                            groupId = groupId,
                            walletId = walletId
                        )
                    )
                if (apiResult.isSuccess) {
                    _event.emit(ColdCardVerifyBackupViaAppEvent.OnVerifyBackUpKeySuccess)
                } else {
                    _event.emit(ColdCardVerifyBackupViaAppEvent.ShowError(apiResult.exceptionOrNull()))
                }
            } else {
                _state.update { it.copy(showVerifyError = true) }
            }
        }
    }

    fun handleInputEvent(mnemonic: String) {
        if (mnemonic != _state.value.backUpPassword) {
            _state.update { it.copy(_backUpPassword = mnemonic) }
            val word = mnemonic.lastWord()
            if (word.isNotEmpty()) {
                filter(word)
            }
        } else {
            _state.update { it.copy(_backUpPassword = mnemonic) }
        }
    }

    private fun filter(word: String) {
        val filteredWords =
            if (word.isNotBlank()) bip39Words.filter { it.startsWith(word) } else emptyList()
        _state.update { it.copy(suggestions = filteredWords) }
    }

    fun handleSelectWord(word: String) {
        _state.update { it.copy(suggestions = bip39Words) }
        val updatedMnemonic = _state.value.backUpPassword.replaceLastWord(word)
        _state.update { it.copy(_backUpPassword = "$updatedMnemonic ") }
    }

    private suspend fun getChecksum(filePath: String): String = withContext(ioDispatcher) {
        ChecksumUtil.getChecksum(File(filePath).readBytes())
    }

    companion object {
        private const val MAX_TRY = 5
    }
}

sealed class ColdCardVerifyBackupViaAppEvent {
    data object OnVerifyBackUpKeySuccess : ColdCardVerifyBackupViaAppEvent()
    data class ShowError(val throwable: Throwable?) : ColdCardVerifyBackupViaAppEvent()
    data object OnVerifyFailedTooMuch : ColdCardVerifyBackupViaAppEvent()
}

data class ColdCardVerifyBackupViaAppState(
    var _backUpPassword: String = "",
    var _backupPasswords: List<String> = arrayListOf("", ""),
    val suggestions: List<String> = emptyList(),
    val showVerifyError: Boolean = false,
) {
    var backUpPassword: String
        get() = _backUpPassword.trim()
        set(value) {
            _backUpPassword = value
        }
}