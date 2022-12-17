package com.nunchuk.android.main.components.tabs.services.emergencylockdown.lockdownsuccess

import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
class EmergencyLockdownSuccessViewModel @Inject constructor(
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val appScope: CoroutineScope,
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _event = MutableSharedFlow<EmergencyLockdownSuccessEvent>()
    val event = _event.asSharedFlow()

    fun onContinueClicked() {
        appScope.launch {
            repository.signOut()
                .flowOn(Dispatchers.IO)
                .onException {
                    _event.emit(EmergencyLockdownSuccessEvent.Loading(false))
                }
                .first()
            _event.emit(EmergencyLockdownSuccessEvent.Loading(true))
            withContext(dispatcher) {
                clearInfoSessionUseCase.invoke(Unit)
            }
            _event.emit(EmergencyLockdownSuccessEvent.SignOut)
        }
    }

}