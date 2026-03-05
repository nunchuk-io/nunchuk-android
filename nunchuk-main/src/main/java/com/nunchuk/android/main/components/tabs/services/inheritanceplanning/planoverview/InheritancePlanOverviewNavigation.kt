package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.planoverview

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class InheritancePlanOverviewRoute(val setupFlowType: InheritanceSetupFlowType = InheritanceSetupFlowType.OLD_FLOW)

fun NavGraphBuilder.inheritancePlanOverview(
    onContinueClicked: (InheritanceSetupFlowType) -> Unit,
) {
    composable<InheritancePlanOverviewRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritancePlanOverviewRoute>()
        val viewModel = hiltViewModel<InheritancePlanOverviewViewModel>()
        val sharedState by activityViewModel.state.collectAsStateWithLifecycle()
        InheritancePlanOverviewScreen(
            viewModel = viewModel,
            groupWalletType = activityViewModel.getGroupWalletType(),
            isMiniscriptWallet = sharedState.isMiniscriptWallet,
            setupFlowType = route.setupFlowType,
            onContinueClicked = { onContinueClicked(route.setupFlowType) },
        )
    }
}

fun NavController.navigateToInheritancePlanOverview(
    setupFlowType: InheritanceSetupFlowType = InheritanceSetupFlowType.OLD_FLOW,
) {
    navigate(InheritancePlanOverviewRoute(setupFlowType = setupFlowType))
}
