package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiodmethod

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceReleaseScheduleFlowViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.toBufferPeriodMethodOption
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceBufferPeriodMethodRoute(
    val draftId: String = "",
    val fromBeneficiarySchedules: Boolean = false,
    val beneficiaryEmail: String = "",
    val returnToReviewPlan: Boolean = false,
)

internal fun NavGraphBuilder.inheritanceBufferPeriodMethod(
    onContinueClicked: (BufferPeriodMethodOption, String, Boolean, String, Boolean) -> Unit,
) {
    composable<InheritanceBufferPeriodMethodRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val releaseScheduleFlowViewModel: InheritanceReleaseScheduleFlowViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceBufferPeriodMethodRoute>()
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val routeDraftId = route.draftId.takeIf { it.isNotBlank() }.orEmpty()
        val resolvedDraftId = routeDraftId.ifBlank { releaseScheduleFlowViewModel.state.value.activeDraftId }
        val draft = if (routeDraftId.isNotBlank()) {
            releaseScheduleFlowViewModel.getDraft(resolvedDraftId)
        } else {
            null
        }
        val selectedOption = if (routeDraftId.isNotBlank()) {
            draft?.bufferPeriodApplyType?.toBufferPeriodMethodOption()
                ?: BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY
        } else {
            draft?.bufferPeriodApplyType?.toBufferPeriodMethodOption()
                ?: activityViewModel.setupOrReviewParam.bufferPeriodApplyType
                    ?.toBufferPeriodMethodOption()
                ?: BufferPeriodMethodOption.FIRST_WITHDRAWAL_ONLY
        }
        InheritanceBufferPeriodMethodScreen(
            remainTime = remainTime,
            selectedOption = selectedOption,
            onBackClicked = { activity.onBackPressedDispatcher.onBackPressed() },
            onContinueClicked = { option ->
                onContinueClicked(
                    option,
                    routeDraftId,
                    route.fromBeneficiarySchedules,
                    route.beneficiaryEmail,
                    route.returnToReviewPlan,
                )
            },
        )
    }
}

fun NavController.navigateToInheritanceBufferPeriodMethod(
    draftId: String = "",
    fromBeneficiarySchedules: Boolean = false,
    beneficiaryEmail: String = "",
    returnToReviewPlan: Boolean = false,
) {
    navigate(
        InheritanceBufferPeriodMethodRoute(
            draftId = draftId,
            fromBeneficiarySchedules = fromBeneficiarySchedules,
            beneficiaryEmail = beneficiaryEmail,
            returnToReviewPlan = returnToReviewPlan,
        )
    )
}
