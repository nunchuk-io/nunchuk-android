package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.assetallocation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceBeneficiaryAllocation
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceAssetAllocationRoute

fun NavGraphBuilder.inheritanceAssetAllocation(
    onBackClicked: () -> Unit,
    onContinueClicked: (List<InheritanceBeneficiaryAllocation>) -> Unit,
) {
    composable<InheritanceAssetAllocationRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val setupOrReviewParam = activityViewModel.setupOrReviewParam
        InheritanceAssetAllocationScreen(
            remainTime = remainTime,
            initialBeneficiaries = setupOrReviewParam.beneficiaryAllocations,
            onBackClicked = onBackClicked,
            onContinueClicked = onContinueClicked,
        )
    }
}

fun NavController.navigateToInheritanceAssetAllocation() {
    navigate(InheritanceAssetAllocationRoute)
}
