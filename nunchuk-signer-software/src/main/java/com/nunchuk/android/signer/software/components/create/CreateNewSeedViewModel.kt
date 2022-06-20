package com.nunchuk.android.signer.software.components.create

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.signer.software.components.create.CreateNewSeedEvent.OpenSelectPhraseEvent
import com.nunchuk.android.usecase.GenerateMnemonicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CreateNewSeedViewModel @Inject constructor(
    private val generateMnemonicUseCase: GenerateMnemonicUseCase
) : NunchukViewModel<CreateNewSeedState, CreateNewSeedEvent>() {

    private var mnemonic: String = ""

    override val initialState = CreateNewSeedState()

    fun init() {
        viewModelScope.launch {
            when (val result = generateMnemonicUseCase.execute()) {
                is Success -> {
                    mnemonic = result.data
                    updateState { copy(seeds = mnemonic.toPhrases()) }
                }
                is Error -> event(GenerateMnemonicCodeErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }

    fun handleContinueEvent() {
        event(OpenSelectPhraseEvent(mnemonic))
    }
}

private fun Int.toCountable() = (this + 1).let {
    if (it < 10) "0$it" else "$it"
}

internal fun String.toPhrases() = this.split(PHRASE_SEPARATOR).mapIndexed { index, s -> "${index.toCountable()}. $s" }

internal const val PHRASE_SEPARATOR = " "
