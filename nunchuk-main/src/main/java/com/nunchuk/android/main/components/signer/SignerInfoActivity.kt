package com.nunchuk.android.main.components.signer

import android.content.Context
import android.os.Bundle
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.main.databinding.ActivitySignerInfoBinding

class SignerInfoActivity : BaseActivity() {
    private lateinit var binding: ActivitySignerInfoBinding

    private val args: SignerInfoArgs by lazy { SignerInfoArgs.deserializeFrom(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignerInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.signerName.text = args.signerName
        binding.signerSpec.text = args.signerSpec
    }

    companion object {

        fun start(activityContext: Context, signerName: String, signerSpec: String) {
            activityContext.startActivity(
                SignerInfoArgs(
                    signerName = signerName,
                    signerSpec = signerSpec
                ).buildIntent(activityContext)
            )
        }
    }

}