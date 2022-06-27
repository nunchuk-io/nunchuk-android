package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.settings.AccountSettingEvent.*
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AccountSettingViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : NunchukViewModel<Unit, AccountSettingEvent>() {

    override val initialState = Unit

    fun sendRequestDeleteAccount() {
        viewModelScope.launch {
            repository.requestDeleteAccount()
                .onStart { event(Loading) }
                .flowOn(Dispatchers.IO)
                .onException { event(RequestDeleteError(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { event(RequestDeleteSuccess) }
        }
    }

}
