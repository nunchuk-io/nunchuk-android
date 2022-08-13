package com.nunchuk.android.signer.software.components.passphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivitySetPassphraseBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetPassphraseActivity : BaseActivity<ActivitySetPassphraseBinding>() {
    override fun initializeBinding() = ActivitySetPassphraseBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, SetPassphraseFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    companion object {
        fun start(activityContext: Context, mnemonic: String, signerName: String) {
            activityContext.startActivity(
                Intent(activityContext, SetPassphraseActivity::class.java).putExtras(SetPassphraseFragmentArgs(
                    mnemonic = mnemonic,
                    signerName = signerName
                ).toBundle())
            )
        }
    }

}