package com.nunchuk.android.main.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.SetFirstCreatedChatIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuestWalletNoticeViewModel @Inject constructor(
    private val setFirstCreatedChatIdUseCase: SetFirstCreatedChatIdUseCase
) : ViewModel() {

    fun handledFirstCreateEmail() {
        viewModelScope.launch {
            setFirstCreatedChatIdUseCase(
                SetFirstCreatedChatIdUseCase.Params(chatId = DEFAULT_HANDLED_CHAT_ID, isForce = true)
            )
        }
    }

    companion object {
        const val DEFAULT_HANDLED_CHAT_ID = "xxxxxxxxxxxxxxxx"
    }
} 