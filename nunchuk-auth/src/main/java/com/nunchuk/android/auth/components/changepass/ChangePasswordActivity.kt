package com.nunchuk.android.auth.components.changepass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.*
import com.nunchuk.android.auth.databinding.ActivityChangePasswordBinding
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

class ChangePasswordActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ChangePasswordViewModel by lazy {
        ViewModelProviders.of(this, factory).get(ChangePasswordViewModel::class.java)
    }

    private lateinit var binding: ActivityChangePasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            }
        }
    }

    private fun showEmailConfirmation(email: String) {
        binding.emailSentDescription.text = getString(R.string.nc_text_email_sent, email)
    }

    private fun setupViews() {
        val passwordInputType = PasswordTransformationMethod.getInstance()
        binding.oldPassword.getEditTextView().transformationMethod = passwordInputType
        binding.newPassword.getEditTextView().transformationMethod = passwordInputType
        binding.confirmPassword.getEditTextView().transformationMethod = passwordInputType
        binding.changePassword.setOnClickListener { onChangePasswordClicked() }
        binding.signIn.setOnClickListener { openLoginScreen() }
    }

    private fun openLoginScreen() {
        finish()
        navigator.openSignInScreen(this)
    }

    private fun showChangePasswordError(errorMessage: String) {
        showToast(errorMessage)
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

