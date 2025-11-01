package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.InheritanceClaimingInitUseCase
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.util.lastWord
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.replaceLastWord
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.GetBip39WordListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClaimMagicPhraseViewModel @Inject constructor(
    private val getBip39WordListUseCase: GetBip39WordListUseCase,
    private val inheritanceClaimingInitUseCase: InheritanceClaimingInitUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ClaimMagicPhraseState())
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

    fun handleInputEvent(mnemonic: String) {
        if (mnemonic != _state.value.formattedMagicPhrase) {
            _state.update { it.copy(magicalPhrase = mnemonic) }
            val word = mnemonic.lastWord()
            if (word.isNotEmpty()) {
                filter(word)
            }
        } else {
            _state.update { it.copy(magicalPhrase = mnemonic) }
        }
    }

    private fun filter(word: String) {
        val filteredWords =
            if (word.isNotBlank()) bip39Words.filter { it.startsWith(word) } else emptyList()
        _state.update { it.copy(suggestions = filteredWords) }
    }

    fun handleSelectWord(word: String) {
        _state.update { it.copy(suggestions = bip39Words) }
        val updatedMnemonic = _state.value.formattedMagicPhrase.replaceLastWord(word)
        _state.update { it.copy(magicalPhrase = "$updatedMnemonic ") }
    }

    fun validateMagicPhrase(): Boolean {
        val phrase = _state.value.formattedMagicPhrase.trim()
        if (phrase.isEmpty()) {
            return false
        }
        
        val words = phrase.split(" ").filter { it.isNotBlank() }
        return words.all { word ->
            bip39Words.contains(word.lowercase())
        }
    }

    fun initInheritanceClaiming() {
        val phrase = _state.value.formattedMagicPhrase.trim()
        if (!validateMagicPhrase()) {
            _state.update { it.copy(error = "Invalid Magic Phrase. Please try again.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null, initResult = null, dialog = null) }
        viewModelScope.launch {
            inheritanceClaimingInitUseCase(phrase).onSuccess { result ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        initResult = result,
                        error = null,
                        dialog = null
                    )
                }
            }.onFailure { e ->
                val exception = e as? NunchukApiException
                if (exception != null) {
                    val dialog = when (exception.code) {
                        400 -> ClaimMagicPhraseDialog.SubscriptionExpired
                        803 -> ClaimMagicPhraseDialog.InActivated(e.message.orUnknownError())
                        829 -> ClaimMagicPhraseDialog.PleaseComeLater(e.message.orUnknownError())
                        830 -> ClaimMagicPhraseDialog.SecurityDepositRequired(e.message.orUnknownError())
                        else -> null
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = if (dialog == null) e.message.orUnknownError() else null,
                            dialog = dialog
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to initialize inheritance claiming",
                            dialog = null
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearInitResult() {
        _state.update { it.copy(initResult = null) }
    }

    fun clearDialog() {
        _state.update { it.copy(dialog = null) }
    }
}

