package com.nunchuk.android.signer.details

import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.ext.isVisible
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.toSpec
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivitySignerInfoBinding
import com.nunchuk.android.signer.details.SignerInfoEvent.*
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

class SignerInfoActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SignerInfoViewModel by lazy {
        ViewModelProviders.of(this, factory).get(SignerInfoViewModel::class.java)
    }

    private lateinit var binding: ActivitySignerInfoBinding

    private val args: SignerInfoArgs by lazy { SignerInfoArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignerInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }

    private fun bindRemoteSigner(signer: SingleSigner) {
        binding.signerTypeIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_air_signer_big))
        binding.signerSpec.isVisible = true
        binding.signerSpec.text = signer.toSpec()
        binding.fingerprint.isVisible = false
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
        }
    }

    private fun setupViews() {
        binding.signerName.text = args.name
        if (args.justAdded) {
            NCToastMessage(this).showMessage(
                message = getString(R.string.nc_text_add_signer_success, args.name)
            )
            if (args.software) {
                NCToastMessage(this).showMessage(
                    message = getString(R.string.nc_text_set_passphrase_success),
                    offset = R.dimen.nc_padding_44,
                    dismissTime = 4000L
                )
            }
        }
        if (args.software) {
            binding.signerType.text = getString(R.string.nc_signer_type_software)
        } else {
            binding.signerType.text = getString(R.string.nc_signer_type_air_gapped)
        }
        binding.toolbar.setOnClickListener { openMainScreen() }
        binding.btnDone.setOnClickListener { openMainScreen() }
        binding.btnRemove.setOnClickListener { viewModel.handleRemoveSigner() }
        binding.signerName.setOnClickListener { onEditClicked() }
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
        binding.signerName.post { NCToastMessage(this).show(R.string.nc_text_change_signer_success) }
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