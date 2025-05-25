package com.nunchuk.android.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.usecase.GetFirstCreateEmailUseCase
import com.nunchuk.android.usecase.GetHasWalletInGuestModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TurnNotificationViewModel @Inject constructor(
    private val getHasWalletInGuestModeUseCase: GetHasWalletInGuestModeUseCase,
    private val getFirstCreateEmailUseCase: GetFirstCreateEmailUseCase,
    private val accountManager: AccountManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TurnNotificationUiState())
    val uiState: StateFlow<TurnNotificationUiState> = _uiState

    fun checkShowGuestWalletNotice() {
        viewModelScope.launch {
            combine(
                getFirstCreateEmailUseCase(Unit).map { it.getOrThrow() },
                getHasWalletInGuestModeUseCase(Unit).map { it.getOrThrow() }
            ) { firstCreateEmail, hasWalletInGuestMode ->
                val accountEmail = accountManager.getAccount().email
                (accountEmail == firstCreateEmail && hasWalletInGuestMode)
            }.collect { isShowGuestWalletNotice ->
                _uiState.update { it.copy(isShowGuestWalletNotice = isShowGuestWalletNotice) }
            }
        }
    }

    val isShowGuestWalletNotice: Boolean
        get() = _uiState.value.isShowGuestWalletNotice
} 