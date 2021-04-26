package com.nunchuk.android.signer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.BaseActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.signer.databinding.ActivitySoftwareSignerIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setTransparentStatusBar
import javax.inject.Inject

class SoftwareSignerIntroActivity : BaseActivity() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private lateinit var binding: ActivitySoftwareSignerIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        binding = ActivitySoftwareSignerIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

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