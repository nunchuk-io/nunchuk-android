package com.nunchuk.android.signer.software.components.primarykey.chooseusername

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeyChooseUsernameBinding
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.preventWhitespaceInput
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeyChooseUsernameActivity : BaseActivity<ActivityPkeyChooseUsernameBinding>() {

    @Inject
    internal lateinit var vmFactory: PKeyChooseUsernameViewModel.Factory

    private val args: PKeyChooseUsernameArgs by lazy {
        PKeyChooseUsernameArgs.deserializeFrom(
            intent
        )
    }

    private val viewModel: PKeyChooseUsernameViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    override fun initializeBinding() = ActivityPkeyChooseUsernameBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: PKeyChooseUsernameEvent) {
        when (event) {
            is PKeyChooseUsernameEvent.LoadingEvent -> showOrHideLoading(event.loading)
            is PKeyChooseUsernameEvent.GetDefaultUsernameSuccess -> binding.usernameInput.getEditTextView()
                .setText(event.username)
            is PKeyChooseUsernameEvent.ProcessFailure -> NCToastMessage(this).showError(event.message)
            is PKeyChooseUsernameEvent.SignUpSuccess -> viewModel.getTurnOnNotification()
            is PKeyChooseUsernameEvent.GetTurnOnNotificationSuccess -> openNextScreen(event.isTurnOn)
        }
    }

    private fun openNextScreen(isTurnOn: Boolean) {
        val isEnabledNotification = NotificationUtils.areNotificationsEnabled(this@PKeyChooseUsernameActivity)
        val messages = ArrayList<String>()
        messages.apply {
            add(String.format(getString(R.string.nc_text_key_has_been_added_data), args.signerName))
            add(getString(R.string.nc_primary_key_account_created))
        }
        if (isTurnOn && isEnabledNotification.not()) {
            navigator.openPrimaryKeyNotificationScreen(
                this@PKeyChooseUsernameActivity,
                messages = messages,
                primaryKeyFlow = PrimaryKeyFlow.SIGN_UP
            )
        } else {
            navigator.openMainScreen(
                this@PKeyChooseUsernameActivity,
                accountManager.getAccount().token,
                accountManager.getAccount().deviceId,
                messages = messages,
                isClearTask = true
            )
        }
        viewModel.updateTurnOnNotification()
        finish()
    }

    private fun setupViews() {
        binding.usernameInput.preventWhitespaceInput()
        binding.usernameInput.addTextChangedCallback {
            viewModel.updateUsername(it)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinue() }
    }

    companion object {

        fun start(
            activityContext: Context,
            mnemonic: String,
            passphrase: String,
            signerName: String
        ) {
            activityContext.startActivity(
                PKeyChooseUsernameArgs(
                    mnemonic = mnemonic,
                    passphrase = passphrase,
                    signerName = signerName
                ).buildIntent(
                    activityContext
                )
            )
        }
    }

}