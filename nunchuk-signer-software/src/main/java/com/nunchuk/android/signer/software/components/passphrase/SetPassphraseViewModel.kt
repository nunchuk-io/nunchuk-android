package com.nunchuk.android.signer.software.components.passphrase

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.software.components.passphrase.SetPassphraseEvent.*
import com.nunchuk.android.usecase.CreateSoftwareSignerUseCase
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
            val state = getState()
            when (val result = createSoftwareSignerUseCase.execute(
                name = signerName,
                mnemonic = mnemonic,
                passphrase = state.passphrase
            )) {
                is Success -> event(
                    CreateSoftwareSignerCompletedEvent(
                        id = result.data.id,
                        name = result.data.name,
                        skipPassphrase = skipPassphrase
                    )
                )
                is Error -> event(CreateSoftwareSignerErrorEvent(result.exception.message.orUnknownError()))
            }
        }

    }

}