package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ImportTapsignerMasterSignerContentUseCase
import com.nunchuk.android.core.domain.membership.InheritanceClaimDownloadBackupUseCase
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.util.countWords
import com.nunchuk.android.core.util.lastWord
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.replaceLastWord
import com.nunchuk.android.main.util.ChecksumUtil
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.GetBip39WordListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceClaimInputViewModel @Inject constructor(
    private val getBip39WordListUseCase: GetBip39WordListUseCase,
    private val inheritanceClaimDownloadBackupUseCase: InheritanceClaimDownloadBackupUseCase,
    private val importTapsignerMasterSignerContentUseCase: ImportTapsignerMasterSignerContentUseCase,
    private val masterSignerMapper: MasterSignerMapper,
) : ViewModel() {

    private val _event = MutableSharedFlow<InheritanceClaimInputEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceClaimInputState())
    val state = _state.asStateFlow()

    private var bip39Words = ArrayList<String>()

    init {
        viewModelScope.launch {
            val result = getBip39WordListUseCase.execute()
            if (result is Result.Success) {
                bip39Words = ArrayList(result.data)
                _state.update { it.copy(suggestions = bip39Words) }
            }
        }
    }

    fun downloadBackupKey() = viewModelScope.launch {
        val stateValue = _state.value
        if (stateValue.magicalPhrase.isBlank() || stateValue.backupPassword.isBlank()) return@launch
        _event.emit(InheritanceClaimInputEvent.Loading(true))
        val result = inheritanceClaimDownloadBackupUseCase(stateValue.magicalPhrase)
        _event.emit(InheritanceClaimInputEvent.Loading(false))
        if (result.isSuccess) {
            val backupKey = result.getOrThrow()
            val backupData = Base64.decode(backupKey.keyBackUpBase64, Base64.DEFAULT)
            if (ChecksumUtil.verifyChecksum(backupData, backupKey.keyCheckSum)) {
                val resultImport = importTapsignerMasterSignerContentUseCase(
                    ImportTapsignerMasterSignerContentUseCase.Param(
                        backupData,
                        stateValue.backupPassword,
                        INHERITED_KEY_NAME
                    )
                )
                if (resultImport.isSuccess) {
                    _event.emit(
                        InheritanceClaimInputEvent.ImportSuccess(
                            masterSignerMapper(resultImport.getOrThrow()),
                            magicalPhrase = stateValue.magicalPhrase
                        )
                    )
                } else {
                    _event.emit(InheritanceClaimInputEvent.Error(resultImport.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        } else {
            val exception = result.exceptionOrNull()
            if (exception is NunchukApiException) {
                if (exception.code == 400) {
                    _event.emit(InheritanceClaimInputEvent.SubscriptionExpired)
                } else if (exception.code == 801) {
                    _event.emit(InheritanceClaimInputEvent.InActivated(result.exceptionOrNull()?.message.orUnknownError()))
                }
            } else {
                _event.emit(InheritanceClaimInputEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun updateBackupPassword(password: String) {
        _state.update { it.copy(backupPassword = password) }
    }

    fun handleInputEvent(mnemonic: String) {
        val withoutSpace = mnemonic.trim()
        if (withoutSpace != _state.value.magicalPhrase) {
            _state.update { it.copy(magicalPhrase = withoutSpace) }
            val word = withoutSpace.lastWord()
            if (word.isNotEmpty()) {
                filter(word)
            }
            val canGoNext =
                withoutSpace.countWords() == ACCEPTED_NUM_WORDS && _state.value.backupPassword.isNotBlank()
            _state.update { it.copy(enableContinue = canGoNext) }
        } else {
            _state.update { it.copy(magicalPhrase = mnemonic) }
        }
    }

    private fun filter(word: String) {
        val filteredWords = bip39Words.filter { it.startsWith(word) }
        _state.update { it.copy(suggestions = filteredWords) }
    }

    fun handleSelectWord(word: String) {
        _state.update { it.copy(suggestions = bip39Words) }
        val updatedMnemonic = _state.value.magicalPhrase.replaceLastWord(word)
        _state.update { it.copy(magicalPhrase = updatedMnemonic) }
        val canGoNext =
            updatedMnemonic.countWords() == ACCEPTED_NUM_WORDS && _state.value.backupPassword.isNotBlank()
        _state.update { it.copy(enableContinue = canGoNext) }
    }

    companion object {
        private const val INHERITED_KEY_NAME = "Inherited key"
        private const val ACCEPTED_NUM_WORDS = 3
    }
}