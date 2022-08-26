package com.nunchuk.android.transaction.components.details.fee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.databinding.ActivityReplaceByFeeBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReplaceFeeActivity : BaseActivity<ActivityReplaceByFeeBinding>() {

    override fun initializeBinding(): ActivityReplaceByFeeBinding {
        return ActivityReplaceByFeeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
    }

    companion object {
        fun start(launcher: ActivityResultLauncher<Intent>, context: Context, walletId: String, transaction: Transaction) {
            launcher.launch(ReplaceFeeArgs(walletId, transaction).buildIntent(context))
        }
    }
}