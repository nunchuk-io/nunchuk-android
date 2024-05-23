package com.nunchuk.android.transaction.components.address

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.SavedAddressFlow
import com.nunchuk.android.model.SavedAddress
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.transaction.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SavedAddressActivity : BaseActivity<ActivityNavigationBinding>() {

    @Inject
    internal lateinit var membershipStepManager: MembershipStepManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.navigation_saved_address)
        val flow = intent.getIntExtra(EXTRA_SAVED_ADDRESS_FLOW, InheritancePlanFlow.NONE)

        when (flow) {
            SavedAddressFlow.LIST -> graph.setStartDestination(R.id.savedAddressListFragment)
            SavedAddressFlow.CREATE,
            SavedAddressFlow.EDIT -> graph.setStartDestination(R.id.addOrEditAddressFragment)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        private const val EXTRA_SAVED_ADDRESS_FLOW = "flow"
        private const val EXTRA_SELECT_ADDRESS = "address"

        fun navigate(
            launcher: ActivityResultLauncher<Intent>? = null,
            activity: Context,
            @SavedAddressFlow.SavedAddressFlowInfo flow: Int,
            address: SavedAddress? = null,
        ) {
            val intent = Intent(activity, SavedAddressActivity::class.java)
                .putExtra(EXTRA_SAVED_ADDRESS_FLOW, flow)
                .putExtra(EXTRA_SELECT_ADDRESS, address)
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                activity.startActivity(intent)
            }
        }
    }
}