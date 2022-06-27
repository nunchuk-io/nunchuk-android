package com.nunchuk.android.signer.software.components.name

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameInputCompletedEvent
import com.nunchuk.android.signer.software.components.name.AddSoftwareSignerNameEvent.SignerNameRequiredEvent
import com.nunchuk.android.signer.software.databinding.ActivityAddNameBinding
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddSoftwareSignerNameActivity : BaseActivity<ActivityAddNameBinding>() {

    private val viewModel: AddSoftwareSignerNameViewModel by viewModels()

    private val args: AddSoftwareSignerNameArgs by lazy { AddSoftwareSignerNameArgs.deserializeFrom(intent) }

    override fun initializeBinding() = ActivityAddNameBinding.inflate(layoutInflater)

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

    private fun handleState(state: AddSoftwareSignerNameState) {
        val signerName = state.signerName
        val counter = "${signerName.length}/$MAX_LENGTH"
        binding.nameCounter.text = counter
    }

    private fun handleEvent(event: AddSoftwareSignerNameEvent) {
        when (event) {
            is SignerNameInputCompletedEvent -> openSetPassphraseScreen(event.signerName)
            SignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
        }
    }

    private fun openSetPassphraseScreen(signerName: String) {
        navigator.openSetPassphraseScreen(this, args.mnemonic, signerName)
    }

    private fun setupViews() {
        binding.signerName.setMaxLength(MAX_LENGTH)
        binding.signerName.addTextChangedCallback(viewModel::updateSignerName)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { viewModel.handleContinue() }
    }

    companion object {
        private const val MAX_LENGTH = 20

        fun start(activityContext: Context, mnemonic: String) {
            activityContext.startActivity(AddSoftwareSignerNameArgs(mnemonic = mnemonic).buildIntent(activityContext))
        }
    }

}