package com.nunchuk.android.signer.software.components.primarykey.passphrase

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.CheckPassphrasePrimaryKeyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class PKeyEnterPassphraseViewModel @AssistedInject constructor(
    @Assisted private val args: PKeyEnterPassphraseArgs,
    private val checkPassphrasePrimaryKeyUseCase: CheckPassphrasePrimaryKeyUseCase
) : NunchukViewModel<PKeyEnterPassphraseState, PKeyEnterPassphraseEvent>() {

    override val initialState = PKeyEnterPassphraseState(args = args)

    fun updatePassphrase(passphrase: String) {
        updateState { copy(passphrase = passphrase) }
    }

    fun checkPassphrase() = viewModelScope.launch {
        setEvent(PKeyEnterPassphraseEvent.LoadingEvent(true))
        val result = checkPassphrasePrimaryKeyUseCase(
            CheckPassphrasePrimaryKeyUseCase.Param(
                mnemonic = args.mnemonic,
                passphrase = getState().passphrase
            )
        )
        setEvent(PKeyEnterPassphraseEvent.LoadingEvent(false))
        if (result.isSuccess) {
            result.getOrThrow()?.let {
                setEvent(PKeyEnterPassphraseEvent.CheckPassphraseSuccess(it))
            }
        } else {
            setEvent(PKeyEnterPassphraseEvent.CheckPassphraseError)
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeyEnterPassphraseArgs): PKeyEnterPassphraseViewModel
    }
}