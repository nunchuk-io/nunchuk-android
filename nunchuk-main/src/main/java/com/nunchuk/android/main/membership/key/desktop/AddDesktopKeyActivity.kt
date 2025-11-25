package com.nunchuk.android.main.membership.key.desktop

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddDesktopKeyActivity : BaseActivity<ActivityNavigationBinding>() {
    private val viewModel: AddDesktopKeyViewModel by viewModels()
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater).also {
            enableEdgeToEdge()
        }
    }

    val magic by lazy {
        intent.getStringExtra(EXTRA_MAGIC).orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.extras == null) {
            finish()
            return
        }
        if (savedInstanceState == null) {
            viewModel.deleteClaimingRequests()
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.add_desktop_key_navigation)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {

        private const val EXTRA_SIGNER_TAG = "signer_tag"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_MEMBERSHIP_STEP = "step"
        private const val EXTRA_INHERITANCE_KEY = "is_add_inheritance_key"
        private const val EXTRA_MAGIC = "magic"

        fun navigate(
            launcher: ActivityResultLauncher<Intent>? = null,
            activity: Activity,
            signerTag: SignerTag,
            groupId: String?,
            step: MembershipStep,
            isAddInheritanceKey: Boolean = false,
            magic: String = ""
        ) {
            val intent = Intent(activity, AddDesktopKeyActivity::class.java)
                .putExtra(EXTRA_SIGNER_TAG, signerTag)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .putExtra(EXTRA_MEMBERSHIP_STEP, step)
                .putExtra(EXTRA_INHERITANCE_KEY, isAddInheritanceKey)
                .putExtra(EXTRA_MAGIC, magic)
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                activity.startActivity(intent)
            }
        }
    }
}