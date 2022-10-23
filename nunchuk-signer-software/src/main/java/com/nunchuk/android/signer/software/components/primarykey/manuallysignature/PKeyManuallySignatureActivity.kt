package com.nunchuk.android.signer.software.components.primarykey.manuallysignature

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeyManuallySignatureBinding
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.utils.getTrimmedText
import com.nunchuk.android.utils.viewModelProviderFactoryOf
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeyManuallySignatureActivity : BaseActivity<ActivityPkeyManuallySignatureBinding>() {

    @Inject
    internal lateinit var vmFactory: PKeyManuallySignatureViewModel.Factory

    private val args: PKeyManuallySignatureArgs by lazy {
        PKeyManuallySignatureArgs.deserializeFrom(
            intent
        )
    }

    private val viewModel: PKeyManuallySignatureViewModel by viewModels {
        viewModelProviderFactoryOf {
            vmFactory.create(args)
        }
    }

    override fun initializeBinding() =
        ActivityPkeyManuallySignatureBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: PKeyManuallySignatureState) {
        binding.challengeInput.setText(state.challengeMessage)
    }

    private fun handleEvent(event: PKeyManuallySignatureEvent) {
        when (event) {
            is PKeyManuallySignatureEvent.LoadingEvent -> showOrHideLoading(loading = event.loading)
            is PKeyManuallySignatureEvent.ProcessFailure -> {
                hideLoading()
                NCToastMessage(this).showError(event.message)
            }
            is PKeyManuallySignatureEvent.SignInSuccess -> viewModel.getTurnOnNotification()
            is PKeyManuallySignatureEvent.GetTurnOnNotificationSuccess -> openNextScreen(event.isTurnOn)
        }
    }

    private fun openNextScreen(turnOn: Boolean) {
        val isEnabledNotification = NotificationUtils.areNotificationsEnabled(this@PKeyManuallySignatureActivity)
        val messages = ArrayList<String>()
        messages.add(String.format(getString(R.string.nc_text_signed_in_with_data), args.username))
        if (turnOn && isEnabledNotification) {
            navigator.openPrimaryKeyNotificationScreen(
                this@PKeyManuallySignatureActivity,
                messages = messages,
                primaryKeyFlow = PrimaryKeyFlow.SIGN_IN
            )
        } else {
            navigator.openMainScreen(
                this@PKeyManuallySignatureActivity,
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
        binding.yourSignatureInput.addTextChangedCallback {
            viewModel.updateSignature(it)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnSignIn.setOnClickListener { viewModel.handleSignIn() }
        binding.yourSignatureInput.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_140))
        binding.staySignIn.setOnCheckedChangeListener { _, checked ->
            viewModel.updateStaySignedIn(
                checked
            )
        }
        binding.copyImage.setOnDebounceClickListener {
            copyChallengeMessageText(binding.challengeInput.getTrimmedText())
        }
        binding.reloadImage.setOnDebounceClickListener {
            viewModel.getChallengeMessage()
        }
    }

    private fun copyChallengeMessageText(text: String) {
        this.copyToClipboard(label = "Nunchuk", text = text)
        NCToastMessage(this).showMessage(getString(R.string.nc_primary_key_signin_manually_copy_clipboard))
    }

    companion object {

        fun start(
            activityContext: Context,
            username: String
        ) {
            activityContext.startActivity(
                PKeyManuallySignatureArgs(
                    username = username,
                ).buildIntent(
                    activityContext
                )
            )
        }
    }
}