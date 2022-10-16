package com.nunchuk.android.signer.software.components.primarykey

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.databinding.ActivityPkeyAddSignerBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PKeyAddSignerActivity : BaseActivity<ActivityPkeyAddSignerBinding>() {

    override fun initializeBinding() = ActivityPkeyAddSignerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnAddSSigner.setOnClickListener { openAddSoftwareSignerScreen() }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun openAddSoftwareSignerScreen() {
        val primaryKeyFlow = intent.getIntExtra(EXTRA_PRIMARY_KEY_FLOW, PrimaryKeyFlow.NONE)
        val passphrase = intent.getStringExtra(EXTRA_PASSPHRASE).orEmpty()
        navigator.openAddSoftwareSignerScreen(
            this,
            passphrase = passphrase,
            primaryKeyFlow = primaryKeyFlow
        )
    }

    companion object {
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"

        fun start(activityContext: Context, primaryKeyFlow: Int, passphrase: String) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    PKeyAddSignerActivity::class.java
                ).apply {
                    putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
                    putExtra(EXTRA_PASSPHRASE, passphrase)
                }
            )
        }
    }
}