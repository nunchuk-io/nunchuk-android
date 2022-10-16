package com.nunchuk.android.signer.software.components.primarykey.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.network.NetworkVerifier
import com.nunchuk.android.core.signer.PrimaryKeyFlow
import com.nunchuk.android.signer.software.databinding.ActivityPkeySignUpIntroBinding
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.util.setLightStatusBar
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PKeySignUpIntroActivity : BaseActivity<ActivityPkeySignUpIntroBinding>() {

    @Inject
    lateinit var networkVerifier: NetworkVerifier

    override fun initializeBinding() = ActivityPkeySignUpIntroBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()
        setupViews()
    }

    private fun setupViews() {
        binding.btnGotIt.setOnDebounceClickListener {
            if (networkVerifier.isConnected()) {
                navigator.openAddPrimaryKeyScreen(
                    this,
                    primaryKeyFlow = PrimaryKeyFlow.SIGN_UP
                )
            } else {
                NCInfoDialog(this).init(
                    title = getString(R.string.nc_device_offline),
                    message = getString(R.string.nc_device_offline_primary_key_desc)
                ).show()
            }
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
                    PKeySignUpIntroActivity::class.java
                )
            )
        }
    }
}