package com.nunchuk.android.signer.software.components.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.R
import com.nunchuk.android.signer.software.databinding.ActivityCreateSeedBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateNewSeedActivity : BaseActivity<ActivityCreateSeedBinding>() {
    override fun initializeBinding(): ActivityCreateSeedBinding {
        return ActivityCreateSeedBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, CreateNewSeedFragment().apply {
                    arguments = CreateNewSeedFragmentArgs(false).toBundle()
                })
            }
        }
    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, CreateNewSeedActivity::class.java))
        }
    }
}