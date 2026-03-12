package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceReleaseScheduleFlowViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceReleaseScheduleStageEditRoute(
    val stageId: Int,
    val isNewStage: Boolean = false,
)

fun NavGraphBuilder.inheritanceReleaseScheduleStageEdit(
    onBackClicked: (isNewStage: Boolean) -> Unit,
    onStageNotFound: () -> Unit,
    onDeleteStage: (stageId: Int, isNewStage: Boolean) -> Unit,
    onConfirmStage: (updatedStage: ReleaseScheduleStage, isNewStage: Boolean) -> Unit,
) {
    composable<InheritanceReleaseScheduleStageEditRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val releaseScheduleFlowViewModel: InheritanceReleaseScheduleFlowViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceReleaseScheduleStageEditRoute>()
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val releaseScheduleFlowState by releaseScheduleFlowViewModel.state.collectAsStateWithLifecycle()
        val releaseScheduleUiState = releaseScheduleFlowState.releaseScheduleUiState
        val pendingNewStage = releaseScheduleFlowState.pendingNewStage

        val stage = if (route.isNewStage) {
            pendingNewStage
        } else {
            releaseScheduleUiState.getStage(route.stageId)
        }

        if (stage == null) {
            LaunchedEffect(route.stageId, route.isNewStage) {
                onStageNotFound()
            }
        } else if (stage != null) {
            val previousStageDate = releaseScheduleUiState.previousStageFinalDate(stage.stageNumber)
            InheritanceReleaseScheduleStageEditScreen(
                remainTime = remainTime,
                stage = stage,
                previousStageDate = previousStageDate,
                isNewStage = route.isNewStage,
                onBackClicked = { onBackClicked(route.isNewStage) },
                onDeleteClicked = { stageId -> onDeleteStage(stageId, route.isNewStage) },
                onConfirmClicked = { updatedStage -> onConfirmStage(updatedStage, route.isNewStage) },
            )
        }
    }
}

fun NavController.navigateToInheritanceReleaseScheduleStageEdit(
    stageId: Int,
    isNewStage: Boolean = false,
) {
    navigate(
        InheritanceReleaseScheduleStageEditRoute(
            stageId = stageId,
            isNewStage = isNewStage,
        )
    )
}
