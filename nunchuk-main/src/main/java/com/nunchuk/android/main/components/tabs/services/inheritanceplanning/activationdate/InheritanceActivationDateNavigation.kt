package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.activationdate

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceActivationDateRoute(val isUpdateRequest: Boolean = false)

fun NavGraphBuilder.inheritanceActivationDate(
    onContinueClick: (isUpdateRequest: Boolean, date: Long, selectedZoneId: String) -> Unit,
) {
    composable<InheritanceActivationDateRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceActivationDateRoute>()
        val viewModel = hiltViewModel<InheritanceActivationDateViewModel>()
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(viewModel, lifecycleOwner, route) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceActivationDateEvent.ContinueClick -> {
                            onContinueClick(route.isUpdateRequest, event.date, event.selectedZoneId)
                        }
                    }
                }
        }

        InheritanceActivationDateScreen(
            viewModel = viewModel,
            isUpdateRequest = route.isUpdateRequest,
            inheritanceViewModel = activityViewModel,
        )
    }
}

fun NavController.navigateToInheritanceActivationDate(isUpdateRequest: Boolean = false) {
    navigate(InheritanceActivationDateRoute(isUpdateRequest = isUpdateRequest))
}
