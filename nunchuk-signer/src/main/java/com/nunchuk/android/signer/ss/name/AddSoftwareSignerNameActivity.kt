package com.nunchuk.android.signer.ss.name

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityAddNameBinding
import com.nunchuk.android.signer.ss.name.AddSoftwareSignerNameEvent.SignerNameInputCompletedEvent
import com.nunchuk.android.signer.ss.name.AddSoftwareSignerNameEvent.SignerNameRequiredEvent
import com.nunchuk.android.widget.util.SimpleTextWatcher
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AddSoftwareSignerNameActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: AddSoftwareSignerNameViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AddSoftwareSignerNameViewModel::class.java)
    }

    private lateinit var binding: ActivityAddNameBinding

    private val args: AddSoftwareSignerNameArgs by lazy { AddSoftwareSignerNameArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityAddNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.signerName.getEditTextView().filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        binding.signerName.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                viewModel.updateSignerName("$s")
            }
        })
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