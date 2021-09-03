package com.nunchuk.android.auth.components.changepass

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.*
import com.nunchuk.android.auth.domain.ChangePasswordUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    accountManager: AccountManager
) : NunchukViewModel<Unit, ChangePasswordEvent>() {

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
                event(LoadingEvent)
                changePasswordUseCase.execute(oldPassword, newPassword)
                    .flowOn(Dispatchers.Main)
                    .catch { event(ChangePasswordSuccessError(it.message)) }
                    .onEach { event(ChangePasswordSuccessEvent) }
                    .launchIn(viewModelScope)
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