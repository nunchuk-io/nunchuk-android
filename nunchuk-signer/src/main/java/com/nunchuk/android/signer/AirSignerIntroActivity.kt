package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.databinding.ActivityBeforeAddAirSignerBinding
import com.nunchuk.android.widget.util.setLightStatusBar

class AirSignerIntroActivity : BaseActivity<ActivityBeforeAddAirSignerBinding>() {

    override fun initializeBinding() = ActivityBeforeAddAirSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnContinue.setOnClickListener { navigator.openAddAirSignerScreen(this) }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AirSignerIntroActivity::class.java))
        }
    }

}