package com.nunchuk.android.signer.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.add.AddSignerEvent.*
import com.nunchuk.android.signer.databinding.ActivityAddSignerBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.SimpleTextWatcher
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AddSignerActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: AddSignerViewModel by lazy {
        ViewModelProviders.of(this, factory).get(AddSignerViewModel::class.java)
    }

    private lateinit var binding: ActivityAddSignerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityAddSignerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()

        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is AddSignerSuccessEvent -> openSignerInfo(it.id, it.name)
                InvalidSignerSpecEvent -> binding.signerSpec.setError(getString(R.string.nc_error_invalid_signer_spec))
                is AddSignerErrorEvent -> NCToastMessage(this).showWarning(it.message)
                SignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
            }
        }
    }

    private fun openSignerInfo(id: String, name: String) {
        finish()
        navigator.openSignerInfoScreen(this, id = id, name = name, justAdded = true)
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
        binding.toolbar.setNavigationOnClickListener {
            finish()
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