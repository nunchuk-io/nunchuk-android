package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.note

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
data class InheritanceNoteRoute(val isUpdateRequest: Boolean = false)

fun NavGraphBuilder.inheritanceNote(
    onContinueClick: (isUpdateRequest: Boolean, note: String) -> Unit,
) {
    composable<InheritanceNoteRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceNoteRoute>()
        val viewModel = hiltViewModel<InheritanceNoteViewModel>()
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(Unit) {
            viewModel.init(
                param = activityViewModel.setupOrReviewParam,
                isUpdateRequest = route.isUpdateRequest,
            )
        }

        LaunchedEffect(viewModel, lifecycleOwner, route) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceNoteEvent.ContinueClick -> {
                            onContinueClick(route.isUpdateRequest, event.note)
                        }
                    }
                }
        }

        InheritanceNoteScreen(
            viewModel = viewModel,
            isUpdateRequest = route.isUpdateRequest,
            inheritanceViewModel = activityViewModel,
        )
    }
}

fun NavController.navigateToInheritanceNote(isUpdateRequest: Boolean = false) {
    navigate(InheritanceNoteRoute(isUpdateRequest = isUpdateRequest))
}
