package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceNotifyPrefRoute(val isUpdateRequest: Boolean = false)

fun NavGraphBuilder.inheritanceNotifyPref(
    onSkipClick: (isUpdateRequest: Boolean) -> Unit,
    onContinueClick: (isUpdateRequest: Boolean, emails: List<String>, isNotify: Boolean) -> Unit,
) {
    composable<InheritanceNotifyPrefRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceNotifyPrefRoute>()
        val viewModel = hiltViewModel<InheritanceNotifyPrefViewModel>()

        LaunchedEffect(Unit) {
            viewModel.init(
                param = activityViewModel.setupOrReviewParam,
                isUpdateRequest = route.isUpdateRequest,
            )
        }

        InheritanceNotifyPrefScreen(
            viewModel = viewModel,
            isUpdateRequest = route.isUpdateRequest,
            inheritanceViewModel = activityViewModel,
            onSkipClick = { onSkipClick(route.isUpdateRequest) },
            onContinueClick = { emails, isNotify ->
                onContinueClick(route.isUpdateRequest, emails, isNotify)
            },
        )
    }
}

fun NavController.navigateToInheritanceNotifyPref(isUpdateRequest: Boolean = false) {
    navigate(InheritanceNotifyPrefRoute(isUpdateRequest = isUpdateRequest))
}
