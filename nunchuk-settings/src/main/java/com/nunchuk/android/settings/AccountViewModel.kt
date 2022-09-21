package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.SyncFileCallBack
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.UploadFileUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.profile.UpdateUseProfileUseCase
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@HiltViewModel
internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUseProfileUseCase: UpdateUseProfileUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val repository: UserProfileRepository,
    private val sessionHolder: SessionHolder,
    private val appScope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : NunchukViewModel<AccountState, AccountEvent>() {

    override val initialState = AccountState()

    init {
        updateState {
            copy(
                account = accountManager.getAccount(),
            )
        }

        SyncFileEventHelper.syncFileExecutor = object : SyncFileCallBack {
            override fun onSync(finished: Boolean, progress: Int) {
                postState {
                    copy(
                        syncProgress = progress
                    )
                }
            }
        }
    }

    fun getCurrentAccountInfo() = accountManager.getAccount()

    fun getCurrentUser() {
        if (SignInModeHolder.currentMode.isGuestMode()) {
            event(AccountEvent.GetUserProfileGuestEvent)
        } else {
            viewModelScope.launch {
                getUserProfileUseCase.execute()
                    .flowOn(Dispatchers.IO)
                    .onException { }
                    .flowOn(Dispatchers.Main)
                    .collect {
                        updateStateUserAccount()
                        event(
                            AccountEvent.GetUserProfileSuccessEvent(
                                name = accountManager.getAccount().name,
                                avatarUrl = accountManager.getAccount().avatarUrl
                            )
                        )
                    }
            }
        }
    }

    fun updateUserProfile(name: String? = null, avatarUrl: String? = null) {
        viewModelScope.launch {
            updateUseProfileUseCase.execute(name, avatarUrl)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateStateUserAccount()
                }
        }
    }

    fun uploadPhotoToMaTrix(fileData: ByteArray) {
        viewModelScope.launch {
            uploadFileUseCase.execute(System.currentTimeMillis().toString(), "image/jpeg", fileData)
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    event(
                        AccountEvent.UploadPhotoSuccessEvent(matrixUri = it.contentUri)
                    )
                }
        }
    }

    private fun updateStateUserAccount() {
        updateState {
            copy(
                account = accountManager.getAccount()
            )
        }
    }

    fun handleSignOutEvent() {
        viewModelScope.launch {
            repository.signOut()
                .flowOn(Dispatchers.IO)
                .onException {
                    event(AccountEvent.LoadingEvent(false))
                }
                .collect {}
        }
        appScope.launch {
            event(AccountEvent.LoadingEvent(true))
            withContext(dispatcher) {
                sessionHolder.clearActiveSession()
                accountManager.signOut()
            }
            event(AccountEvent.SignOutEvent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        SyncFileEventHelper.syncFileExecutor = object : SyncFileCallBack {
            override fun onSync(finished: Boolean, progress: Int) {}
        }
    }
}