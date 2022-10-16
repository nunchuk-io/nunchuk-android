package com.nunchuk.android.signer.software.components.primarykey.manuallyusername

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityPkeyManuallyUsernameBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PKeyManuallyUsernameActivity : BaseActivity<ActivityPkeyManuallyUsernameBinding>() {

    private val viewModel: PKeyManuallyUsernameViewModel by viewModels()

    override fun initializeBinding() =
        ActivityPkeyManuallyUsernameBinding.inflate(layoutInflater)

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

    private fun handleState(state: PKeyManuallyUsernameState) {
        val signerName = state.username
        val counter = "${signerName.length}/${MAX_LENGTH}"
        binding.usernameCounter.text = counter
    }

    private fun handleEvent(event: PKeyManuallyUsernameEvent) {
        when (event) {
            is PKeyManuallyUsernameEvent.LoadingEvent -> showOrHideLoading(loading = event.loading)
            is PKeyManuallyUsernameEvent.ProcessFailure -> NCToastMessage(this).showError(getString(R.string.nc_primary_key_signin_manually_account_not_found))
            is PKeyManuallyUsernameEvent.CheckUsernameSuccess -> navigator.openPrimaryKeyManuallySignatureScreen(this, event.username)
        }
    }

    private fun setupViews() {
        binding.usernameInput.addTextChangedCallback {
            viewModel.updateUsername(it)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinue() }
    }

    companion object {
        private const val MAX_LENGTH = 20

        fun start(
            activityContext: Context
        ) {
            activityContext.startActivity(
                Intent(activityContext, PKeyManuallyUsernameActivity::class.java)
            )
        }
    }
}