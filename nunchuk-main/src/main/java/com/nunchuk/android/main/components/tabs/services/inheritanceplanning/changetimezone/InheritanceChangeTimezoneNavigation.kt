package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.changetimezone

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
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceChangeTimezoneRoute(
    val isUpdateRequest: Boolean = false,
)

internal fun NavGraphBuilder.inheritanceChangeTimezone(
    onBackClicked: () -> Unit,
    onSaveClicked: (isUpdateRequest: Boolean, selectedZoneId: String) -> Unit,
) {
    composable<InheritanceChangeTimezoneRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<InheritanceChangeTimezoneRoute>()
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        InheritanceChangeTimezoneScreen(
            remainTime = remainTime,
            selectedZoneId = activityViewModel.setupOrReviewParam.selectedZoneId,
            isUpdateRequest = route.isUpdateRequest,
            onBackClicked = onBackClicked,
            onSaveClicked = { onSaveClicked(route.isUpdateRequest, it) },
        )
    }
}

fun NavController.navigateToInheritanceChangeTimezone(isUpdateRequest: Boolean = false) {
    navigate(InheritanceChangeTimezoneRoute(isUpdateRequest = isUpdateRequest))
}

