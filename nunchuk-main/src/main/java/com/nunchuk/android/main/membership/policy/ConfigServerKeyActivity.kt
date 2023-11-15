package com.nunchuk.android.main.membership.policy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfigServerKeyActivity : BaseActivity<ActivityNavigationBinding>() {
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
        val graph = inflater.inflate(R.navigation.config_server_key_navigation)
        when (intent.serializable<MembershipStage>(MembershipActivity.EXTRA_GROUP_STEP)) {
            MembershipStage.CONFIG_SERVER_KEY -> if (groupId.isNotEmpty()) {
                graph.setStartDestination(R.id.configureByzantineServerKeySettingFragment)
            } else {
                graph.setStartDestination(R.id.configureServerKeySettingFragment)
            }
            MembershipStage.CONFIG_SPENDING_LIMIT -> if (groupId.isNotEmpty()) {
                graph.setStartDestination(R.id.configByzantineSpendingLimitFragment)
            } else {
                graph.setStartDestination(R.id.configSpendingLimitFragment)
            }
            else -> graph.setStartDestination(R.id.configureServerKeyIntroFragment)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    val groupId: String
        by lazy(LazyThreadSafetyMode.NONE) {  intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }

    companion object {
        private const val EXTRA_KEY_POLICY = "key_policy"
        private const val EXTRA_KEY_XFP = "xfp"
        private const val EXTRA_GROUP_STEP = "group_step"
        private const val EXTRA_GROUP_ID = "group_id"

        fun buildIntent(
            activity: Activity,
            groupStep: MembershipStage,
            keyPolicy: KeyPolicy? = null,
            xfp: String? = null
        ) = Intent(activity, ConfigServerKeyActivity::class.java).apply {
            putExtra(EXTRA_KEY_POLICY, keyPolicy)
            putExtra(EXTRA_KEY_XFP, xfp)
            putExtra(EXTRA_GROUP_STEP, groupStep)
        }

        fun buildGroupIntent(
            activity: Activity,
            groupStep: MembershipStage,
            groupId: String?,
            keyPolicy: GroupKeyPolicy? = null,
            xfp: String? = null
        ) = Intent(activity, ConfigServerKeyActivity::class.java).apply {
            putExtra(EXTRA_KEY_POLICY, keyPolicy)
            putExtra(EXTRA_KEY_XFP, xfp)
            putExtra(EXTRA_GROUP_STEP, groupStep)
            putExtra(EXTRA_GROUP_ID, groupId)
        }
    }
}