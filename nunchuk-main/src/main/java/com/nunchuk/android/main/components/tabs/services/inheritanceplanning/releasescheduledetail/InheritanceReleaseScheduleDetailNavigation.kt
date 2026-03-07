package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseScheduleBufferPeriodSummaryText
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceReleaseScheduleDetailRoute(
    val isPostBufferPeriodMethod: Boolean = false,
    val fromBeneficiarySchedules: Boolean = false,
    val beneficiaryEmail: String = "",
)

fun NavGraphBuilder.inheritanceReleaseScheduleDetail(
    releaseScheduleUiState: ReleaseScheduleUiState,
    onUiStateChanged: (ReleaseScheduleUiState) -> Unit,
    onEditStage: (ReleaseScheduleStage) -> Unit,
    onEditBufferPeriodClicked: (InheritanceReleaseScheduleDetailRoute) -> Unit,
    onAddStageRequested: () -> Unit,
    onContinueClicked: (InheritanceReleaseScheduleDetailRoute) -> Unit,
) {
    composable<InheritanceReleaseScheduleDetailRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val setupOrReviewParam = activityViewModel.setupOrReviewParam
        val route = backStackEntry.toRoute<InheritanceReleaseScheduleDetailRoute>()
        InheritanceReleaseScheduleDetailScreen(
            remainTime = remainTime,
            uiState = releaseScheduleUiState,
            descriptionText = if (route.fromBeneficiarySchedules) {
                stringResource(id = R.string.nc_release_schedule_shared_beneficiary_desc)
            } else {
                null
            },
            bufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                period = setupOrReviewParam.bufferPeriod,
                applyType = setupOrReviewParam.bufferPeriodApplyType,
            ),
            onUiStateChanged = onUiStateChanged,
            onEditStage = onEditStage,
            onEditBufferPeriodClicked = { onEditBufferPeriodClicked(route) },
            onAddStageRequested = onAddStageRequested,
            onContinueClicked = { onContinueClicked(route) },
        )
    }
}

fun NavController.navigateToInheritanceReleaseScheduleDetail(
    isPostBufferPeriodMethod: Boolean = false,
    fromBeneficiarySchedules: Boolean = false,
    beneficiaryEmail: String = "",
) {
    navigate(
        InheritanceReleaseScheduleDetailRoute(
            isPostBufferPeriodMethod = isPostBufferPeriodMethod,
            fromBeneficiarySchedules = fromBeneficiarySchedules,
            beneficiaryEmail = beneficiaryEmail,
        )
    )
}
