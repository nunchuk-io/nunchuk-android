package com.nunchuk.android.signer.ui.nfc

import android.app.Activity
import android.content.Intent
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivityNfcSetupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcSetupActivity : BaseActivity<ActivityNfcSetupBinding>() {
    override fun initializeBinding(): ActivityNfcSetupBinding =
        ActivityNfcSetupBinding.inflate(layoutInflater)

    companion object {
        fun navigate(activity: Activity) {
            activity.startActivity(Intent(activity, NfcSetupActivity::class.java))
        }
    }
}