package com.nunchuk.android.main.membership

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.server.setting.ConfigureServerKeySettingFragmentArgs
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MembershipActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.membership_navigation)
        val stage = intent.serializable<MembershipStage>(EXTRA_GROUP_STEP)
        when (stage) {
            MembershipStage.NONE -> graph.setStartDestination(R.id.introAssistedWalletFragment)
            MembershipStage.CONFIG_USER_KEYS_IN_PROGRESS -> graph.setStartDestination(R.id.addKeyListFragment)
            MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS -> graph.setStartDestination(
                R.id.addKeyStepFragment
            )
            MembershipStage.CONFIG_SERVER_KEY -> graph.setStartDestination(R.id.configureServerKeySettingFragment)
            else -> Unit
        }
        val bundle = when (stage) {
            MembershipStage.CONFIG_SERVER_KEY -> ConfigureServerKeySettingFragmentArgs(
                keyPolicy = intent.parcelable(EXTRA_KEY_POLICY),
                xfp = intent.getStringExtra(EXTRA_KEY_XFP)
            ).toBundle()
            else -> null
        }
        navHostFragment.navController.setGraph(graph, bundle)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {
        private const val EXTRA_GROUP_STEP = "group_step"
        private const val EXTRA_KEY_POLICY = "key_policy"
        private const val EXTRA_KEY_XFP = "key_xfp"

        fun buildIntent(
            activity: Activity,
            groupStep: MembershipStage,
            keyPolicy: KeyPolicy? = null,
            xfp: String? = null
        ) = Intent(activity, MembershipActivity::class.java).apply {
            putExtra(EXTRA_GROUP_STEP, groupStep)
            putExtra(EXTRA_KEY_POLICY, keyPolicy)
            putExtra(EXTRA_KEY_XFP, xfp)
        }
    }
}