package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasemethod

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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.toReleaseMethodOption
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceReleaseMethodRoute(
    val isUpdateRequest: Boolean = false,
)

internal fun NavGraphBuilder.inheritanceReleaseMethod(
    onBackClicked: () -> Unit,
    onContinueClicked: (InheritanceReleaseMethod, Boolean) -> Unit,
) {
    composable<InheritanceReleaseMethodRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<InheritanceReleaseMethodRoute>()
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val setupOrReviewParam = activityViewModel.setupOrReviewParam
        InheritanceReleaseMethodScreen(
            remainTime = remainTime,
            selectedMethod = setupOrReviewParam.releaseMethodType.toReleaseMethodOption(),
            isUpdateRequest = route.isUpdateRequest,
            onBackClicked = onBackClicked,
            onContinueClicked = { onContinueClicked(it, route.isUpdateRequest) },
        )
    }
}

fun NavController.navigateToInheritanceReleaseMethod(isUpdateRequest: Boolean = false) {
    navigate(InheritanceReleaseMethodRoute(isUpdateRequest = isUpdateRequest))
}
