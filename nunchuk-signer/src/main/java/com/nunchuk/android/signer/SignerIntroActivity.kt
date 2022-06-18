package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivitySignerIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignerIntroActivity : BaseActivity<ActivitySignerIntroBinding>() {

    override fun initializeBinding() = ActivitySignerIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnAddAirSigner.setOnClickListener { openAddAirSignerIntroScreen() }
        binding.btnAddSSigner.setOnClickListener { openAddSoftwareSignerScreen() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun openAddAirSignerIntroScreen() {
        finish()
        navigator.openAddAirSignerIntroScreen(this)
    }

    private fun openAddSoftwareSignerScreen() {
        finish()
        navigator.openAddSoftwareSignerScreen(this)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, SignerIntroActivity::class.java))
        }
    }

}