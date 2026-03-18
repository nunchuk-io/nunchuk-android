package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceReleaseScheduleFlowViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseScheduleBufferPeriodSummaryText
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceReleaseScheduleDetailRoute(
    val draftId: String = "",
    val isPostBufferPeriodMethod: Boolean = false,
    val fromBeneficiarySchedules: Boolean = false,
    val beneficiaryEmail: String = "",
    val returnToReviewPlan: Boolean = false,
)

fun NavGraphBuilder.inheritanceReleaseScheduleDetail(
    onEditStage: (ReleaseScheduleStage) -> Unit,
    onEditBufferPeriodClicked: (InheritanceReleaseScheduleDetailRoute) -> Unit,
    onAddStageRequested: () -> Unit,
    onContinueClicked: (InheritanceReleaseScheduleDetailRoute) -> Unit,
) {
    composable<InheritanceReleaseScheduleDetailRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        val releaseScheduleFlowViewModel: InheritanceReleaseScheduleFlowViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val setupOrReviewParam = activityViewModel.setupOrReviewParam
        val route = backStackEntry.toRoute<InheritanceReleaseScheduleDetailRoute>()
        val releaseScheduleFlowState by releaseScheduleFlowViewModel.state.collectAsStateWithLifecycle()
        val draftId = route.draftId.ifBlank { releaseScheduleFlowState.activeDraftId }
        val draft = releaseScheduleFlowState.drafts[draftId]
            ?: releaseScheduleFlowViewModel.getDraft(draftId)
        LaunchedEffect(draftId) {
            releaseScheduleFlowViewModel.setActiveDraftId(draftId)
        }
        val releaseScheduleUiState = draft.releaseScheduleUiState
        val beneficiaryKey = route.beneficiaryEmail.trim().lowercase().ifBlank {
            ""
        }
        val isBeneficiaryScheduleContext =
            route.fromBeneficiarySchedules || beneficiaryKey.isNotBlank()
        val existingConfig = when {
            beneficiaryKey.isNotBlank() ->
                setupOrReviewParam.individualScheduleConfigs.entries
                    .firstOrNull { it.key.trim().lowercase() == beneficiaryKey }
                    ?.value

            route.fromBeneficiarySchedules || setupOrReviewParam.setupFlowType == com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceSetupFlowType.SINGLE_BENEFICIARY ->
                setupOrReviewParam.sharedScheduleConfig

            else -> null
        }
        val hasExistingScheduleConfig = if (isBeneficiaryScheduleContext) {
            if (beneficiaryKey.isNotBlank()) {
                setupOrReviewParam.individualScheduleConfigs.keys.any { key ->
                    key.trim().lowercase() == beneficiaryKey
                }
            } else {
                existingConfig != null
            }
        } else {
            existingConfig != null
        }
        val shouldShowBufferSummary =
            draft.hasBufferPeriodSelection ||
            route.isPostBufferPeriodMethod ||
                hasExistingScheduleConfig
        InheritanceReleaseScheduleDetailScreen(
            remainTime = remainTime,
            uiState = releaseScheduleUiState,
            descriptionText = if (route.fromBeneficiarySchedules) {
                stringResource(id = R.string.nc_release_schedule_shared_beneficiary_desc)
            } else {
                null
            },
            bufferPeriodSummaryText = if (shouldShowBufferSummary) {
                releaseScheduleBufferPeriodSummaryText(
                    period = draft.bufferPeriod,
                    applyType = draft.bufferPeriodApplyType,
                )
            } else {
                null
            },
            onUiStateChanged = { updatedState ->
                releaseScheduleFlowViewModel.setReleaseScheduleUiState(draftId, updatedState)
            },
            onEditStage = onEditStage,
            onEditBufferPeriodClicked = { onEditBufferPeriodClicked(route) },
            onAddStageRequested = onAddStageRequested,
            onContinueClicked = { onContinueClicked(route) },
        )
    }
}

fun NavController.navigateToInheritanceReleaseScheduleDetail(
    draftId: String = "",
    isPostBufferPeriodMethod: Boolean = false,
    fromBeneficiarySchedules: Boolean = false,
    beneficiaryEmail: String = "",
    returnToReviewPlan: Boolean = false,
) {
    navigate(
        InheritanceReleaseScheduleDetailRoute(
            draftId = draftId,
            isPostBufferPeriodMethod = isPostBufferPeriodMethod,
            fromBeneficiarySchedules = fromBeneficiarySchedules,
            beneficiaryEmail = beneficiaryEmail,
            returnToReviewPlan = returnToReviewPlan,
        )
    )
}
