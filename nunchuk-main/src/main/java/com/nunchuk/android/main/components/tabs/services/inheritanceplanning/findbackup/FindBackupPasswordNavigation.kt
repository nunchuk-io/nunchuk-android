package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.findbackup

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritanceKeyType
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.MembershipStepEffect
import kotlinx.serialization.Serializable

@Serializable
data class FindBackupPasswordRoute(val stepNumber: Int = 1)

fun NavGraphBuilder.findBackupPassword(
    onContinueClicked: (stepNumber: Int) -> Unit,
) {
    composable<FindBackupPasswordRoute> { backStackEntry ->
        val activity = LocalActivity.current as InheritancePlanningActivity
        val activityViewModel: InheritancePlanningViewModel =
            hiltViewModel(viewModelStoreOwner = activity)
        MembershipStepEffect(activity.membershipStepManager)
        val route = backStackEntry.toRoute<FindBackupPasswordRoute>()
        val viewModel = hiltViewModel<FindBackupPasswordViewModel>()
        val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
        val uiState by activityViewModel.state.collectAsStateWithLifecycle()

        FindBackupPasswordContent(
            remainTime = remainTime,
            inheritanceKeyType = if (uiState.keyTypes.isNotEmpty()) {
                uiState.keyTypes[route.stepNumber - 1]
            } else {
                InheritanceKeyType.TAPSIGNER
            },
            numOfKeys = uiState.keyTypes.size,
            keyTypes = uiState.keyTypes,
            stepNumber = route.stepNumber,
        ) {
            onContinueClicked(route.stepNumber)
        }
    }
}

fun NavController.navigateToFindBackupPassword(stepNumber: Int = 1) {
    navigate(FindBackupPasswordRoute(stepNumber = stepNumber))
}
