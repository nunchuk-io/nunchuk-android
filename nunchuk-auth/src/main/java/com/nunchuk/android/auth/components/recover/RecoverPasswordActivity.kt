package com.nunchuk.android.auth.components.recover

import android.content.Context
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.*
import com.nunchuk.android.auth.databinding.ActivityRecoverPasswordBinding
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

class RecoverPasswordActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory

    private val args: RecoverPasswordArgs by lazy { RecoverPasswordArgs.deserializeFrom(intent) }

    private val viewModel: RecoverPasswordViewModel by viewModels { factory }

    private lateinit var binding: ActivityRecoverPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)

        viewModel.initData(args.email)

        binding = ActivityRecoverPasswordBinding.inflate(layoutInflater)

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
                is RecoverPasswordErrorEvent -> showChangePasswordError(it.errorMessage.orUnknownError())
                is RecoverPasswordSuccessEvent -> openLoginScreen()
            }
        }
    }

    private fun setupViews() {
        val passwordInputType = PasswordTransformationMethod.getInstance()
        binding.oldPassword.getEditTextView().transformationMethod = passwordInputType
        binding.newPassword.getEditTextView().transformationMethod = passwordInputType
        binding.confirmPassword.getEditTextView().transformationMethod = passwordInputType
        binding.recoverPassword.setOnClickListener { onChangePasswordClicked() }
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
        fun start(activityContext: Context, email: String) {
            activityContext.startActivity(RecoverPasswordArgs(email).buildIntent(activityContext))
        }
    }

}

