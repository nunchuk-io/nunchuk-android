package com.nunchuk.android.main.membership.authentication.confirmationcode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.main.membership.byzantine.groupchathistory.GroupChatHistoryFragmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ConfirmationCodeViewModel @Inject constructor(
    accountManager: AccountManager,
) : ViewModel() {

    private val _event = MutableSharedFlow<ConfirmChangeEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ConfirmChangeState())
    val state = _state.asStateFlow()

    val email = accountManager.getAccount().email

    fun onCodeChange(code: String) {
        _state.update {
            it.copy(code = code)
        }
    }
}