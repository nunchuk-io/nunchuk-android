package com.nunchuk.android.signer.details

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.arch.vm.NunchukFactory
import com.nunchuk.android.core.util.showToast
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivitySignerInfoBinding
import com.nunchuk.android.signer.details.SignerInfoEvent.*
import com.nunchuk.android.signer.util.toSigner
import com.nunchuk.android.widget.NCToastMessage
import javax.inject.Inject

class SignerInfoActivity : BaseActivity() {

    @Inject
    lateinit var factory: NunchukFactory

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
        viewModel.init(args.signerSpec)

    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is UpdateNameSuccessEvent -> {
                    binding.signerName.text = it.signerName
                    showEditSignerNameSuccess()
                }
                RemoveSignerCompletedEvent -> finish()
                is RemoveSignerErrorEvent -> showToast(it.message)
                is UpdateNameErrorEvent -> showToast(it.message)
            }
        }
    }

    private fun setupViews() {
        val signer = args.signerSpec.toSigner()
        binding.signerName.text = args.signerName
        binding.signerSpec.text = args.signerSpec
        binding.btnDone.setOnClickListener { finish() }
        binding.btnRemove.setOnClickListener { viewModel.handleRemoveSigner(signer) }
        binding.signerName.setOnClickListener { onEditClicked() }
        if (args.justAdded) {
            NCToastMessage(this).show(R.string.nc_text_add_signer_success)
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

        fun start(activityContext: Context, signerName: String, signerSpec: String, justAdded: Boolean = false) {
            activityContext.startActivity(
                SignerInfoArgs(
                    signerName = signerName,
                    signerSpec = signerSpec,
                    justAdded = justAdded
                ).buildIntent(activityContext)
            )
        }
    }

}