package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecretinfo

import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardActivity
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCInfoDialog
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceShareSecretInfoRoute(
    val sourceFlow: Int = InheritanceSourceFlow.NONE,
    val magicalPhrase: String = "",
    val type: Int = 0,
    val planFlow: Int = InheritancePlanFlow.NONE,
    val walletId: String = "",
)

fun NavGraphBuilder.inheritanceShareSecretInfo(
    navigator: NunchukNavigator,
    onNavigateToHowItWorks: (type: Int) -> Unit,
    onNavigateToBackUpDownload: () -> Unit,
) {
    composable<InheritanceShareSecretInfoRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceShareSecretInfoRoute>()
        val viewModel = hiltViewModel<InheritanceShareSecretInfoViewModel>()
        InheritanceShareSecretInfoScreen(
            viewModel = viewModel,
            sharedViewModel = activityViewModel,
            type = route.type,
            magicalPhrase = route.magicalPhrase,
            planFlow = route.planFlow,
            onContinue = {
                if (activityViewModel.isMiniscriptWallet()) {
                    onNavigateToHowItWorks(route.type)
                } else if (route.planFlow == InheritancePlanFlow.SETUP) {
                    NCInfoDialog(activity).showDialog(
                        message = activity.getString(R.string.nc_inheritance_share_secret_info_dialog_desc),
                        onYesClick = {
                            handleInheritanceShareSecretBack(
                                activity = activity,
                                sourceFlow = route.sourceFlow,
                                planFlow = route.planFlow,
                                walletId = route.walletId,
                                navigator = navigator,
                            )
                        },
                    )
                } else {
                    handleInheritanceShareSecretBack(
                        activity = activity,
                        sourceFlow = route.sourceFlow,
                        planFlow = route.planFlow,
                        walletId = route.walletId,
                        navigator = navigator,
                    )
                }
            },
            onLearnMoreClicked = onNavigateToBackUpDownload,
            onSaveBsms = {
                activity.showSaveShareOption()
            },
        )
    }
}

fun NavController.navigateToInheritanceShareSecretInfo(
    sourceFlow: Int = InheritanceSourceFlow.NONE,
    magicalPhrase: String = "",
    type: Int = 0,
    planFlow: Int = InheritancePlanFlow.NONE,
    walletId: String = "",
) {
    navigate(
        InheritanceShareSecretInfoRoute(
            sourceFlow = sourceFlow,
            magicalPhrase = magicalPhrase,
            type = type,
            planFlow = planFlow,
            walletId = walletId,
        )
    )
}

private fun handleInheritanceShareSecretBack(
    activity: InheritancePlanningActivity,
    sourceFlow: Int,
    planFlow: Int,
    walletId: String,
    navigator: NunchukNavigator,
) {
    when (sourceFlow) {
        InheritanceSourceFlow.GROUP_DASHBOARD -> {
            ActivityManager.popUntil(GroupDashboardActivity::class.java)
        }
        InheritanceSourceFlow.SERVICE_TAB -> activity.finish()
        else -> {
            ActivityManager.popUntilRoot()
            if (planFlow == InheritancePlanFlow.SETUP && sourceFlow == InheritanceSourceFlow.WIZARD) {
                navigator.openWalletDetailsScreen(activity, walletId)
            }
        }
    }
}
