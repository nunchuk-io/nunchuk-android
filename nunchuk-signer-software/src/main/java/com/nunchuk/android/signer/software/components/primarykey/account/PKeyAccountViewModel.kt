package com.nunchuk.android.signer.software.components.primarykey.account

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PKeyAccountViewModel @Inject constructor(
    private val getPrimaryKeyListUseCase: GetPrimaryKeyListUseCase,
) : NunchukViewModel<PKeyAccountState, PKeyAccountEvent>() {

    override val initialState = PKeyAccountState()

    init {
        getAccounts()
    }

    private fun getAccounts() = viewModelScope.launch {
        setEvent(PKeyAccountEvent.LoadingEvent(true))
        val result = getPrimaryKeyListUseCase(Unit)
        setEvent(PKeyAccountEvent.LoadingEvent(false))
        if (result.isSuccess) {
            updateState { copy(primaryKeys = result.getOrThrow()) }
        } else {
            setEvent(PKeyAccountEvent.ProcessErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }
}
