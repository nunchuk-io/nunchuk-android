package com.nunchuk.android.signer.software.components.primarykey.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.databinding.ActivityPkeySignInIntroBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PKeySignInIntroActivity : BaseActivity<ActivityPkeySignInIntroBinding>() {

    override fun initializeBinding() = ActivityPkeySignInIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnManually.setOnDebounceClickListener {
            navigator.openPrimaryKeyManuallyUsernameScreen(this)
        }
        binding.btnImport.setOnDebounceClickListener {
            navigator.openRecoverSeedScreen(this, primaryKeyFlow = PrimaryKeyFlow.SIGN_IN)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    PKeySignInIntroActivity::class.java
                )
            )
        }
    }
}