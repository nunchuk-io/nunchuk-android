package com.nunchuk.android.signer.software

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.databinding.ActivitySoftwareSignerIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SoftwareSignerIntroActivity : BaseActivity<ActivitySoftwareSignerIntroBinding>() {

    private val primaryKeyFlow: Int by lazy {
        intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
    }
    private val passphrase: String by lazy {
        intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
    }

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
        navigator.openCreateNewSeedScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow
        )
    }

    private fun openRecoverSeedScreen() {
        navigator.openRecoverSeedScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow
        )
    }

    companion object {
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        fun start(
            activityContext: Context,
            passphrase: String,
            primaryKeyFlow: Int = PrimaryKeyFlow.NONE
        ) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    SoftwareSignerIntroActivity::class.java
                ).apply {
                    putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
                    putExtra(EXTRA_PASSPHRASE, passphrase)
                })

        }
    }
}