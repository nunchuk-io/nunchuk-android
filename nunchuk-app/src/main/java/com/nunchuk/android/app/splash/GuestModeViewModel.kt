package com.nunchuk.android.app.splash

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class GuestModeViewModel @Inject constructor(
    private val initNunchukUseCase: InitNunchukUseCase,
) : NunchukViewModel<Unit, GuestModeEvent>() {

    override val initialState = Unit

    fun initGuestModeNunchuk() {
        viewModelScope.launch {
            initNunchukUseCase.execute(accountId = "")
                .onStart { event(GuestModeEvent.LoadingEvent(true)) }
                .flowOn(Dispatchers.IO)
                .onException { event(GuestModeEvent.InitErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { event(GuestModeEvent.InitSuccessEvent) }
        }
    }

}