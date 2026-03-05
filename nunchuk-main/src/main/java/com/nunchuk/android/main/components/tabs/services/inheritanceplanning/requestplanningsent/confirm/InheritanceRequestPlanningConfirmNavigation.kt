package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.requestplanningsent.confirm

import android.app.Activity
import android.content.Intent
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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceRequestPlanningConfirmRoute(
    val walletId: String,
    val groupId: String,
)

fun NavGraphBuilder.inheritanceRequestPlanningConfirm(
    onRequestSuccess: () -> Unit,
) {
    composable<InheritanceRequestPlanningConfirmRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val route = backStackEntry.toRoute<InheritanceRequestPlanningConfirmRoute>()
        val viewModel = hiltViewModel<InheritanceRequestPlanningConfirmViewModel>()
        val lifecycleOwner = LocalLifecycleOwner.current

        LaunchedEffect(viewModel, lifecycleOwner) {
            viewModel.event.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { event ->
                    when (event) {
                        is InheritanceRequestPlanningConfirmEvent.Loading -> {
                            activity.showOrHideLoading(event.isLoading)
                        }
                        is InheritanceRequestPlanningConfirmEvent.Error -> {
                            NCToastMessage(activity).show(event.message)
                        }
                        InheritanceRequestPlanningConfirmEvent.RequestInheritanceSuccess -> {
                            activity.setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra(InheritancePlanningActivity.RESULT_REQUEST_PLANNING, true)
                            })
                            onRequestSuccess()
                        }
                    }
                }
        }

        InheritanceRequestPlanningConfirmScreen(
            onCancel = { activity.finish() },
            onContinue = {
                viewModel.requestInheritancePlanning(
                    walletId = route.walletId,
                    groupId = route.groupId,
                )
            },
        )
    }
}

fun NavController.navigateToInheritanceRequestPlanningConfirm(
    walletId: String,
    groupId: String,
) {
    navigate(
        InheritanceRequestPlanningConfirmRoute(
            walletId = walletId,
            groupId = groupId,
        )
    )
}
