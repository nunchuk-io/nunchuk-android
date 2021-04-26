package com.nunchuk.android.signer.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.add.AddSignerEvent.*
import com.nunchuk.android.signer.databinding.ActivityAddSignerBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.SimpleTextWatcher
import com.nunchuk.android.widget.util.heightExtended
import javax.inject.Inject

class AddSignerActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: AddSignerViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AddSignerViewModel::class.java)
    }

    private lateinit var binding: ActivityAddSignerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddSignerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is AddSignerSuccessEvent -> openSignerInfo(it.signerName, it.signerSpec)
                InvalidSignerSpecEvent -> binding.signerSpec.setError(getString(R.string.nc_error_invalid_signer_spec))
                is AddSignerErrorEvent -> NCToastMessage(this).showWarning(it.message)
                SignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
            }
        }
    }

    private fun openSignerInfo(signerName: String, signerSpec: String) {
        finish()
        navigator.openSignerInfoScreen(this, signerName, signerSpec, true)
    }

    private fun setupViews() {
        binding.signerName.getEditTextView().filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))
        updateCounter(0)
        binding.signerName.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateCounter(s.length)
            }
        })

        binding.addSignerViaQR.setOnClickListener { showToast("Scan QR coming soon") }
        binding.signerSpec.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
        binding.addSigner.setOnClickListener {
            viewModel.handleAddSigner(binding.signerName.getEditText(), binding.signerSpec.getEditText())
        }
    }

    private fun updateCounter(length: Int) {
        val counterValue = "$length/$MAX_LENGTH"
        binding.signerNameCounter.text = counterValue
    }

    companion object {
        private const val MAX_LENGTH = 20
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AddSignerActivity::class.java))
        }
    }

}