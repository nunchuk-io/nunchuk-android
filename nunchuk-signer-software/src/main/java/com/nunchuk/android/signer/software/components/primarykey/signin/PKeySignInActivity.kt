package com.nunchuk.android.signer.software.components.primarykey.signin

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.PrimaryKey
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeySignInBinding
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeySignInActivity : BaseActivity<ActivityPkeySignInBinding>() {

    @Inject
    internal lateinit var vmFactory: PKeySignInViewModel.Factory

    private val args by lazy { PKeySignInArgs.deserializeFrom(intent) }

    private val viewModel: PKeySignInViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    override fun initializeBinding() = ActivityPkeySignInBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: PKeySignInEvent) {
        when (event) {
            is PKeySignInEvent.LoadingEvent -> showOrHideLoading(event.loading)
            is PKeySignInEvent.ProcessErrorEvent -> NCToastMessage(this).showError(event.message)
            is PKeySignInEvent.SignInSuccessEvent -> openMainScreen()
            is PKeySignInEvent.InitFailure -> {
                NCToastMessage(this).showError(event.message)
                finish()
            }
        }
    }

    private fun openMainScreen() {
        hideLoading()
        val messages = ArrayList<String>()
        messages.add(String.format(getString(R.string.nc_text_signed_in_with_data), args.primaryKey.account))
        navigator.openMainScreen(
            this, loginHalfToken = accountManager.getAccount().token,
            deviceId = accountManager.getAccount().deviceId, messages = messages,
            isClearTask = true
        )
        finish()
    }

    private fun setupViews() {
        binding.keyName.post {
            binding.keyName.getEditTextView().setText(args.primaryKey.account)
        }

        binding.passphrase.makeMaskedInput()
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.staySignIn.setOnCheckedChangeListener { _, checked ->
            viewModel.setStaySignedIn(
                checked
            )
        }
        binding.signIn.setOnDebounceClickListener { onSignInClick() }
        clearInputFields()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearInputFields()
    }

    private fun clearInputFields() {
        clearInputField(binding.keyName.getEditTextView())
        clearInputField(binding.passphrase.getEditTextView())
    }

    private fun clearInputField(edittext: EditText) {
        edittext.clearComposingText()
        edittext.setText("")
    }

    private fun onSignInClick() {
        viewModel.handleSignIn(binding.passphrase.getEditText())
    }

    companion object {
        fun start(
            activityContext: Context,
            primaryKey: PrimaryKey
        ) {
            activityContext.startActivity(
                PKeySignInArgs(
                    primaryKey = primaryKey,
                ).buildIntent(
                    activityContext
                )
            )
        }
    }

}
