package com.nunchuk.android.wallet.personal.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.personal.R
import com.nunchuk.android.wallet.personal.databinding.ActivityWalletIntermediaryBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletIntermediaryActivity : BaseActivity<ActivityWalletIntermediaryBinding>() {
    override fun initializeBinding(): ActivityWalletIntermediaryBinding {
        return ActivityWalletIntermediaryBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, WalletIntermediaryFragment().apply {
                    arguments = intent.extras
                })
            }
        }
    }
    companion object {
        const val REQUEST_CODE = 1111
        const val REQUEST_PERMISSION_CAMERA = 1112

        const val EXTRA_HAS_SIGNER = "EXTRA_HAS_SIGNER"
        fun start(activityContext: Context, hasSigner: Boolean) {
            val intent = Intent(activityContext, WalletIntermediaryActivity::class.java).apply {
                putExtra(EXTRA_HAS_SIGNER, hasSigner)
            }
            activityContext.startActivity(intent)
        }
    }

}