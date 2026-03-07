package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.beneficiaryschedules

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.defaultBeneficiaryAllocations
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseScheduleBufferPeriodSummaryText
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.toReleaseMethodOption
import kotlinx.serialization.Serializable

@Serializable
data object InheritanceBeneficiarySchedulesRoute

fun NavGraphBuilder.inheritanceBeneficiarySchedules(
    releaseScheduleUiStateProvider: () -> ReleaseScheduleUiState,
    onBackClicked: () -> Unit,
    onEditReleaseMethodClicked: () -> Unit,
    onAddReleaseScheduleClicked: () -> Unit,
    onEditSharedScheduleClicked: () -> Unit,
    onEditBeneficiaryScheduleClicked: (String) -> Unit,
) {
    composable<InheritanceBeneficiarySchedulesRoute> {
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val planningState by activityViewModel.state.collectAsStateWithLifecycle()
        val setupOrReviewParam = planningState.setupOrReviewParam
        val releaseScheduleUiState = releaseScheduleUiStateProvider()
        val individualScheduleCardDataByEmail =
            setupOrReviewParam.individualScheduleConfigs.mapValues { (_, config) ->
                InheritanceBeneficiaryScheduleCardData(
                    releaseScheduleUiState = config.releaseScheduleUiState,
                    bufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                        period = config.bufferPeriod,
                        applyType = config.bufferPeriodApplyType,
                    ),
                )
            }
        InheritanceBeneficiarySchedulesScreen(
            remainTime = remainTime,
            releaseMethod = setupOrReviewParam.releaseMethodType.toReleaseMethodOption(),
            beneficiaries = setupOrReviewParam.beneficiaryAllocations.ifEmpty {
                defaultBeneficiaryAllocations()
            },
            releaseScheduleUiState = releaseScheduleUiState,
            individualScheduleCardDataByEmail = individualScheduleCardDataByEmail,
            sharedBufferPeriodSummaryText = releaseScheduleBufferPeriodSummaryText(
                period = setupOrReviewParam.bufferPeriod,
                applyType = setupOrReviewParam.bufferPeriodApplyType,
            ),
            isSharedScheduleConfigured = setupOrReviewParam.isSharedScheduleConfigured,
            onBackClicked = onBackClicked,
            onEditReleaseMethodClicked = onEditReleaseMethodClicked,
            onAddReleaseScheduleClicked = onAddReleaseScheduleClicked,
            onEditSharedScheduleClicked = onEditSharedScheduleClicked,
            onEditBeneficiaryScheduleClicked = onEditBeneficiaryScheduleClicked,
        )
    }
}

fun NavController.navigateToInheritanceBeneficiarySchedules() { navigate(InheritanceBeneficiarySchedulesRoute) }
