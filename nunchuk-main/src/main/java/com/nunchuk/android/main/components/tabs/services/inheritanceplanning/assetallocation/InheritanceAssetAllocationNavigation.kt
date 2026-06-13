package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceAssetAllocationRoute(val isUpdateRequest: Boolean = false)

fun NavGraphBuilder.inheritanceAssetAllocation(
    onBackClicked: () -> Unit,
    onContinueClicked: (List<InheritanceBeneficiaryAllocation>, Boolean) -> Unit,
) {
    composable<InheritanceAssetAllocationRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val viewModel = hiltViewModel<InheritanceAssetAllocationViewModel>()
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceAssetAllocationRoute>()
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val setupOrReviewParam = activityViewModel.setupOrReviewParam
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(viewModel, lifecycleOwner) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceAssetAllocationEvent.Loading ->
                            activity.showOrHideLoading(event.loading)

                        is InheritanceAssetAllocationEvent.Error ->
                            NCToastMessage(activity).showError(event.message)

                        is InheritanceAssetAllocationEvent.AssociateMagicSuccess ->
                            onContinueClicked(event.allocations, route.isUpdateRequest)
                    }
                }
        }

        InheritanceAssetAllocationScreen(
            remainTime = remainTime,
            initialBeneficiaries = setupOrReviewParam.beneficiaryAllocations,
            onBackClicked = onBackClicked,
            onContinueClicked = { allocations ->
                if (route.isUpdateRequest) {
                    // Re-associate magic so the edited/deleted email reflects an
                    // up-to-date magic phrase before returning to the review screen.
                    viewModel.associateMagic(
                        walletId = setupOrReviewParam.walletId,
                        groupId = setupOrReviewParam.groupId,
                        allocations = allocations,
                    )
                } else {
                    onContinueClicked(allocations, route.isUpdateRequest)
                }
            },
        )
    }
}

fun NavController.navigateToInheritanceAssetAllocation(isUpdateRequest: Boolean = false) {
    navigate(InheritanceAssetAllocationRoute(isUpdateRequest = isUpdateRequest))
}
