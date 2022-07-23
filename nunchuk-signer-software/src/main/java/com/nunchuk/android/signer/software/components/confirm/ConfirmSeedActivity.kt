package com.nunchuk.android.signer.software.components.confirm

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityConfirmSeedBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmSeedActivity : BaseActivity<ActivityConfirmSeedBinding>() {

    override fun initializeBinding(): ActivityConfirmSeedBinding {
        return ActivityConfirmSeedBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, ConfirmSeedFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }

    companion object {

        fun start(activityContext: Context, mnemonic: String) {
            activityContext.startActivity(ConfirmSeedArgs(mnemonic = mnemonic).buildIntent(activityContext))
        }
    }

}