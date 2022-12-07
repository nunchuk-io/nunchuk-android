package com.nunchuk.android.main.membership.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navArgs
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletAuthenticationActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    private val args: WalletAuthenticationActivityArgs by navArgs()
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.check_sign_message_navigation)

        when (args.type) {
            SIGN_TEMP_MESSAGE -> graph.setStartDestination(R.id.checkSignMessageFragment)
            SIGN_DUMMY_TX -> graph.setStartDestination(R.id.dummyTransactionDetailsFragment)
            SECURITY_QUESTION -> graph.setStartDestination(R.id.answerSecurityQuestionFragment2)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        internal const val SIGN_TEMP_MESSAGE = "SIGN_MESSAGE"
        internal const val SIGN_DUMMY_TX = "SIGN_DUMMY_TX"
        internal const val SECURITY_QUESTION = "SECURITY_QUESTION"

        fun start(
            walletId: String,
            userData: String,
            requiredSignatures: Int,
            type: String,
            launcher: ActivityResultLauncher<Intent>,
            activityContext: Activity
        ) {
            launcher.launch(
                Intent(activityContext, WalletAuthenticationActivity::class.java).apply {
                    putExtras(
                        WalletAuthenticationActivityArgs(
                            walletId = walletId,
                            userData = userData,
                            requiredSignatures = requiredSignatures,
                            type = type,
                        ).toBundle()
                    )
                }
            )
        }
    }
}