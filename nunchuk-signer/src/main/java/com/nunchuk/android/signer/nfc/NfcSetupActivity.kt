package com.nunchuk.android.signer.nfc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel.Companion.EXTRA_MASTER_SIGNER_ID
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.ActivityNfcSetupBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcSetupActivity : BaseNfcActivity<ActivityNfcSetupBinding>() {
    override fun initializeBinding(): ActivityNfcSetupBinding =
        ActivityNfcSetupBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nfc_setup_navigation)
        val startDestinationId = when (setUpAction) {
            SETUP_NFC, SETUP_SATSCARD -> R.id.setupChainCodeFragment
            CHANGE_CVC -> R.id.changeNfcCvcFragment
            RECOVER_NFC -> R.id.recoverNfcKeyGuideFragment
            else -> R.id.addNfcNameFragment
        }
        graph.setStartDestination(startDestinationId)
        navHostFragment.navController.graph = graph
    }

    val setUpAction: Int
            by lazy(LazyThreadSafetyMode.NONE) { intent.getIntExtra(EXTRA_ACTION, SETUP_NFC) }

    companion object {
        private const val EXTRA_ACTION = "EXTRA_ACTION"
        const val SETUP_NFC = 1
        const val ADD_KEY = 2
        const val CHANGE_CVC = 3
        const val RECOVER_NFC = 4
        const val SETUP_SATSCARD = 5

        fun navigate(activity: Activity, setUpAction: Int, masterSignerId: String? = null) {
            activity.startActivity(Intent(activity, NfcSetupActivity::class.java).apply {
                putExtra(EXTRA_ACTION, setUpAction)
                putExtra(EXTRA_MASTER_SIGNER_ID, masterSignerId)
            })
        }
    }
}