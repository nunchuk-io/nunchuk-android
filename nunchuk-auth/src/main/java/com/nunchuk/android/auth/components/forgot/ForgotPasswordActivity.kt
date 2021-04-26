package com.nunchuk.android.auth.components.forgot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.forgot.ForgotPasswordEvent.*
import com.nunchuk.android.auth.databinding.ActivityForgotPasswordBinding
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

class ForgotPasswordActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ForgotPasswordViewModel by lazy {
        ViewModelProviders.of(this, factory).get(ForgotPasswordViewModel::class.java)
    }

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()
    }

    private fun setupViews() {
        binding.forgotPassword.setOnClickListener { onForgotPasswordClicked() }
    }

    private fun onForgotPasswordClicked() {
        viewModel.handleForgotPassword(binding.email.getEditText())
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                EmailInvalidEvent -> showEmailError(R.string.nc_text_email_invalid)
                EmailRequiredEvent -> showEmailError(R.string.nc_text_required)
                EmailValidEvent -> hideEmailError()
                is ForgotPasswordSuccessEvent -> openRecoverPasswordScreen(it.email)
                is ForgotPasswordErrorEvent -> showToast(it.errorMessage.orUnknownError())
            }
        }
    }

    private fun openRecoverPasswordScreen(email: String) {
        navigator.openRecoverPasswordScreen(this, email)
    }

    private fun hideEmailError() {
        binding.email.hideError()
    }

    private fun showEmailError(errorMessageId: Int) {
        binding.email.setError(getString(errorMessageId))
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, ForgotPasswordActivity::class.java))
        }
    }

}