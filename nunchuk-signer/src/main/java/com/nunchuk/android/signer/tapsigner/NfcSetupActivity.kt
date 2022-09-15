package com.nunchuk.android.signer.tapsigner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcViewModel.Companion.EXTRA_MASTER_SIGNER_ID
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.verify.TapSignerVerifyBackUpOptionFragmentArgs
import com.nunchuk.android.signer.tapsigner.id.TapSignerIdFragmentArgs
import com.nunchuk.android.signer.tapsigner.intro.AddTapSignerIntroFragmentArgs
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcSetupActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding =
        ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nfc_setup_navigation)
        val startDestinationId = when (setUpAction) {
            SETUP_TAP_SIGNER -> R.id.addTapSignerIntroFragment
            SETUP_SATSCARD -> R.id.setupChainCodeFragment
            RECOVER_NFC -> R.id.recoverNfcKeyGuideFragment
            VERIFY_TAP_SIGNER -> R.id.tapSignerVerifyBackUpOptionFragment
            CREATE_BACK_UP_KEY -> R.id.tapSignerIdFragment
            else -> R.id.addNfcNameFragment
        }
        graph.setStartDestination(startDestinationId)
        val extras = when (setUpAction) {
            SETUP_TAP_SIGNER -> AddTapSignerIntroFragmentArgs(fromMembershipFlow).toBundle()
            VERIFY_TAP_SIGNER -> TapSignerVerifyBackUpOptionFragmentArgs(
                masterSignerId = intent.getStringExtra(EXTRA_MASTER_SIGNER_ID).orEmpty(),
                filePath = intent.getStringExtra(EXTRA_BACKUP_FILE_PATH).orEmpty()
            ).toBundle()
            CREATE_BACK_UP_KEY -> TapSignerIdFragmentArgs(
                intent.getStringExtra(EXTRA_MASTER_SIGNER_ID).orEmpty()
            ).toBundle()
            else -> intent.extras
        }
        navHostFragment.navController.setGraph(graph, extras)
    }

    val setUpAction: Int
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getIntExtra(
                    EXTRA_ACTION,
                    SETUP_TAP_SIGNER
                )
            }

    val fromMembershipFlow: Boolean
            by lazy(LazyThreadSafetyMode.NONE) {
                intent.getBooleanExtra(
                    EXTRA_FROM_MEMBERSHIP_FLOW,
                    false
                )
            }

    val hasWallet: Boolean
            by lazy(LazyThreadSafetyMode.NONE) { intent.getBooleanExtra(EXTRA_HAS_WALLET, false) }

    companion object {
        private const val EXTRA_ACTION = "EXTRA_ACTION"
        private const val EXTRA_HAS_WALLET = "EXTRA_HAS_WALLET"
        private const val EXTRA_FROM_MEMBERSHIP_FLOW = "isMembershipFlow"
        private const val EXTRA_BACKUP_FILE_PATH = "EXTRA_BACKUP_FILE_PATH"

        /**
         * Setup action
         */
        const val SETUP_TAP_SIGNER = 1
        const val ADD_KEY = 2
        const val CHANGE_CVC = 3
        const val RECOVER_NFC = 4
        const val SETUP_SATSCARD = 5
        const val VERIFY_TAP_SIGNER = 6
        const val CREATE_BACK_UP_KEY = 7

        /**
         * @param masterSignerId need to setup satscard
         * @param hasWallet need to setup satscard
         */
        fun navigate(
            activity: Activity,
            setUpAction: Int,
            fromMembershipFlow: Boolean = false,
            masterSignerId: String? = null,
            backUpFilePath: String? = null,
            hasWallet: Boolean = false
        ) {
            activity.startActivity(
                buildIntent(
                    activity = activity,
                    setUpAction = setUpAction,
                    fromMembershipFlow = fromMembershipFlow,
                    masterSignerId = masterSignerId,
                    backUpFilePath = backUpFilePath,
                    hasWallet = hasWallet,
                )
            )
        }

        fun buildIntent(
            activity: Activity,
            setUpAction: Int,
            fromMembershipFlow: Boolean = false,
            masterSignerId: String? = null,
            backUpFilePath: String? = null,
            hasWallet: Boolean = false
        ) = Intent(activity, NfcSetupActivity::class.java).apply {
            putExtra(EXTRA_ACTION, setUpAction)
            putExtra(EXTRA_MASTER_SIGNER_ID, masterSignerId)
            putExtra(EXTRA_HAS_WALLET, hasWallet)
            putExtra(EXTRA_FROM_MEMBERSHIP_FLOW, fromMembershipFlow)
            putExtra(EXTRA_BACKUP_FILE_PATH, backUpFilePath)
        }
    }
}