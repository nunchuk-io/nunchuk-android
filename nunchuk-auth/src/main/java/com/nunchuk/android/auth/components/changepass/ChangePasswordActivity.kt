package com.nunchuk.android.auth.components.changepass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.*
import com.nunchuk.android.auth.databinding.ActivityChangePasswordBinding
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.passwordEnabled
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

class ChangePasswordActivity : BaseActivity<ActivityChangePasswordBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val viewModel: ChangePasswordViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityChangePasswordBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        setupViews()

        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is OldPasswordRequiredEvent -> binding.oldPassword.setError(getString(R.string.nc_text_required))
                is OldPasswordValidEvent -> binding.oldPassword.hideError()
                is NewPasswordRequiredEvent -> binding.newPassword.setError(getString(R.string.nc_text_required))
                is NewPasswordValidEvent -> binding.newPassword.hideError()
                is ConfirmPasswordRequiredEvent -> binding.confirmPassword.setError(getString(R.string.nc_text_required))
                is ConfirmPasswordValidEvent -> binding.confirmPassword.hideError()
                is ConfirmPasswordNotMatchedEvent -> binding.confirmPassword.setError(getString(R.string.nc_text_password_does_not_match))
                is ChangePasswordSuccessError -> showChangePasswordError(it.errorMessage.orUnknownError())
                is ChangePasswordSuccessEvent -> openLoginScreen()
                is ShowEmailSentEvent -> showEmailConfirmation(it.email)
                LoadingEvent -> showLoading()
            }
        }
    }

    private fun showEmailConfirmation(email: String) {
        binding.emailSentDescription.isVisible = true
        binding.emailSentDescription.text = getString(R.string.nc_text_email_sent, email)
    }

    private fun setupViews() {
        binding.oldPassword.passwordEnabled()
        binding.oldPassword.passwordEnabled()
        binding.confirmPassword.passwordEnabled()
        binding.changePassword.setOnClickListener { onChangePasswordClicked() }
        binding.signIn.setOnClickListener { openLoginScreen() }
    }

    private fun openLoginScreen() {
        hideLoading()
        finish()
        navigator.openSignInScreen(this)
    }

    private fun showChangePasswordError(errorMessage: String) {
        hideLoading()
        NCToastMessage(this).showError(errorMessage)
    }

    private fun onChangePasswordClicked() {
        viewModel.handleChangePassword(
            oldPassword = binding.oldPassword.getEditText(),
            newPassword = binding.newPassword.getEditText(),
            confirmPassword = binding.confirmPassword.getEditText()
        )
    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, ChangePasswordActivity::class.java))
        }
    }

}

