package com.nunchuk.android.main.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.SetFirstCreateEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuestWalletNoticeViewModel @Inject constructor(
    private val setFirstCreateEmailUseCase: SetFirstCreateEmailUseCase
) : ViewModel() {

    fun handledFirstCreateEmail() {
        viewModelScope.launch {
            setFirstCreateEmailUseCase(
                SetFirstCreateEmailUseCase.Params(email = DEFAULT_HANDLED_EMAIL, isForce = true)
            )
        }
    }

    companion object {
        const val DEFAULT_HANDLED_EMAIL = "xxxxxxxxxxxxxxxx"
    }
} 