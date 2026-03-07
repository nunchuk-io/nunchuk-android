package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod

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
import com.nunchuk.android.model.Period
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceBufferPeriodRoute(
    val isUpdateRequest: Boolean = false,
    val fromBeneficiarySchedules: Boolean = false,
    val beneficiaryEmail: String = "",
)

fun NavGraphBuilder.inheritanceBufferPeriod(
    onContinueClick: (
        isUpdateRequest: Boolean,
        fromBeneficiarySchedules: Boolean,
        beneficiaryEmail: String,
        period: Period?,
    ) -> Unit,
) {
    composable<InheritanceBufferPeriodRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceBufferPeriodRoute>()
        val viewModel = hiltViewModel<InheritanceBufferPeriodViewModel>()
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
                        is InheritanceBufferPeriodEvent.Loading -> activity.showOrHideLoading(event.isLoading)
                        is InheritanceBufferPeriodEvent.Error -> NCToastMessage(activity).showError(event.message)
                        is InheritanceBufferPeriodEvent.OnContinueClick -> {
                            onContinueClick(
                                route.isUpdateRequest,
                                route.fromBeneficiarySchedules,
                                route.beneficiaryEmail,
                                event.period
                            )
                        }
                    }
                }
        }

        InheritanceBufferPeriodScreen(
            viewModel = viewModel,
            isUpdateRequest = route.isUpdateRequest,
            inheritanceViewModel = activityViewModel,
        )
    }
}

fun NavController.navigateToInheritanceBufferPeriod(
    isUpdateRequest: Boolean = false,
    fromBeneficiarySchedules: Boolean = false,
    beneficiaryEmail: String = "",
) {
    navigate(
        InheritanceBufferPeriodRoute(
            isUpdateRequest = isUpdateRequest,
            fromBeneficiarySchedules = fromBeneficiarySchedules,
            beneficiaryEmail = beneficiaryEmail,
        )
    )
}
