package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.SyncFileCallBack
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.matrix.UploadFileUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.profile.UpdateUseProfileUseCase
import com.nunchuk.android.core.provider.AppInfoProvider
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val appInfoProvider: AppInfoProvider,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUseProfileUseCase: UpdateUseProfileUseCase,
    private val uploadFileUseCase: UploadFileUseCase
) : NunchukViewModel<AccountState, AccountEvent>() {

    override val initialState = AccountState()

    init {
        updateState {
            copy(
                account = accountManager.getAccount(),
                appVersion = appInfoProvider.getAppVersion()
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
        if (SignInModeHolder.currentMode == SignInMode.GUEST_MODE) {
            event(
                AccountEvent.GetUserProfileGuestEvent
            )
            return
        }
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
        accountManager.signOut()
        event(AccountEvent.SignOutEvent)
    }
}