package com.nunchuk.android.signer.ss.passphrase

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivitySetPassphraseBinding
import com.nunchuk.android.signer.ss.passphrase.SetPassphraseEvent.*
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.SimpleTextWatcher
import com.nunchuk.android.widget.util.passwordEnabled
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class SetPassphraseActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SetPassphraseViewModel by lazy {
        ViewModelProviders.of(this, factory).get(SetPassphraseViewModel::class.java)
    }

    private lateinit var binding: ActivitySetPassphraseBinding

    private val args: SetPassphraseActivityArgs by lazy { SetPassphraseActivityArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivitySetPassphraseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.init(args.mnemonic, args.signerName)
        setupViews()

        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: SetPassphraseState) {
        Log.d(TAG, "handleState($state)")
    }

    private fun handleEvent(event: SetPassphraseEvent) {
        when (event) {
            PassPhraseRequiredEvent -> binding.passphrase.setError(getString(R.string.nc_text_required))
            ConfirmPassPhraseRequiredEvent -> binding.confirmPassphrase.setError(getString(R.string.nc_text_required))
            ConfirmPassPhraseNotMatchedEvent -> binding.confirmPassphrase.setError(getString(R.string.nc_text_confirm_passphrase_not_matched))
            is CreateSoftwareSignerCompletedEvent -> openSignerInfoScreen(event.id, event.skipPassphrase)
            is CreateSoftwareSignerErrorEvent -> NCToastMessage(this).showError(event.message)
            PassPhraseValidEvent -> removeValidationError()
        }
    }

    private fun removeValidationError() {
        binding.passphrase.hideError()
        binding.confirmPassphrase.hideError()
    }

    private fun openSignerInfoScreen(id: String, skipPassphrase: Boolean) {
        Log.d(TAG, "Create software signer completed::(id=$id, skipPassphrase=$skipPassphrase)")
        NCToastMessage(this).showMessage("Create software signer successful")
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.passphrase.passwordEnabled()
        binding.passphrase.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.updatePassphrase("$s")
            }
        })
        binding.confirmPassphrase.passwordEnabled()
        binding.confirmPassphrase.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.updateConfirmPassphrase("$s")
            }
        })
        binding.btnNoPassphrase.setOnClickListener { viewModel.skipPassphraseEvent() }
        binding.btnSetPassphrase.setOnClickListener { viewModel.confirmPassphraseEvent() }
    }

    companion object {
        private const val TAG = "SetPassphraseActivity"

        fun start(activityContext: Context, mnemonic: String, signerName: String) {
            activityContext.startActivity(
                SetPassphraseActivityArgs(
                    mnemonic = mnemonic,
                    signerName = signerName
                ).buildIntent(activityContext)
            )
        }
    }

}