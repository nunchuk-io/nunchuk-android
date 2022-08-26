package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.DeletePrimaryKeyUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.settings.AccountSettingEvent.*
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AccountSettingViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val deletePrimaryKeyUseCase: DeletePrimaryKeyUseCase,
    private val appScope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder
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

    fun deletePrimaryKey(passphrase: String) = viewModelScope.launch {
        setEvent(Loading)
        val result = deletePrimaryKeyUseCase(DeletePrimaryKeyUseCase.Param(passphrase))
        if (result.isFailure) {
            setEvent(RequestDeleteError(result.exceptionOrNull()?.message.orUnknownError()))
            return@launch
        }
        if (result.isSuccess) {
            appScope.launch(dispatcher) {
                clearInfoSessionUseCase.invoke(Unit)
                event(DeletePrimaryKeySuccess)
            }
        }
    }

    fun checkNeedPassphraseSent() {
        setEvent(Loading)
        viewModelScope.launch {
            val isNeeded = primaryKeySignerInfoHolder.isNeedPassphraseSent()
            setEvent(CheckNeedPassphraseSent(isNeeded))
        }
    }

}
