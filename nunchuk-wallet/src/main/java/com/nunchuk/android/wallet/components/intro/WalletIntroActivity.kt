package com.nunchuk.android.wallet.components.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.databinding.ActivityWalletIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar

class WalletIntroActivity : BaseActivity<ActivityWalletIntroBinding>() {

    override fun initializeBinding() = ActivityWalletIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnContinue.setOnClickListener { openAddSignerScreen() }
    }

    private fun openAddSignerScreen() {
        finish()
        navigator.openSignerIntroScreen(this)
    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, WalletIntroActivity::class.java))
        }
    }

}