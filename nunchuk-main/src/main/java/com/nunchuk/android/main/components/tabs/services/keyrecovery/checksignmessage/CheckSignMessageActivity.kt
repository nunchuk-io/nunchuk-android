package com.nunchuk.android.main.components.tabs.services.keyrecovery.checksignmessage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.main.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class CheckSignMessageActivity : BaseNfcActivity<ActivityNavigationBinding>() {
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
        navHostFragment.navController.setGraph(
            R.navigation.check_sign_message_navigation,
            intent.extras
        )
    }

    companion object {
        const val SIGNATURE_EXTRA = "SIGNATURE_EXTRA"
        fun start(
            walletId: String,
            userData: String,
            requiredSignatures: Int,
            launcher: ActivityResultLauncher<Intent>,
            activityContext: Activity
        ) {
            launcher.launch(
                Intent(activityContext, CheckSignMessageActivity::class.java).apply {
                    putExtras(
                        CheckSignMessageFragmentArgs(
                            walletId = walletId,
                            userData = userData,
                            requiredSignatures = requiredSignatures
                        ).toBundle()
                    )
                }
            )
        }
    }
}