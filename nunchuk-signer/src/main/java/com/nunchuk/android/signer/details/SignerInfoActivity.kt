package com.nunchuk.android.signer.details

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.signer.util.SignerInput
import com.nunchuk.android.signer.util.SignerMapper
import com.nunchuk.android.model.Result
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivitySignerInfoBinding
import com.nunchuk.android.usecase.DeleteRemoteSignerUseCase
import com.nunchuk.android.usecase.UpdateRemoteSignerUseCase
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SignerInfoActivity : BaseActivity() {

    // TODO add ViewModel
    @Inject
    lateinit var deleteRemoteSignerUseCase: DeleteRemoteSignerUseCase

    @Inject
    lateinit var updateRemoteSignerUseCase: UpdateRemoteSignerUseCase

    private lateinit var binding: ActivitySignerInfoBinding

    private val args: SignerInfoArgs by lazy { SignerInfoArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignerInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        val signer = SignerMapper.toSigner(args.signerSpec)
        binding.signerName.text = args.signerName
        binding.signerSpec.text = args.signerSpec
        binding.btnDone.setOnClickListener { finish() }
        binding.btnRemove.setOnClickListener { handleRemoveSigner(signer) }
        binding.signerName.setOnClickListener { onEditClicked() }
        if (args.justAdded) {
            NCToastMessage(this).show(R.string.nc_text_add_signer_success)
        }
    }

    private fun onEditClicked() {
        val bottomSheet = UpdateSignerBottomSheet.show(
            fragmentManager = supportFragmentManager,
            signerName = binding.signerName.text.toString()
        )
        bottomSheet.setListener(::onEditCompleted)
    }

    private fun onEditCompleted(updateSignerName: String) {
        if (updateSignerName.isNotEmpty() && updateSignerName != args.signerName) {
            GlobalScope.launch {
                val result = updateRemoteSignerUseCase.execute(SignerMapper.toSingleSigner(updateSignerName, args.signerSpec))
                if (result is Result.Success) {
                    binding.signerName.text = updateSignerName
                    showEditSignerNameSuccess()
                }
            }
        }
    }

    private fun showEditSignerNameSuccess() {
        binding.signerName.post { NCToastMessage(this).show(R.string.nc_text_change_signer_success) }
    }

    private fun handleRemoveSigner(signer: SignerInput) {
        GlobalScope.launch {
            deleteRemoteSignerUseCase.execute(masterFingerprint = signer.fingerPrint, derivationPath = signer.path)
        }
        finish()
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