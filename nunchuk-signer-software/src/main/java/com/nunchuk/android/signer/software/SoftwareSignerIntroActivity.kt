package com.nunchuk.android.signer.software

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.software.databinding.ActivitySoftwareSignerIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SoftwareSignerIntroActivity : BaseActivity<ActivitySoftwareSignerIntroBinding>() {

    override fun initializeBinding() = ActivitySoftwareSignerIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnCreateSeed.setOnClickListener { openCreateNewSeedScreen() }
        binding.btnRecoverSeed.setOnClickListener { openRecoverSeedScreen() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun openCreateNewSeedScreen() {
        navigator.openCreateNewSeedScreen(this)
    }

    private fun openRecoverSeedScreen() {
        navigator.openRecoverSeedScreen(this)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SoftwareSignerIntroActivity::class.java))
        }
    }
}