package com.nunchuk.android.auth.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.*
import com.nunchuk.android.auth.domain.RecoverPasswordUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecoverPasswordViewModel @Inject constructor(
    private val recoverPasswordUseCase: RecoverPasswordUseCase,
) : NunchukViewModel<Unit, RecoverPasswordEvent>() {

    override val initialState = Unit
    lateinit var email: String

    fun initData(email: String) {
        this.email = email
    }

    fun handleChangePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            if (validateOldPassword(oldPassword)
                && validateNewPassword(newPassword)
                && validateConfirmPassword(confirmPassword)
                && validateConfirmPasswordMatched(newPassword, confirmPassword)
            ) {
                event(LoadingEvent)
                val result = recoverPasswordUseCase.execute(emailAddress = email, oldPassword = oldPassword, newPassword = newPassword)
                if (result is Success) {
                    event(RecoverPasswordSuccessEvent)
                } else if (result is Error) {
                    event(RecoverPasswordErrorEvent(result.exception.message))
                }
            }
        }
    }

    private fun validateConfirmPasswordMatched(newPassword: String, confirmPassword: String): Boolean {
        val matched = newPassword == confirmPassword
        if (!matched) {
            event(ConfirmPasswordNotMatchedEvent)
        }
        return matched
    }

    private fun validateOldPassword(oldPassword: String) = when {
        oldPassword.isEmpty() -> doAfterValidate(false) { event(OldPasswordRequiredEvent) }
        else -> doAfterValidate { event(OldPasswordValidEvent) }
    }

    private fun validateNewPassword(newPassword: String) = when {
        newPassword.isEmpty() -> doAfterValidate(false) { event(NewPasswordRequiredEvent) }
        else -> doAfterValidate { event(NewPasswordValidEvent) }
    }

    private fun validateConfirmPassword(confirmPassword: String) = when {
        confirmPassword.isEmpty() -> doAfterValidate(false) { event(ConfirmPasswordRequiredEvent) }
        else -> doAfterValidate { event(ConfirmPasswordValidEvent) }
    }

}