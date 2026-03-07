package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releaseschedulestageedit

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleStage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.releasescheduledetail.ReleaseScheduleUiState
import kotlinx.serialization.Serializable

@Serializable
data class InheritanceReleaseScheduleStageEditRoute(
    val stageId: Int,
    val isNewStage: Boolean = false,
)

fun NavGraphBuilder.inheritanceReleaseScheduleStageEdit(
    releaseScheduleUiStateProvider: () -> ReleaseScheduleUiState,
    pendingNewStageProvider: () -> ReleaseScheduleStage?,
    onBackClicked: (isNewStage: Boolean) -> Unit,
    onStageNotFound: () -> Unit,
    onDeleteStage: (stageId: Int, isNewStage: Boolean) -> Unit,
    onConfirmStage: (updatedStage: ReleaseScheduleStage, isNewStage: Boolean) -> Unit,
) {
    composable<InheritanceReleaseScheduleStageEditRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<InheritanceReleaseScheduleStageEditRoute>()
        val remainTime by activity.membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val releaseScheduleUiState = releaseScheduleUiStateProvider()
        val pendingNewStage = pendingNewStageProvider()

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
            val previousStageDate = releaseScheduleUiState.previousStageDate(stage.stageNumber)
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
