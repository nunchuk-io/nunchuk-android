package com.nunchuk.android.auth.components.changepass

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NCViewModel
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.*
import com.nunchuk.android.auth.domain.ChangePasswordUseCase
import com.nunchuk.android.auth.domain.RecoverPasswordUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.model.Result
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ChangePasswordViewModel @Inject constructor(
    private val recoverPasswordUseCase: RecoverPasswordUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    accountManager: AccountManager
) : NCViewModel<Unit, ChangePasswordEvent>() {

    private val account = accountManager.getAccount()

    init {
        if (!account.activated) {
            event(ShowEmailSentEvent(account.email))
        }
    }

    override val initialState = Unit

    fun handleChangePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            val isOldPasswordValid = validateOldPassword(oldPassword)
            val isNewPasswordValid = validateNewPassword(newPassword)
            val isConfirmPasswordValid = validateConfirmPassword(confirmPassword)
            val isConfirmPasswordMatched = validateConfirmPasswordMatched(newPassword, confirmPassword)
            if (isOldPasswordValid && isNewPasswordValid && isConfirmPasswordValid && isConfirmPasswordMatched) {
                val result = if (account.activated) {
                    changePasswordUseCase.execute(oldPassword, newPassword, confirmPassword)
                } else {
                    recoverPasswordUseCase.execute(account.email, oldPassword, newPassword, confirmPassword)
                }
                if (result is Result.Success) {
                    event(ChangePasswordSuccessEvent)
                } else if (result is Result.Error) {
                    event(ChangePasswordSuccessError(result.exception.message))
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

    fun validateOldPassword(oldPassword: String) = when {
        oldPassword.isEmpty() -> doAfterValidate(false) { event(OldPasswordRequiredEvent) }
        else -> doAfterValidate { event(OldPasswordValidEvent) }
    }

    fun validateNewPassword(newPassword: String) = when {
        newPassword.isEmpty() -> doAfterValidate(false) { event(NewPasswordRequiredEvent) }
        else -> doAfterValidate { event(NewPasswordValidEvent) }
    }

    fun validateConfirmPassword(confirmPassword: String) = when {
        confirmPassword.isEmpty() -> doAfterValidate(false) { event(ConfirmPasswordRequiredEvent) }
        else -> doAfterValidate { event(ConfirmPasswordValidEvent) }
    }

}