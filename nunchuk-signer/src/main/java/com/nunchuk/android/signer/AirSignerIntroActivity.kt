package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.databinding.ActivityBeforeAddAirSignerBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import javax.inject.Inject

class AirSignerIntroActivity : BaseActivity() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private lateinit var binding: ActivityBeforeAddAirSignerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivityBeforeAddAirSignerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnContinue.setOnClickListener { navigator.openAddAirSignerScreen(this) }
    }

    private fun openAddAirSignerScreen() {
        finish()
        navigator.openAddAirSignerScreen(this)
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, AirSignerIntroActivity::class.java))
        }
    }

}