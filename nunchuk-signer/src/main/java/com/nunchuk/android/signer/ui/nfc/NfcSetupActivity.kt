package com.nunchuk.android.signer.ui.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityNfcSetupBinding

class NfcSetupActivity : BaseActivity<ActivityNfcSetupBinding>() {
    override fun initializeBinding(): ActivityNfcSetupBinding =
        ActivityNfcSetupBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDestination()
    }

    private fun initDestination() {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) ?: return
        val inflater = navHost.findNavController().navInflater
        val graph = inflater.inflate(R.navigation.nfc_setup_navigation)
        val shouldOpenTurnOnNfc = intent.getBooleanExtra(EXTRA_OPEN_TURN_ON_NFC_SCREEN, false)
        if (shouldOpenTurnOnNfc) {
            graph.startDestination = R.id.turnOnNfcFragment
        } else {
            graph.startDestination = R.id.changeNfcCvcFragment
        }
        navHost.findNavController().graph = graph
    }

    companion object {
        private const val EXTRA_OPEN_TURN_ON_NFC_SCREEN = "EXTRA_OPEN_TURN_ON_NFC_SCREEN"
        fun navigate(activity: Activity, shouldOpenTurnOnNfc: Boolean = false) {
            activity.startActivity(Intent(activity, NfcSetupActivity::class.java).apply {
                putExtra(EXTRA_OPEN_TURN_ON_NFC_SCREEN, shouldOpenTurnOnNfc)
            })
        }
    }
}