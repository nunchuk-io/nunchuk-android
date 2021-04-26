package com.nunchuk.android.signer.ss.create

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.signer.ss.create.CreateNewSeedEvent.GenerateMnemonicCodeErrorEvent
import com.nunchuk.android.usecase.GetMnemonicCodeUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CreateNewSeedViewModel @Inject constructor(
    private val getMnemonicCodeUseCase: GetMnemonicCodeUseCase
) : NunchukViewModel<CreateNewSeedState, CreateNewSeedEvent>() {

    override val initialState = CreateNewSeedState()

    fun init() {
        viewModelScope.launch {
            when (val result = getMnemonicCodeUseCase.execute()) {
                is Success -> updateState { copy(seeds = result.data.toPhrases()) }
                is Error -> event(GenerateMnemonicCodeErrorEvent(result.exception.message.orUnknownError()))
            }
        }
    }
}

private fun String.toPhrases() = this.split(" ").mapIndexed { index, s -> "${index.toCountable()}. $s" }

private fun Int.toCountable() = (this + 1).let {
    if (it < 10) "0$it" else "$it"
}
