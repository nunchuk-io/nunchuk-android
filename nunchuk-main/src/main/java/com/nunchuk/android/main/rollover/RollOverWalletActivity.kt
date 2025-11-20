package com.nunchuk.android.main.rollover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Amount
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RollOverWalletActivity : BaseActivity<ActivityNavigationBinding>() {

    @Inject
    internal lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: RollOverWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.rollover_wallet_navigation)


        val startScreen = intent.getIntExtra(START_SCREEN, RollOverWalletFlow.NONE)
        when (startScreen) {
            RollOverWalletFlow.REFUND -> graph.setStartDestination(R.id.rollOverTransferFundFragment)
            RollOverWalletFlow.PREVIEW -> graph.setStartDestination(R.id.rollOverPreviewFragment)
        }

        navHostFragment.navController.setGraph(graph, intent.extras)

        viewModel.init(
            oldWalletId = intent.getStringExtra(OLD_WALLET_ID).orEmpty(),
            newWalletId = intent.getStringExtra(NEW_WALLET_ID).orEmpty(),
            selectedTagIds = intent.getIntegerArrayListExtra(SELECT_TAG_IDS).orEmpty(),
            selectedCollectionIds = intent.getIntegerArrayListExtra(SELECT_COLLECTION_IDS).orEmpty(),
            feeRate = intent.parcelable<Amount>(FEE_RATE) ?: Amount.ZER0,
            source = intent.getIntExtra(SOURCE, RollOverWalletSource.WALLET_CONFIG),
            antiFeeSniping = intent.getBooleanExtra(ANTI_FEE_SNIPING, false)
        )

        flowObserver(viewModel.event) { event ->
            when (event) {
                is RollOverWalletEvent.Error -> NCToastMessage(this).showError(event.message)
                is RollOverWalletEvent.Loading -> showOrHideLoading(event.isLoading)
                RollOverWalletEvent.Success -> {
                    val source = intent.getIntExtra(SOURCE, RollOverWalletSource.WALLET_CONFIG)
                    if (source == RollOverWalletSource.REPLACE_KEY) {
                        navigator.returnToMainScreen(this)
                        navigator.openWalletDetailsScreen(this, intent.getStringExtra(OLD_WALLET_ID).orEmpty())
                    } else {
                        navigator.returnToMainScreen(this)
                    }
                    NcToastManager.scheduleShowMessage(message = "Please sign the rollover transactions at your convenience.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAllTagsAndCollections()
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        private const val OLD_WALLET_ID = "old_wallet_id"
        private const val NEW_WALLET_ID = "new_wallet_id"
        private const val START_SCREEN = "start_screen"
        private const val SELECT_TAG_IDS = "select_tag_ids"
        private const val SELECT_COLLECTION_IDS = "select_collection_ids"
        private const val FEE_RATE = "fee_rate"
        private const val SOURCE = "source"
        private const val ANTI_FEE_SNIPING = "anti_fee_sniping"

        fun navigate(
            activity: Context,
            @RollOverWalletFlow.RollOverWalletFlowInfo startScreen: Int,
            oldWalletId: String,
            newWalletId: String,
            selectedTagIds: List<Int>,
            selectedCollectionIds: List<Int>,
            feeRate: Amount,
            source: Int,
            antiFeeSniping: Boolean,
        ) {
            val intent = Intent(activity, RollOverWalletActivity::class.java)
                .apply {
                    putExtra(OLD_WALLET_ID, oldWalletId)
                    putExtra(NEW_WALLET_ID, newWalletId)
                    putExtra(START_SCREEN, startScreen)
                    putIntegerArrayListExtra(SELECT_TAG_IDS, ArrayList(selectedTagIds))
                    putIntegerArrayListExtra(
                        SELECT_COLLECTION_IDS,
                        ArrayList(selectedCollectionIds)
                    )
                    putExtra(FEE_RATE, feeRate)
                    putExtra(SOURCE, source)
                    putExtra(ANTI_FEE_SNIPING, antiFeeSniping)
                }
            activity.startActivity(intent)
        }
    }
}