package com.nunchuk.android.signer.components.details

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.core.util.toReadableString
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.toSpec
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.components.details.SignerInfoEvent.*
import com.nunchuk.android.signer.databinding.ActivitySignerInfoBinding
import com.nunchuk.android.widget.NCInputDialog
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

class SignerInfoActivity : BaseActivity<ActivitySignerInfoBinding>() {

    @Inject
    lateinit var factory: NunchukFactory

    private val viewModel: SignerInfoViewModel by viewModels { factory }

    override fun initializeBinding() = ActivitySignerInfoBinding.inflate(layoutInflater)

    private val args: SignerInfoArgs by lazy { SignerInfoArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
        observeEvent()
        viewModel.init(args.id, args.software)
    }

    private fun observeEvent() {
        viewModel.event.observe(this, ::handleEvent)
        viewModel.state.observe(this, ::handleState)
    }

    private fun handleState(state: SignerInfoState) {
        state.remoteSigner?.let(::bindRemoteSigner)
        state.masterSigner?.let(::bindMasterSigner)
    }

    private fun bindMasterSigner(signer: MasterSigner) {
        binding.signerTypeIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_software_signer_big))
        binding.fingerprint.isVisible = true
        binding.fingerprint.text = signer.device.masterFingerprint
        binding.signerSpec.isVisible = false
        binding.signerType.text = signer.type.toReadableString(this)
    }

    private fun bindRemoteSigner(signer: SingleSigner) {
        binding.signerTypeIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_air_signer_big))
        binding.signerSpec.isVisible = true
        binding.signerSpec.text = signer.toSpec()
        binding.fingerprint.isVisible = false
        binding.signerType.text = signer.type.toReadableString(this)
    }

    private fun handleEvent(event: SignerInfoEvent) {
        when (event) {
            is UpdateNameSuccessEvent -> {
                binding.signerName.text = event.signerName
                showEditSignerNameSuccess()
            }
            RemoveSignerCompletedEvent -> openMainScreen()
            is RemoveSignerErrorEvent -> showToast(event.message)
            is UpdateNameErrorEvent -> showToast(event.message)
            is HealthCheckErrorEvent -> showHealthCheckError(event)
            HealthCheckSuccessEvent -> NCToastMessage(this).showMessage(
                message = getString(R.string.nc_txt_run_health_check_success_event, args.name),
                icon = R.drawable.ic_check_circle_outline
            )
        }
    }

    private fun showHealthCheckError(event: HealthCheckErrorEvent) {
        if (event.message.isNullOrEmpty()) {
            NCToastMessage(this).show(getString(R.string.nc_txt_run_health_check_error_event, args.name))
        } else {
            NCToastMessage(this).showWarning(event.message)
        }
    }

    private fun setupViews() {
        binding.signerName.text = args.name
        if (args.justAdded) {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_add_signer_success, args.name),
                icon = R.drawable.ic_check_circle_outline
            )
            if (args.setPassphrase) {
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_set_passphrase_success),
                    offset = R.dimen.nc_padding_44,
                    dismissTime = 4000L
                )
            }
        }
        binding.btnDone.isVisible = args.justAdded
        if (args.software) {
            binding.signerType.text = getString(R.string.nc_signer_type_software)
        } else {
            binding.signerType.text = getString(R.string.nc_signer_type_air_gapped)
        }
        binding.toolbar.setNavigationOnClickListener { openMainScreen() }
        binding.btnDone.setOnClickListener { openMainScreen() }
        binding.btnRemove.setOnClickListener { viewModel.handleRemoveSigner() }
        binding.signerName.setOnClickListener { onEditClicked() }
        binding.btnHealthCheck.setOnClickListener { handleRunHealthCheck() }
    }

    private fun handleRunHealthCheck() {
        val masterSigner = viewModel.state.value?.masterSigner
        if (masterSigner != null && masterSigner.software) {
            if (masterSigner.device.needPassPhraseSent) {
                NCInputDialog(this).showDialog(
                    title = getString(R.string.nc_transaction_enter_passphrase),
                    onConfirmed = { viewModel.handleHealthCheck(masterSigner, it) }
                )
            } else {
                viewModel.handleHealthCheck(masterSigner)
            }
        }
    }

    private fun openMainScreen() {
        if (args.justAdded) {
            navigator.openMainScreen(this)
        } else {
            finish()
        }
    }

    private fun onEditClicked() {
        val bottomSheet = SignerUpdateBottomSheet.show(
            fragmentManager = supportFragmentManager,
            signerName = binding.signerName.text.toString()
        )
        bottomSheet.setListener(viewModel::handleEditCompletedEvent)
    }

    private fun showEditSignerNameSuccess() {
        binding.signerName.post {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_change_signer_success),
                icon = R.drawable.ic_check_circle_outline
            )
        }
    }

    companion object {

        fun start(
            activityContext: Context,
            id: String,
            name: String,
            justAdded: Boolean = false,
            software: Boolean = false,
            setPassphrase: Boolean = false
        ) {
            activityContext.startActivity(
                SignerInfoArgs(
                    id = id,
                    name = name,
                    justAdded = justAdded,
                    software = software,
                    setPassphrase = setPassphrase
                ).buildIntent(activityContext)
            )
        }
    }

}