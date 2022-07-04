package com.nunchuk.android.settings

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CleanUpCryptoAssetsUseCase
import com.nunchuk.android.core.profile.UserProfileRepository
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.settings.DeleteAccountEvent.*
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DeleteAccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val repository: UserProfileRepository,
    private val cleanUpCryptoAssetsUseCase: CleanUpCryptoAssetsUseCase,
) : NunchukViewModel<DeleteAccountState, DeleteAccountEvent>() {

    override val initialState = DeleteAccountState("")

    init {
        updateState { copy(email = accountManager.getAccount().email) }
    }

    fun sendConfirmDeleteAccount(confirmationCode: String) {
        viewModelScope.launch {
            repository.confirmDeleteAccount(confirmationCode)
                .onStart { event(Loading) }
                .flowOn(Dispatchers.IO)
                .onException { event(ConfirmDeleteError(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { handleSuccess() }
        }
    }

    private fun handleSuccess() {
        viewModelScope.launch {
            cleanUpCryptoAssetsUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    accountManager.clearUserData()
                    accountManager.signOut(onSignedOut = {
                        event(ConfirmDeleteSuccess)
                    })
                }
        }
    }

    fun signOutNunchuk() {
        viewModelScope.launch {
            repository.signOut()
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect {}
        }
    }

}
