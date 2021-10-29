package com.nunchuk.android.signer.software.components.passphrase

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.*
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SetPassphraseViewModel @Inject constructor(
    private val createSoftwareSignerUseCase: CreateSoftwareSignerUseCase
) : NunchukViewModel<SetPassphraseState, SetPassphraseEvent>() {

    private lateinit var mnemonic: String

    private lateinit var signerName: String

    override val initialState = SetPassphraseState()

    fun init(mnemonic: String, signerName: String) {
        this.mnemonic = mnemonic
        this.signerName = signerName
    }

    fun updatePassphrase(passphrase: String) {
        updateState { copy(passphrase = passphrase) }
    }

    fun updateConfirmPassphrase(confirmPassphrase: String) {
        updateState { copy(confirmPassphrase = confirmPassphrase) }
    }

    fun skipPassphraseEvent() {
        createSoftwareSigner(true)
    }

    fun confirmPassphraseEvent() {
        val state = getState()
        val passphrase = state.passphrase
        val confirmPassphrase = state.confirmPassphrase
        when {
            passphrase.isEmpty() -> event(PassPhraseRequiredEvent)
            confirmPassphrase.isEmpty() -> event(ConfirmPassPhraseRequiredEvent)
            passphrase != confirmPassphrase -> event(ConfirmPassPhraseNotMatchedEvent)
            else -> {
                event(PassPhraseValidEvent)
                createSoftwareSigner(false)
            }
        }
    }

    private fun createSoftwareSigner(skipPassphrase: Boolean) {
        viewModelScope.launch {
            createSoftwareSignerUseCase.execute(
                name = signerName,
                mnemonic = mnemonic,
                passphrase = getState().passphrase
            )
                .flowOn(Dispatchers.IO)
                .onStart { event(LoadingEvent(true)) }
                .onException { event(CreateSoftwareSignerErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    event(
                        CreateSoftwareSignerCompletedEvent(id = it.id, name = it.name, skipPassphrase = skipPassphrase)
                    )
                }
        }

    }

}