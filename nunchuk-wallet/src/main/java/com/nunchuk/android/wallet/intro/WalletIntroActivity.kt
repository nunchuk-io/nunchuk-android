package com.nunchuk.android.wallet.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.databinding.ActivityWalletIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar

class WalletIntroActivity : BaseActivity() {

    private lateinit var binding: ActivityWalletIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityWalletIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

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