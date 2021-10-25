package com.nunchuk.android.signer.components.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.qr.QRCodeParser
import com.nunchuk.android.core.qr.startQRCodeScan
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.add.AddSignerEvent.*
import com.nunchuk.android.signer.databinding.ActivityAddSignerBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import com.nunchuk.android.widget.util.heightExtended
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setMaxLength
import javax.inject.Inject

class AddSignerActivity : BaseActivity<ActivityAddSignerBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: AddSignerViewModel by viewModels { factory }

    override fun initializeBinding() = ActivityAddSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is AddSignerSuccessEvent -> openSignerInfo(it.id, it.name)
                InvalidSignerSpecEvent -> binding.signerSpec.setError(getString(R.string.nc_error_invalid_signer_spec))
                is AddSignerErrorEvent -> onAddAirSignerError(it.message)
                SignerNameRequiredEvent -> binding.signerName.setError(getString(R.string.nc_text_required))
                LoadingEvent -> showLoading()
                is ParseKeystoneSignerSuccess -> onParseCompleted(it.signerSpec)
            }
        }
    }

    private fun onParseCompleted(signerSpec: String) {
        hideLoading()
        binding.signerSpec.getEditTextView().setText(signerSpec)
    }

    private fun onAddAirSignerError(message: String) {
        hideLoading()
        NCToastMessage(this).showWarning(message)
    }

    private fun openSignerInfo(id: String, name: String) {
        hideLoading()
        finish()
        navigator.openSignerInfoScreen(this, id = id, name = name, justAdded = true)
    }

    private fun setupViews() {
        binding.signerName.setMaxLength(MAX_LENGTH)
        updateCounter(0)
        binding.signerName.addTextChangedCallback {
            updateCounter(it.length)
        }

        binding.addSignerViaQR.setOnClickListener { startQRCodeScan() }
        binding.signerSpec.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_180))
        binding.addSigner.setOnClickListener {
            viewModel.handleAddSigner(binding.signerName.getEditText(), binding.signerSpec.getEditText())
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        QRCodeParser.parse(requestCode, resultCode, data)?.apply {
            viewModel.handleAddQrData(this)
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